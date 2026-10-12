import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { CartLine, CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { QuantityStepperComponent } from '../../shared/components/quantity-stepper.component';
import { CouponFieldComponent } from './coupon-field.component';
import { FulfillmentToggleComponent } from './fulfillment-toggle.component';
import { QuoteSummaryComponent } from './quote-summary.component';

@Component({
  selector: 'app-cart-page',
  imports: [RouterLink, LucideAngularModule, MoneyPipe, EmptyStateComponent, QuantityStepperComponent,
    QuoteSummaryComponent, CouponFieldComponent, FulfillmentToggleComponent],
  templateUrl: './cart-page.component.html',
})
export class CartPageComponent {
  readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);

  quoteIssues(): string[] {
    const q = this.cart.quote();
    // line errors are shown inline; only show cart-level issues here
    return (q?.issues ?? []).filter((i) => !q?.lines.some((l) => l.error === i));
  }

  remove(line: CartLine) {
    this.cart.remove(line.key);
    this.toast.success(`Removed ${line.name}`, { label: 'Undo', run: () => this.cart.undoRemove() });
  }
}
