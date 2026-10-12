import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Order, OrderItem } from '../../core/models/api.models';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';
import { errorMessage } from '../../core/utils/errors';
import { isActive } from '../../core/utils/order-status';
import { ConfirmService } from '../../core/services/confirm.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ModalComponent } from '../../shared/components/modal.component';
import { OrderViewComponent } from '../orders/order-view.component';
import { ReceiptComponent } from '../orders/receipt.component';

@Component({
  selector: 'app-my-order-detail',
  imports: [FormsModule, RouterLink, LucideAngularModule, OrderViewComponent, ReceiptComponent, EmptyStateComponent, ModalComponent],
  templateUrl: './my-order-detail.component.html',
})
export class MyOrderDetailComponent implements OnInit {
  private readonly orders = inject(OrderService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();
  readonly order = signal<Order | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly active = () => !!this.order() && isActive(this.order()!.status);
  readonly reviewing = signal<OrderItem | null>(null);
  readonly rating = signal(5);
  comment = '';

  ngOnInit() {
    this.load();
    const t = setInterval(() => this.active() && document.visibilityState === 'visible' && this.load(), 15_000);
    this.destroyRef.onDestroy(() => clearInterval(t));
  }

  load() {
    this.orders.get(+this.id()).subscribe({
      next: (o) => this.order.set(o),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  /** One review button per distinct product still on the menu. */
  reviewable(o: Order): OrderItem[] {
    const seen = new Set<number>();
    return o.items.filter((i) => i.productId != null && !seen.has(i.productId) && seen.add(i.productId));
  }

  async cancel() {
    const ok = await this.confirm.ask({
      title: 'Cancel this order?',
      message: 'We’ll stop the kitchen from starting it. Online payments are refunded automatically.',
      confirmLabel: 'Cancel order',
      danger: true,
    });
    if (!ok) return;
    this.busy.set(true);
    this.orders.cancel(this.order()!.id, null).subscribe({
      next: (o) => {
        this.order.set(o);
        this.busy.set(false);
        this.toast.success('Your order was cancelled');
      },
      error: (e) => {
        this.busy.set(false);
        this.toast.error(errorMessage(e));
        this.load();
      },
    });
  }

  /** The server rebuilds cart lines from today's menu (unavailable items are reported back). */
  reorder() {
    this.busy.set(true);
    this.orders.reorder(this.order()!.id).subscribe({
      next: (res) => {
        if (!res.items.length) {
          this.busy.set(false);
          this.toast.error(res.skipped[0] ?? 'None of these items are available right now.');
          return;
        }
        for (const l of res.items) {
          this.cart.add({
            ...l.request,
            slug: l.slug,
            name: l.name,
            imageUrl: l.imageUrl,
            categoryName: l.categoryName,
            summary: l.summary,
            displayUnitPrice: l.unitPrice,
          });
        }
        this.busy.set(false);
        if (res.skipped.length) this.toast.info(`Some items were skipped: ${res.skipped.join('; ')}`);
        else this.toast.success('Added to your bag — priced from today’s menu');
        this.router.navigateByUrl('/cart');
      },
      error: (e) => {
        this.busy.set(false);
        this.toast.error(errorMessage(e));
      },
    });
  }

  startReview(item: OrderItem) {
    this.rating.set(5);
    this.comment = '';
    this.reviewing.set(item);
  }

  submitReview(item: OrderItem) {
    this.busy.set(true);
    this.orders.review({ orderId: this.order()!.id, productId: item.productId!, rating: this.rating(), comment: this.comment.trim() || null })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.reviewing.set(null);
          this.toast.success('Thanks for the review!');
          this.load();
        },
        error: (e) => {
          this.busy.set(false);
          this.toast.error(errorMessage(e));
        },
      });
  }

  print() {
    window.print();
  }
}
