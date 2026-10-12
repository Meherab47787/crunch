import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { Order } from '../../core/models/api.models';

/**
 * Plain black-on-white kitchen ticket, meant to be rendered inside a `.print-only` wrapper.
 * No prices — just what the line needs to cook.
 */
@Component({
  selector: 'app-kitchen-ticket-print',
  imports: [DatePipe],
  templateUrl: './kitchen-ticket-print.component.html',
})
export class KitchenTicketPrintComponent {
  readonly order = input.required<Order>();
  readonly printedAt = new Date();
}
