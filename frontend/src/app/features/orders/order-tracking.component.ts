import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Order } from '../../core/models/api.models';
import { OrderService } from '../../core/services/order.service';
import { RecentOrdersService } from '../../core/services/recent-orders.service';
import { errorMessage } from '../../core/utils/errors';
import { isActive } from '../../core/utils/order-status';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { OrderViewComponent } from './order-view.component';
import { ReceiptComponent } from './receipt.component';

const POLL_MS = 15_000;

/** Guest-friendly tracking page: /orders/:orderNumber?token=... (polls while the order is active). */
@Component({
  selector: 'app-order-tracking',
  imports: [RouterLink, LucideAngularModule, EmptyStateComponent, OrderViewComponent, ReceiptComponent],
  templateUrl: './order-tracking.component.html',
})
export class OrderTrackingComponent implements OnInit {
  private readonly orders = inject(OrderService);
  private readonly recent = inject(RecentOrdersService);
  private readonly destroyRef = inject(DestroyRef);

  readonly orderNumber = input.required<string>();
  readonly token = input<string>();
  readonly placed = input<string>();

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly active = computed(() => !!this.order() && isActive(this.order()!.status));
  readonly firstName = computed(() => this.order()?.contact.name.split(' ')[0] ?? '');
  private timer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.fetch();
    this.timer = setInterval(() => {
      if (this.active() && document.visibilityState === 'visible') this.fetch();
    }, POLL_MS);
    this.destroyRef.onDestroy(() => clearInterval(this.timer));
  }

  private resolvedToken(): string | null {
    return this.token() ?? this.recent.orders().find((o) => o.orderNumber === this.orderNumber())?.token ?? null;
  }

  private fetch(): void {
    const token = this.resolvedToken();
    if (!token) {
      this.loading.set(false);
      this.error.set('This link is missing its tracking code. Use the link from your confirmation, or sign in to see your orders.');
      return;
    }
    this.orders.track(this.orderNumber(), token).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'Check the order number and tracking code.'));
      },
    });
  }

  print(): void {
    window.print();
  }
}
