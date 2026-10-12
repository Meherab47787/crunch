import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../../../core/services/admin.service';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { remote } from '../../../core/utils/remote';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { PaginationComponent } from '../shared/pagination.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-customers',
  imports: [DatePipe, RouterLink, LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent,
    PaginationComponent, TableSkeletonComponent],
  templateUrl: './customers-list.component.html',
})
export class CustomersListComponent {
  private readonly admin = inject(AdminService);
  readonly search = signal('');
  readonly q = signal('');
  readonly page = signal(0);
  private debounce?: ReturnType<typeof setTimeout>;
  private readonly query = computed(() => ({ q: this.q(), page: this.page() }));
  readonly customers = remote(() => this.admin.customers(this.query().q, this.query().page, PAGE_SIZE), { lazy: true });

  constructor() {
    effect(() => {
      this.query();
      untracked(() => this.customers.reload());
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
}
