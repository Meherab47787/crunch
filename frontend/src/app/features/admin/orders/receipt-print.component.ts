import { DatePipe } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { Order } from '../../../core/models/api.models';
import { SettingsService } from '../../../core/services/settings.service';
import { MoneyPipe } from '../../../core/utils/money.pipe';

/** Customer receipt for printing (rendered inside a `.print-only` wrapper). */
@Component({
  selector: 'app-receipt-print',
  imports: [DatePipe, MoneyPipe],
  templateUrl: './receipt-print.component.html',
})
export class ReceiptPrintComponent {
  readonly order = input.required<Order>();
  readonly settings = inject(SettingsService);
}
