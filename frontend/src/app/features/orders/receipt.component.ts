import { Component, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Order } from '../../core/models/api.models';
import { SettingsService } from '../../core/services/settings.service';
import { MoneyPipe } from '../../core/utils/money.pipe';

/** Plain, printer-friendly receipt (shown only when printing). */
@Component({
  selector: 'app-receipt',
  imports: [DatePipe, MoneyPipe],
  templateUrl: './receipt.component.html',
})
export class ReceiptComponent {
  readonly settings = inject(SettingsService);
  readonly order = input.required<Order>();
}
