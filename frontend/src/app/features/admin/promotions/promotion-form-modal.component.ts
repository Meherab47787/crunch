import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Category, DiscountType, ProductSummary, Promotion, PromotionRequest } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal.component';
import { ImageUploadButtonComponent } from '../products/image-upload-button.component';
import { applyServerErrors, blankToNull, fieldError, fromLocalInput, toLocalInput } from '../shared/form-utils';

type Target = 'CATEGORY' | 'PRODUCT';

@Component({
  selector: 'app-promotion-form-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, ModalComponent, ImageUploadButtonComponent],
  templateUrl: './promotion-form-modal.component.html',
})
export class PromotionFormModalComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly promotion = input<Promotion | null>(null);
  readonly categories = input<Category[]>([]);
  readonly products = input<ProductSummary[]>([]);
  readonly closed = output<void>();
  readonly saved = output<void>();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly targetError = signal<string | null>(null);
  readonly target = signal<Target>('CATEGORY');
  readonly err = fieldError;

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    imageUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    discountType: new FormControl<DiscountType>('PERCENT', { nonNullable: true }),
    discountValue: new FormControl<number | null>(10, { validators: [Validators.required, Validators.min(0.01)] }),
    categoryId: new FormControl<number | null>(null),
    productId: new FormControl<number | null>(null),
    startsAt: new FormControl('', { nonNullable: true }),
    endsAt: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  ngOnInit() {
    const p = this.promotion();
    if (!p) return;
    this.target.set(p.productId ? 'PRODUCT' : 'CATEGORY');
    this.form.reset({
      name: p.name, description: p.description ?? '', imageUrl: p.imageUrl ?? '', discountType: p.discountType, discountValue: p.discountValue,
      categoryId: p.categoryId, productId: p.productId, startsAt: toLocalInput(p.startsAt), endsAt: toLocalInput(p.endsAt), active: p.active,
    });
  }

  setTarget(t: Target) {
    this.target.set(t);
    this.targetError.set(null);
  }

  save() {
    this.form.markAllAsTouched();
    this.formError.set(null);
    this.targetError.set(null);
    const v = this.form.getRawValue();
    const categoryId = this.target() === 'CATEGORY' ? v.categoryId : null;
    const productId = this.target() === 'PRODUCT' ? v.productId : null;
    if (!categoryId && !productId) {
      this.targetError.set(this.target() === 'CATEGORY' ? 'Choose a category.' : 'Choose a product.');
      return;
    }
    if (v.discountType === 'PERCENT' && Number(v.discountValue) > 100) {
      this.form.controls.discountValue.setErrors({ server: 'Percent cannot exceed 100.' });
      return;
    }
    const starts = fromLocalInput(v.startsAt);
    const ends = fromLocalInput(v.endsAt);
    if (starts && ends && ends <= starts) {
      this.form.controls.endsAt.setErrors({ server: 'End must be after the start.' });
      return;
    }
    if (this.form.invalid) return;
    const body: PromotionRequest = {
      name: v.name.trim(),
      description: blankToNull(v.description),
      imageUrl: blankToNull(v.imageUrl),
      discountType: v.discountType,
      discountValue: Number(v.discountValue),
      categoryId,
      productId,
      startsAt: starts,
      endsAt: ends,
      active: v.active,
    };
    const p = this.promotion();
    this.saving.set(true);
    (p ? this.admin.updatePromotion(p.id, body) : this.admin.createPromotion(body)).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(p ? 'Promotion saved' : 'Promotion created');
        this.saved.emit();
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }
}
