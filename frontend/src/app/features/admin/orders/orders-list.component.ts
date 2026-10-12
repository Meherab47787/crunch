import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { OrderStatus } from '../../../core/models/api.models';
import { AdminOrderQuery, AdminService } from '../../../core/services/admin.service';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { ALL_STATUSES, STATUS_SHORT } from '../../../core/utils/order-status';
import { remote } from '../../../core/utils/remote';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { PaginationComponent } from '../shared/pagination.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-orders',
  imports: [DatePipe, RouterLink, LucideAngularModule, MoneyPipe, StatusBadgeComponent, EmptyStateComponent, ErrorStateComponent,
    AdminPageHeaderComponent, PaginationComponent, TableSkeletonComponent],
  templateUrl: './orders-list.component.html',
})
export class OrdersListComponent {
  private readonly admin = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly statuses = ALL_STATUSES;
  readonly statusLabel = STATUS_SHORT;
  readonly categories = remote(() => this.admin.categories());

  readonly params = toSignal(this.route.queryParams, { initialValue: {} as Params });
  readonly searchText = signal('');
  readonly filtersOpen = signal(false);
  readonly filterCls = computed(() => (this.filtersOpen() ? '' : 'hidden sm:block'));
  readonly activeFilterCount = computed(() => {
    const p = this.params();
    return ['status', 'fulfillment', 'payment', 'category', 'from', 'to'].filter((k) => !!p[k]).length;
  });
  private debounce?: ReturnType<typeof setTimeout>;

  readonly query = computed<AdminOrderQuery>(() => {
    const p = this.params();
    return {
      q: p['q'] || undefined,
      status: (p['status'] as OrderStatus) || undefined,
      fulfillmentType: p['fulfillment'] || undefined,
      paymentMethod: p['payment'] || undefined,
      category: p['category'] || undefined,
      from: p['from'] ? new Date(p['from'] + 'T00:00:00').toISOString() : undefined,
      to: p['to'] ? new Date(p['to'] + 'T23:59:59.999').toISOString() : undefined,
      page: Number(p['page']) || 0,
      size: PAGE_SIZE,
    };
  });

  readonly hasFilters = computed(() => {
    const p = this.params();
    return ['q', 'status', 'fulfillment', 'payment', 'category', 'from', 'to'].some((k) => !!p[k]);
  });

  readonly orders = remote(() => this.admin.orders(this.query()), { lazy: true });

  constructor() {
    effect(() => {
      const p = this.params();
      untracked(() => {
        if (this.searchText() !== (p['q'] ?? '')) this.searchText.set(p['q'] ?? '');
      });
    });
    effect(() => {
      this.query();
      untracked(() => this.orders.reload());
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.debounce));
  }

  onSearchInput(v: string) {
    this.searchText.set(v);
    clearTimeout(this.debounce);
    this.debounce = setTimeout(() => this.applySearch(), 350);
  }

  applySearch() {
    clearTimeout(this.debounce);
    const v = this.searchText().trim();
    if ((this.params()['q'] ?? '') !== v) this.setParam('q', v);
  }

  setParam(key: string, value: string) {
    const queryParams: Params = { [key]: value || null };
    if (key !== 'page') queryParams['page'] = null;
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: key === 'q' });
  }

  clearFilters() {
    this.searchText.set('');
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }
}
