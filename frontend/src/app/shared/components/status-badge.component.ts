import { Component, computed, input } from '@angular/core';
import { OrderStatus } from '../../core/models/api.models';
import { STATUS_CLASS, STATUS_LABEL, STATUS_SHORT } from '../../core/utils/order-status';

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
})
export class StatusBadgeComponent {
  readonly status = input.required<OrderStatus>();
  readonly staff = input(false);
  readonly cls = computed(() => 'chip whitespace-nowrap ' + STATUS_CLASS[this.status()]);
  readonly text = computed(() => (this.staff() ? STATUS_SHORT : STATUS_LABEL)[this.status()]);
}
