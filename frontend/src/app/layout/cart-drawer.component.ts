import { Component, HostListener, computed, effect, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { CartLine, CartService } from '../core/services/cart.service';
import { ToastService } from '../core/services/toast.service';
import { MoneyPipe } from '../core/utils/money.pipe';
import { EmptyStateComponent } from '../shared/components/empty-state.component';
import { QuantityStepperComponent } from '../shared/components/quantity-stepper.component';

@Component({
  selector: 'app-cart-drawer',
  imports: [RouterLink, LucideAngularModule, MoneyPipe, QuantityStepperComponent, EmptyStateComponent],
  templateUrl: './cart-drawer.component.html',
})
export class CartDrawerComponent {
  readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly freeDeliveryGap = computed(() => {
    const q = this.cart.quote();
    if (!q?.freeDeliveryThreshold || this.cart.fulfillmentType() !== 'DELIVERY') return 0;
    return Math.max(0, q.freeDeliveryThreshold - (q.subtotal - q.discountTotal));
  });

  constructor() {
    effect(() => {
      document.body.style.overflow = this.cart.drawerOpen() ? 'hidden' : '';
    });
  }

  quoteLine(i: number) {
    return this.cart.quote()?.lines[i] ?? null;
  }

  setQty(line: CartLine, qty: number) {
    if (qty <= 0) this.remove(line);
    else this.cart.setQuantity(line.key, qty);
  }

  remove(line: CartLine) {
    this.cart.remove(line.key);
    this.toast.success(`Removed ${line.name}`, { label: 'Undo', run: () => this.cart.undoRemove() });
  }

  checkout() {
    this.close();
    this.router.navigateByUrl('/checkout');
  }

  @HostListener('document:keydown.escape')
  close() {
    this.cart.drawerOpen.set(false);
  }
}
