import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, map, of, startWith, switchMap } from 'rxjs';
import { CartLineRequest, ComboSelection, FulfillmentType, Quote } from '../models/api.models';
import { errorMessage } from '../utils/errors';
import { safeStorage } from '../utils/storage';

/**
 * A cart line stores only ids + display hints. Prices shown in the cart come from the
 * server quote (POST /api/cart/quote); `displayUnitPrice` is just an instant estimate
 * used before the quote returns.
 */
export interface CartLine extends CartLineRequest {
  key: string;
  slug: string;
  name: string;
  imageUrl: string | null;
  categoryName: string;
  summary: string[];
  displayUnitPrice: number;
}

interface PersistedCart {
  lines: CartLine[];
  couponCode: string | null;
  fulfillmentType: FulfillmentType;
}

const STORAGE_KEY = 'cc.cart.v1';
const MAX_QTY = 50;

/** Two lines are "the same item" only if every customization matches. */
export function lineKey(l: Pick<CartLineRequest, 'productId' | 'variantId' | 'optionIds' | 'comboSelections' | 'notes'>): string {
  const opts = [...l.optionIds].sort((a, b) => a - b).join(',');
  const combo = [...l.comboSelections]
    .map((c: ComboSelection) => `${c.componentId}:${c.productId}`)
    .sort()
    .join(',');
  return `${l.productId}|${l.variantId ?? ''}|${opts}|${combo}|${(l.notes ?? '').trim().toLowerCase()}`;
}

type QuoteState = { quote: Quote | null; loading: boolean; error: string | null };

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly initial = safeStorage.get<PersistedCart>(STORAGE_KEY);

  readonly lines = signal<CartLine[]>(this.initial?.lines ?? []);
  readonly couponCode = signal<string | null>(this.initial?.couponCode ?? null);
  readonly fulfillmentType = signal<FulfillmentType>(this.initial?.fulfillmentType ?? 'DELIVERY');
  readonly drawerOpen = signal(false);

  readonly itemCount = computed(() => this.lines().reduce((n, l) => n + l.quantity, 0));
  readonly isEmpty = computed(() => this.lines().length === 0);
  readonly estimatedSubtotal = computed(() =>
    this.lines().reduce((sum, l) => sum + l.displayUnitPrice * l.quantity, 0),
  );

  private readonly quoteState = signal<QuoteState>({ quote: null, loading: false, error: null });
  readonly quote = computed(() => this.quoteState().quote);
  readonly quoteLoading = computed(() => this.quoteState().loading);
  readonly quoteError = computed(() => this.quoteState().error);

  /** Last change that should be undoable from a toast. */
  private lastRemoved: { line: CartLine; index: number } | null = null;

  private readonly refreshTick = signal(0);

  private readonly quoteRequest = computed(() => ({
    tick: this.refreshTick(),
    items: this.lines().map(toRequest),
    couponCode: this.couponCode(),
    fulfillmentType: this.fulfillmentType(),
  }));

  constructor() {
    // Server re-pricing: debounced, latest request wins.
    toObservable(this.quoteRequest)
      .pipe(
        debounceTime(250),
        switchMap((req) => {
          if (req.items.length === 0) {
            return of<QuoteState>({ quote: null, loading: false, error: null });
          }
          const { tick: _tick, ...body } = req;
          return this.http.post<Quote>('/api/cart/quote', body).pipe(
            map((quote): QuoteState => ({ quote, loading: false, error: null })),
            catchError((err) => of<QuoteState>({ quote: null, loading: false, error: errorMessage(err) })),
            startWith<QuoteState>({ quote: this.quoteState().quote, loading: true, error: null }),
          );
        }),
      )
      .subscribe((s) => this.quoteState.set(s));

    // Keep carts in sync across browser tabs.
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (e) => {
        if (e.key !== STORAGE_KEY) return;
        const next = safeStorage.get<PersistedCart>(STORAGE_KEY);
        this.lines.set(next?.lines ?? []);
        this.couponCode.set(next?.couponCode ?? null);
        this.fulfillmentType.set(next?.fulfillmentType ?? 'DELIVERY');
      });
    }
  }

  add(input: Omit<CartLine, 'key'>): void {
    const line: CartLine = { ...input, key: lineKey(input) };
    this.lines.update((lines) => {
      const existing = lines.find((l) => l.key === line.key);
      if (existing) {
        return lines.map((l) =>
          l.key === line.key ? { ...l, quantity: Math.min(MAX_QTY, l.quantity + line.quantity) } : l,
        );
      }
      return [...lines, line];
    });
    this.persist();
  }

  /** Replaces a line after it was edited on the product page (merging if it now matches another line). */
  replace(oldKey: string, input: Omit<CartLine, 'key'>): void {
    const key = lineKey(input);
    this.lines.update((lines) => {
      const idx = lines.findIndex((l) => l.key === oldKey);
      const without = lines.filter((l) => l.key !== oldKey);
      const dup = without.find((l) => l.key === key);
      if (dup) {
        return without.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QTY, l.quantity + input.quantity) } : l));
      }
      const next = [...without];
      next.splice(idx < 0 ? next.length : idx, 0, { ...input, key });
      return next;
    });
    this.persist();
  }

  setQuantity(key: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(key);
      return;
    }
    this.lines.update((lines) => lines.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QTY, quantity) } : l)));
    this.persist();
  }

  remove(key: string): void {
    const lines = this.lines();
    const index = lines.findIndex((l) => l.key === key);
    if (index < 0) return;
    this.lastRemoved = { line: lines[index], index };
    this.lines.set(lines.filter((l) => l.key !== key));
    this.persist();
  }

  undoRemove(): void {
    const r = this.lastRemoved;
    if (!r) return;
    this.lines.update((lines) => {
      const next = [...lines];
      next.splice(Math.min(r.index, next.length), 0, r.line);
      return next;
    });
    this.lastRemoved = null;
    this.persist();
  }

  /** Written synchronously so a full page navigation right after a change can't lose it. */
  private persist(): void {
    const state: PersistedCart = {
      lines: this.lines(),
      couponCode: this.couponCode(),
      fulfillmentType: this.fulfillmentType(),
    };
    safeStorage.set(STORAGE_KEY, state);
  }

  find(key: string): CartLine | undefined {
    return this.lines().find((l) => l.key === key);
  }

  applyCoupon(code: string): void {
    this.couponCode.set(code.trim().toUpperCase() || null);
    this.persist();
  }

  removeCoupon(): void {
    this.couponCode.set(null);
    this.persist();
  }

  setFulfillment(type: FulfillmentType): void {
    this.fulfillmentType.set(type);
    this.persist();
  }

  clear(): void {
    this.lines.set([]);
    this.couponCode.set(null);
    this.persist();
  }

  /** Forces a fresh server quote (e.g. after checkout reports the menu changed). */
  refreshQuote(): void {
    this.refreshTick.update((n) => n + 1);
  }

  requests(): CartLineRequest[] {
    return this.lines().map(toRequest);
  }
}

function toRequest(l: CartLine): CartLineRequest {
  return {
    productId: l.productId,
    variantId: l.variantId,
    optionIds: l.optionIds,
    comboSelections: l.comboSelections,
    quantity: l.quantity,
    notes: l.notes,
  };
}
