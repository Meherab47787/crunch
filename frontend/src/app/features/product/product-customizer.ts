import { Injectable, computed, signal } from '@angular/core';
import { CustomizationGroup, ProductDetail } from '../../core/models/api.models';
import { CartLine } from '../../core/services/cart.service';

/**
 * State + rules for configuring any product (sizes, option groups, combo slots).
 * Driven entirely by the ProductDetail returned from the API, so no product type
 * needs its own form. Prices here are a preview; the server re-prices everything.
 */
@Injectable()
export class ProductCustomizer {
  readonly product = signal<ProductDetail | null>(null);
  readonly variantId = signal<number | null>(null);
  /** groupId -> selected option ids */
  readonly selected = signal<Record<number, number[]>>({});
  /** componentId -> chosen product id per slot (length = component.quantity) */
  readonly comboPicks = signal<Record<number, (number | null)[]>>({});
  readonly quantity = signal(1);
  readonly notes = signal('');

  init(detail: ProductDetail, existing?: CartLine): void {
    this.product.set(detail);
    const availableVariants = detail.variants.filter((v) => v.available);
    const defaultVariant = availableVariants.find((v) => v.defaultVariant) ?? availableVariants[0] ?? null;
    this.variantId.set(
      existing?.variantId && detail.variants.some((v) => v.id === existing.variantId) ? existing.variantId : defaultVariant?.id ?? null,
    );

    const sel: Record<number, number[]> = {};
    for (const g of detail.groups) {
      const ids = new Set(g.options.filter((o) => o.available).map((o) => o.id));
      sel[g.id] = existing
        ? existing.optionIds.filter((id) => ids.has(id))
        : g.options.filter((o) => o.defaultOption && o.available).map((o) => o.id).slice(0, g.maxSelect);
      // single required group with no default: pick nothing, let the user choose
    }
    this.selected.set(sel);

    const picks: Record<number, (number | null)[]> = {};
    for (const comp of detail.comboComponents) {
      const fromLine = existing?.comboSelections.filter((s) => s.componentId === comp.id).map((s) => s.productId) ?? [];
      const def = comp.choices.find((c) => c.defaultChoice && c.available) ?? comp.choices.find((c) => c.available);
      picks[comp.id] = Array.from({ length: comp.quantity }, (_, i) => fromLine[i] ?? def?.productId ?? null);
    }
    this.comboPicks.set(picks);
    this.quantity.set(existing?.quantity ?? 1);
    this.notes.set(existing?.notes ?? '');
  }

  selectedFor(groupId: number): number[] {
    return this.selected()[groupId] ?? [];
  }

  toggleOption(group: CustomizationGroup, optionId: number): void {
    const cur = this.selectedFor(group.id);
    let next: number[];
    if (group.selectionType === 'SINGLE') {
      // radio behaviour; optional single groups can be cleared by tapping again
      next = cur.includes(optionId) ? (group.minSelect > 0 ? cur : []) : [optionId];
    } else if (cur.includes(optionId)) {
      next = cur.filter((id) => id !== optionId);
    } else {
      if (cur.length >= group.maxSelect) return;
      next = [...cur, optionId];
    }
    this.selected.update((s) => ({ ...s, [group.id]: next }));
  }

  setComboPick(componentId: number, slot: number, productId: number): void {
    this.comboPicks.update((p) => {
      const arr = [...(p[componentId] ?? [])];
      arr[slot] = productId;
      return { ...p, [componentId]: arr };
    });
  }

  readonly unitPrice = computed(() => {
    const d = this.product();
    if (!d) return 0;
    const variant = d.variants.find((v) => v.id === this.variantId());
    let price = d.summary.basePrice + (variant?.priceDelta ?? 0);
    const sel = this.selected();
    for (const g of d.groups) {
      for (const o of g.options) if ((sel[g.id] ?? []).includes(o.id)) price += o.priceDelta;
    }
    const picks = this.comboPicks();
    for (const comp of d.comboComponents) {
      for (const pid of picks[comp.id] ?? []) {
        price += comp.choices.find((c) => c.productId === pid)?.extraPrice ?? 0;
      }
    }
    return round(price);
  });

  readonly discountPerUnit = computed(() => {
    const promo = this.product()?.promotion;
    const unit = this.unitPrice();
    if (!promo) return 0;
    const d = promo.discountType === 'PERCENT' ? round((unit * promo.discountValue) / 100) : promo.discountValue;
    return Math.min(d, unit);
  });

  readonly lineTotal = computed(() => round((this.unitPrice() - this.discountPerUnit()) * this.quantity()));

  /** Human-readable reasons the current configuration can't be added yet. */
  readonly problems = computed(() => {
    const d = this.product();
    if (!d) return [];
    const out: string[] = [];
    if (d.variants.length && this.variantId() == null) out.push('Choose a size');
    const sel = this.selected();
    for (const g of d.groups) {
      const n = (sel[g.id] ?? []).length;
      if (n < g.minSelect) out.push(g.minSelect === 1 ? `Choose a ${g.name.toLowerCase()}` : `Choose at least ${g.minSelect} for ${g.name}`);
    }
    const picks = this.comboPicks();
    for (const comp of d.comboComponents) {
      if ((picks[comp.id] ?? []).some((p) => p == null)) out.push(comp.name);
    }
    return out;
  });

  toCartLine(): Omit<CartLine, 'key'> {
    const d = this.product()!;
    const variant = d.variants.find((v) => v.id === this.variantId());
    const sel = this.selected();
    const optionIds: number[] = [];
    const summary: string[] = [];
    if (variant) summary.push(variant.name);
    for (const g of d.groups) {
      const chosen = g.options.filter((o) => (sel[g.id] ?? []).includes(o.id));
      chosen.forEach((o) => optionIds.push(o.id));
      if (chosen.length) {
        summary.push(g.selectionType === 'SINGLE' ? chosen[0].name : chosen.map((o) => '+ ' + o.name).join(', '));
      }
    }
    const comboSelections = d.comboComponents.flatMap((comp) =>
      (this.comboPicks()[comp.id] ?? [])
        .filter((pid): pid is number => pid != null)
        .map((productId) => ({ componentId: comp.id, productId })),
    );
    for (const comp of d.comboComponents) {
      const names = (this.comboPicks()[comp.id] ?? []).map((pid) => comp.choices.find((c) => c.productId === pid)?.name).filter(Boolean);
      summary.push(...collapse(names as string[]));
    }
    const notes = this.notes().trim() || null;
    if (notes) summary.push(`“${notes}”`);
    return {
      productId: d.summary.id,
      variantId: variant?.id ?? null,
      optionIds,
      comboSelections,
      quantity: this.quantity(),
      notes,
      slug: d.summary.slug,
      name: d.summary.name,
      imageUrl: d.summary.imageUrl,
      categoryName: d.summary.categoryName,
      summary,
      displayUnitPrice: round(this.unitPrice() - this.discountPerUnit()),
    };
  }
}

function round(n: number): number {
  return Math.round(n * 100) / 100;
}

/** ["Soda","Soda","Lemonade"] -> ["2× Soda","Lemonade"] */
function collapse(names: string[]): string[] {
  const counts = new Map<string, number>();
  names.forEach((n) => counts.set(n, (counts.get(n) ?? 0) + 1));
  return [...counts].map(([n, c]) => (c > 1 ? `${c}× ${n}` : n));
}
