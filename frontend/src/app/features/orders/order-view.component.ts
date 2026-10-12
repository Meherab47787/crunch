import { Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { Order, OrderStatus } from '../../core/models/api.models';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { STATUS_LABEL } from '../../core/utils/order-status';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';

interface Step {
  status: OrderStatus;
  label: string;
  icon: string;
  at: string | null;
}

/** Customer-facing order view: live progress, items with customizations, totals. */
@Component({
  selector: 'app-order-view',
  imports: [DatePipe, LucideAngularModule, MoneyPipe, StatusBadgeComponent],
  templateUrl: './order-view.component.html',
})
export class OrderViewComponent {
  readonly order = input.required<Order>();

  readonly active = computed(() => !['DELIVERED', 'CANCELLED'].includes(this.order().status));

  readonly steps = computed<Step[]>(() => {
    const o = this.order();
    // latest entry wins: the kitchen can move a ticket back and forward again
    const at = (s: OrderStatus) => o.history.filter((h) => h.toStatus === s).pop()?.createdAt ?? null;
    const base: Step[] = [
      { status: 'PENDING', label: 'Received', icon: 'receipt', at: at('PENDING') },
      { status: 'CONFIRMED', label: 'Confirmed', icon: 'check', at: at('CONFIRMED') },
      { status: 'PREPARING', label: 'Cooking', icon: 'chef-hat', at: at('PREPARING') },
    ];
    return o.fulfillmentType === 'DELIVERY'
      ? [...base,
        { status: 'OUT_FOR_DELIVERY', label: 'On the way', icon: 'bike', at: at('OUT_FOR_DELIVERY') },
        { status: 'DELIVERED', label: 'Delivered', icon: 'house', at: at('DELIVERED') }]
      : [...base,
        { status: 'READY_FOR_PICKUP', label: 'Ready', icon: 'package-check', at: at('READY_FOR_PICKUP') },
        { status: 'DELIVERED', label: 'Collected', icon: 'circle-check', at: at('DELIVERED') }];
  });

  readonly currentIndex = computed(() => {
    const o = this.order();
    // READY_FOR_PICKUP on a delivery order sits between cooking and on-the-way
    const status = o.fulfillmentType === 'DELIVERY' && o.status === 'READY_FOR_PICKUP' ? 'PREPARING' : o.status;
    return this.steps().findIndex((s) => s.status === status);
  });

  readonly headline = computed(() => {
    const o = this.order();
    if (o.status === 'READY_FOR_PICKUP') return o.fulfillmentType === 'DELIVERY' ? 'Packed and waiting for a rider' : 'Ready for pickup!';
    if (o.status === 'DELIVERED') return o.fulfillmentType === 'DELIVERY' ? 'Delivered. Enjoy!' : 'Collected. Enjoy!';
    return STATUS_LABEL[o.status];
  });
}
