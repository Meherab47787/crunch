import { Component, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Quote } from '../../core/models/api.models';
import { MoneyPipe } from '../../core/utils/money.pipe';

/** Server-calculated totals. Used by the cart and checkout pages. */
@Component({
  selector: 'app-quote-summary',
  imports: [LucideAngularModule, MoneyPipe],
  templateUrl: './quote-summary.component.html',
})
export class QuoteSummaryComponent {
  readonly quote = input<Quote | null>(null);
  readonly loading = input(false);
  readonly showDelivery = input(true);
}
