import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { OrderSummary, Page } from '../../core/models/api.models';
import { OrderService } from '../../core/services/order.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { remote } from '../../core/utils/remote';
import { isActive } from '../../core/utils/order-status';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../shared/components/error-state.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';

@Component({
  selector: 'app-my-orders',
  imports: [RouterLink, DatePipe, LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, StatusBadgeComponent],
  templateUrl: './my-orders.component.html',
})
export class MyOrdersComponent {
  private readonly api = inject(OrderService);
  readonly page = signal(0);
  readonly orders = remote<Page<OrderSummary>>(() => this.api.myOrders(this.page(), 10));

  active(o: OrderSummary) {
    return isActive(o.status);
  }

  go(p: number) {
    this.page.set(p);
    this.orders.reload();
  }
}
