import { DatePipe } from '@angular/common';
import { Component, Injector, afterNextRender, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Order, OrderStatus } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { STATUS_SHORT } from '../../../core/utils/order-status';
import { remote } from '../../../core/utils/remote';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { ModalComponent } from '../../../shared/components/modal.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { KitchenTicketPrintComponent } from '../../kitchen/kitchen-ticket-print.component';
import { OrderItemsComponent } from './order-items.component';
import { ReceiptPrintComponent } from './receipt-print.component';

const CANCELLABLE: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PREPARING'];

@Component({
  selector: 'app-admin-order-detail',
  imports: [DatePipe, FormsModule, RouterLink, LucideAngularModule, MoneyPipe, StatusBadgeComponent, ErrorStateComponent, ModalComponent,
    OrderItemsComponent, ReceiptPrintComponent, KitchenTicketPrintComponent],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly injector = inject(Injector);

  readonly id = input.required<string>();
  readonly statusLabel = STATUS_SHORT;
  readonly order = remote<Order>(() => this.admin.order(Number(this.id())), { lazy: true });
  readonly busy = signal(false);
  readonly printMode = signal<'receipt' | 'ticket' | null>(null);
  readonly cancelOpen = signal(false);
  readonly cancelTouched = signal(false);
  cancelReason = '';

  readonly canCancel = computed(() => {
    const o = this.order.data();
    return !!o && CANCELLABLE.includes(o.status);
  });
  readonly history = computed(() => [...(this.order.data()?.history ?? [])].reverse());

  constructor() {
    effect(() => {
      this.id();
      untracked(() => this.order.reload());
    });
  }

  advance() {
    const o = this.order.data();
    if (!o?.nextStatus) return;
    this.update(o, o.nextStatus, null, `Order marked as ${STATUS_SHORT[o.nextStatus].toLowerCase()}`);
  }

  openCancel() {
    this.cancelReason = '';
    this.cancelTouched.set(false);
    this.cancelOpen.set(true);
  }

  confirmCancel() {
    this.cancelTouched.set(true);
    const o = this.order.data();
    if (!o || !this.cancelReason.trim()) return;
    this.update(o, 'CANCELLED', this.cancelReason.trim(), 'Order cancelled', () => this.cancelOpen.set(false));
  }

  private update(o: Order, status: OrderStatus, note: string | null, msg: string, done?: () => void) {
    this.busy.set(true);
    this.admin.updateOrderStatus(o.id, status, note).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.busy.set(false);
        this.toast.success(msg);
        done?.();
      },
      error: (err) => {
        this.busy.set(false);
        this.toast.error(errorMessage(err, 'Could not update the order.'));
      },
    });
  }

  print(mode: 'receipt' | 'ticket') {
    if (this.printMode() === mode) {
      window.print();
      return;
    }
    this.printMode.set(mode);
    afterNextRender(() => window.print(), { injector: this.injector });
  }
}
