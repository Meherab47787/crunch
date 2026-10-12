import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-coupon-field',
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './coupon-field.component.html',
})
export class CouponFieldComponent {
  readonly cart = inject(CartService);
  readonly code = signal('');

  apply() {
    this.cart.applyCoupon(this.code());
    this.code.set('');
  }
}
