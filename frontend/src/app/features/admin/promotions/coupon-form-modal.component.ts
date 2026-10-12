import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Category, Coupon, CouponRequest, DiscountType } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal.component';
import { applyServerErrors, blankToNull, fieldError, fromLocalInput, toLocalInput } from '../shared/form-utils';

const num = (v: unknown): number | null => (v === null || v === undefined || v === '' ? null : Number(v));

@Component({
  selector: 'app-coupon-form-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, ModalComponent],
  templateUrl: './coupon-form-modal.component.html',
})
export class CouponFormModalComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly coupon = input<Coupon | null>(null);
  readonly categories = input<Category[]>([]);
  readonly closed = output<void>();
  readonly saved = output<void>();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly err = fieldError;

  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(40), Validators.pattern(/^[A-Za-z0-9_-]+$/)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    discountType: new FormControl<DiscountType>('PERCENT', { nonNullable: true }),
    discountValue: new FormControl<number | null>(10, { validators: [Validators.required, Validators.min(0.01)] }),
    minOrderAmount: new FormControl<number | null>(0, { validators: [Validators.required, Validators.min(0)] }),
    maxDiscountAmount: new FormControl<number | null>(null, { validators: [Validators.min(0.01)] }),
    categoryId: new FormControl<number | null>(null),
    startsAt: new FormControl('', { nonNullable: true }),
    expiresAt: new FormControl('', { nonNullable: true }),
    maxRedemptions: new FormControl<number | null>(null, { validators: [Validators.min(1)] }),
    perUserLimit: new FormControl<number | null>(null, { validators: [Validators.min(1)] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  ngOnInit() {
    const c = this.coupon();
    if (!c) return;
    this.form.reset({
      code: c.code, description: c.description ?? '', discountType: c.discountType, discountValue: c.discountValue,
      minOrderAmount: c.minOrderAmount, maxDiscountAmount: c.maxDiscountAmount, categoryId: c.categoryId,
      startsAt: toLocalInput(c.startsAt), expiresAt: toLocalInput(c.expiresAt),
      maxRedemptions: c.maxRedemptions, perUserLimit: c.perUserLimit, active: c.active,
    });
  }

  save() {
    this.form.markAllAsTouched();
    this.formError.set(null);
    const v = this.form.getRawValue();
    if (v.discountType === 'PERCENT' && Number(v.discountValue) > 100) {
      this.form.controls.discountValue.setErrors({ server: 'Percent cannot exceed 100.' });
      return;
    }
    const starts = fromLocalInput(v.startsAt);
    const expires = fromLocalInput(v.expiresAt);
    if (starts && expires && expires <= starts) {
      this.form.controls.expiresAt.setErrors({ server: 'Expiry must be after the start.' });
      return;
    }
    if (this.form.invalid) return;
    const body: CouponRequest = {
      code: v.code.trim().toUpperCase(),
      description: blankToNull(v.description),
      discountType: v.discountType,
      discountValue: Number(v.discountValue),
      minOrderAmount: num(v.minOrderAmount) ?? 0,
      maxDiscountAmount: num(v.maxDiscountAmount),
      categoryId: v.categoryId,
      startsAt: starts,
      expiresAt: expires,
      maxRedemptions: num(v.maxRedemptions),
      perUserLimit: num(v.perUserLimit),
      active: v.active,
    };
    const c = this.coupon();
    this.saving.set(true);
    (c ? this.admin.updateCoupon(c.id, body) : this.admin.createCoupon(body)).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(c ? 'Coupon saved' : 'Coupon created');
        this.saved.emit();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }
}
