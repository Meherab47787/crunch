import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ProductSummary, ProductType } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { PaginationComponent } from '../shared/pagination.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import { ToggleSwitchComponent } from '../shared/toggle-switch.component';

const PAGE_SIZE = 25;

@Component({
  selector: 'app-admin-products',
  imports: [RouterLink, LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent,
    PaginationComponent, TableSkeletonComponent, ToggleSwitchComponent],
  templateUrl: './products-list.component.html',
})
export class ProductsListComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  readonly categories = remote(() => this.admin.categories());
  readonly search = signal('');
  readonly q = signal('');
  /** Pre-filtered when arriving from a link such as /admin/products?category=pizza. */
  readonly category = signal(inject(ActivatedRoute).snapshot.queryParamMap.get('category') ?? '');
  readonly type = signal<ProductType | ''>('');
  readonly page = signal(0);
  readonly pending = signal(new Set<number>());
  private debounce?: ReturnType<typeof setTimeout>;

  private readonly query = computed(() => ({
    q: this.q() || undefined,
    category: this.category() || undefined,
    type: this.type() || undefined,
    page: this.page(),
    size: PAGE_SIZE,
  }));
  readonly products = remote(() => this.admin.products(this.query()), { lazy: true });

  constructor() {
    effect(() => {
      this.query();
      untracked(() => this.products.reload());
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.debounce));
  }

  onSearch(v: string) {
    this.search.set(v);
    clearTimeout(this.debounce);
    this.debounce = setTimeout(() => {
      this.q.set(v.trim());
      this.page.set(0);
    }, 300);
  }

  toggle(p: ProductSummary, change: { available?: boolean; visible?: boolean }) {
    const page = this.products.data();
    if (!page) return;
    const patch = (fn: (x: ProductSummary) => ProductSummary) =>
      this.products.set({ ...this.products.data()!, content: this.products.data()!.content.map((x) => (x.id === p.id ? fn(x) : x)) });
    patch((x) => ({ ...x, ...change })); // optimistic
    this.setPending(p.id, true);
    this.admin.setAvailability(p.id, change).subscribe({
      next: (updated) => {
        patch(() => updated);
        this.setPending(p.id, false);
        const what = change.available !== undefined ? (updated.available ? 'is back in stock' : 'marked sold out') : updated.visible ? 'is visible on the menu' : 'is hidden from the menu';
        this.toast.success(`${updated.name} ${what}`);
      },
      error: (err) => {
        patch(() => p); // roll back
        this.setPending(p.id, false);
        this.toast.error(errorMessage(err, 'Could not update the product.'));
      },
    });
  }

  async remove(p: ProductSummary) {
    const ok = await this.confirm.ask({
      title: `Delete ${p.name}?`,
      message: 'It will be removed from the menu. Past orders keep their line items. This cannot be undone.',
      confirmLabel: 'Delete product',
      danger: true,
    });
    if (!ok) return;
    this.admin.deleteProduct(p.id).subscribe({
      next: () => {
        this.toast.success(`${p.name} deleted`);
        this.products.reload();
      },
      error: (err) => this.toast.error(errorMessage(err, 'Could not delete the product.')),
    });
  }

  private setPending(id: number, on: boolean) {
    const next = new Set(this.pending());
    if (on) next.add(id);
    else next.delete(id);
    this.pending.set(next);
  }
}
