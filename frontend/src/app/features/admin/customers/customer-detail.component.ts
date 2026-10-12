import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../../../core/services/admin.service';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { remote } from '../../../core/utils/remote';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { PaginationComponent } from '../shared/pagination.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-admin-customer-detail',
  imports: [DatePipe, RouterLink, LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, StatusBadgeComponent,
    PaginationComponent, TableSkeletonComponent],
  templateUrl: './customer-detail.component.html',
})
export class CustomerDetailComponent {
  private readonly admin = inject(AdminService);
  readonly id = input.required<string>();
  readonly page = signal(0);
  readonly customer = remote(() => this.admin.customer(Number(this.id())), { lazy: true });
  readonly orders = remote(() => this.admin.customerOrders(Number(this.id()), this.page(), PAGE_SIZE), { lazy: true });

  constructor() {
    effect(() => {
      this.id();
      untracked(() => {
        this.page.set(0);
        this.customer.reload();
      });
    });
    effect(() => {
      this.id();
      this.page();
      untracked(() => this.orders.reload());
    });
  }
}
