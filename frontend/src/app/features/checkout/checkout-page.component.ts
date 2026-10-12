import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { of } from 'rxjs';
import { Address, CheckoutRequest, PaymentMethod } from '../../core/models/api.models';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { RecentOrdersService } from '../../core/services/recent-orders.service';
import { SettingsService } from '../../core/services/settings.service';
import { apiError, errorMessage } from '../../core/utils/errors';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { uuid } from '../../core/utils/uuid';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { FulfillmentToggleComponent } from '../cart/fulfillment-toggle.component';
import { CouponFieldComponent } from '../cart/coupon-field.component';
import { QuoteSummaryComponent } from '../cart/quote-summary.component';

const KEY_STORAGE = 'cc.checkout.idempotencyKey';
const PHONE = /^[+0-9 ()-]{7,30}$/;

@Component({
  selector: 'app-checkout-page',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, MoneyPipe, EmptyStateComponent,
    FulfillmentToggleComponent, CouponFieldComponent, QuoteSummaryComponent],
  templateUrl: './checkout-page.component.html',
  styleUrl: './checkout-page.component.css',
})
export class CheckoutPageComponent {
  readonly cart = inject(CartService);
  readonly auth = inject(AuthService);
  readonly settings = inject(SettingsService);
  private readonly orders = inject(OrderService);
  private readonly account = inject(AccountService);
  private readonly recent = inject(RecentOrdersService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly payments: { value: PaymentMethod; label: string; hint: string; icon: string }[] = [
    { value: 'CASH', label: 'Cash', hint: 'Pay when your food arrives.', icon: 'banknote' },
    { value: 'ONLINE', label: 'Pay online now', hint: 'Demo gateway — no card details are collected or stored.', icon: 'credit-card' },
  ];

  readonly form = this.fb.nonNullable.group({
    contact: this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(PHONE)]],
    }),
    address: this.fb.nonNullable.group({
      line1: ['', [Validators.required, Validators.maxLength(200)]],
      line2: [''],
      city: ['', [Validators.required, Validators.maxLength(100)]],
      postalCode: [''],
    }),
    deliveryInstructions: [''],
    specialInstructions: [''],
    allergyNotes: [''],
    paymentMethod: ['CASH' as PaymentMethod],
  });

  readonly addresses = toSignal(this.auth.isLoggedIn() ? this.account.addresses() : of([] as Address[]), { initialValue: [] });
  readonly selectedAddressId = signal<number | null>(null);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly canSubmit = computed(
    () => !this.submitting() && !this.cart.quoteLoading() && this.cart.quote()?.valid === true,
  );

  constructor() {
    const u = this.auth.user();
    if (u) this.form.controls.contact.patchValue({ name: u.fullName, email: u.email, phone: u.phone ?? '' });
    // Only validate the address block for delivery orders.
    effect(() => {
      const addr = this.form.controls.address;
      if (this.cart.fulfillmentType() === 'DELIVERY') addr.enable();
      else addr.disable();
    });
    effect(() => {
      const list = this.addresses();
      if (list.length && this.selectedAddressId() == null) this.useAddress(list.find((a) => a.defaultAddress) ?? list[0]);
    });
  }

  useAddress(a: Address): void {
    this.selectedAddressId.set(a.id);
    this.form.controls.address.patchValue({ line1: a.line1, line2: a.line2 ?? '', city: a.city, postalCode: a.postalCode ?? '' });
    if (a.instructions && !this.form.controls.deliveryInstructions.value) {
      this.form.controls.deliveryInstructions.setValue(a.instructions);
    }
  }

  invalid(path: string): boolean {
    const c = this.form.get(path);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  /**
   * One idempotency key per checkout attempt. Retries (double click, network error) reuse it,
   * so the server returns the original order instead of creating a duplicate.
   */
  private idempotencyKey(): string {
    let key = sessionStorage.getItem(KEY_STORAGE);
    if (!key) {
      key = uuid();
      sessionStorage.setItem(KEY_STORAGE, key);
    }
    return key;
  }

  placeOrder(): void {
    this.submitError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      document.querySelector<HTMLElement>('.ng-invalid input, input.ng-invalid')?.focus();
      return;
    }
    if (!this.canSubmit()) return;
    const v = this.form.getRawValue();
    const delivery = this.cart.fulfillmentType() === 'DELIVERY';
    const req: CheckoutRequest = {
      items: this.cart.requests(),
      couponCode: this.cart.quote()?.couponApplied ? this.cart.couponCode() : null,
      fulfillmentType: this.cart.fulfillmentType(),
      contact: { name: v.contact.name.trim(), email: v.contact.email.trim(), phone: v.contact.phone.trim() },
      address: delivery
        ? { line1: v.address.line1.trim(), line2: v.address.line2.trim() || null, city: v.address.city.trim(), postalCode: v.address.postalCode.trim() || null }
        : null,
      deliveryInstructions: delivery ? v.deliveryInstructions.trim() || null : null,
      specialInstructions: v.specialInstructions.trim() || null,
      allergyNotes: v.allergyNotes.trim() || null,
      paymentMethod: v.paymentMethod,
    };
    this.submitting.set(true);
    this.orders.checkout(req, this.idempotencyKey()).subscribe({
      next: (order) => {
        sessionStorage.removeItem(KEY_STORAGE);
        if (order.trackingToken) {
          this.recent.remember({ orderNumber: order.orderNumber, token: order.trackingToken, placedAt: order.createdAt });
        }
        this.cart.clear();
        this.router.navigate(['/orders', order.orderNumber], { queryParams: { token: order.trackingToken, placed: 1 } });
      },
      error: (err) => {
        this.submitting.set(false);
        const status = apiError(err)?.status;
        // Business-rule failures mean the cart itself changed — a fresh attempt gets a fresh key.
        if (status === 422 || status === 409) sessionStorage.removeItem(KEY_STORAGE);
        this.submitError.set(errorMessage(err));
        this.cart.refreshQuote(); // re-price so sold-out / changed lines are flagged
      },
    });
  }
}
