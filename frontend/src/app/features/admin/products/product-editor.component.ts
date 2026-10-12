import { Component, ElementRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { map, startWith } from 'rxjs';
import { ProductDetail } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { applyServerErrors, fieldError, slugify } from '../shared/form-utils';
import { ComboEditorComponent } from './combo-editor.component';
import { GroupsPickerComponent } from './groups-picker.component';
import { ImageUploadButtonComponent } from './image-upload-button.component';
import { buildProductForm, galleryControl, patchProductForm, toProductRequest } from './product-form';
import { VariantsEditorComponent } from './variants-editor.component';

@Component({
  selector: 'app-admin-product-editor',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, ErrorStateComponent, VariantsEditorComponent, GroupsPickerComponent,
    ComboEditorComponent, ImageUploadButtonComponent],
  templateUrl: './product-editor.component.html',
})
export class ProductEditorComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Route param — absent on /admin/products/new. */
  readonly id = input<string>();
  readonly isEdit = computed(() => !!this.id() && this.id() !== 'new');

  readonly form = buildProductForm();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly imgBroken = signal(false);

  readonly categories = remote(() => this.admin.categories());
  readonly groups = remote(() => this.admin.groups());
  readonly comboProducts = remote(() => this.admin.products({ type: 'STANDARD', size: 200 }));
  readonly product = remote<ProductDetail>(() => this.admin.product(Number(this.id())), { lazy: true });

  readonly isCombo = toSignal(this.form.controls.productType.valueChanges.pipe(startWith(this.form.controls.productType.value), map((t) => t === 'COMBO')), { initialValue: false });
  readonly imageUrl = toSignal(this.form.controls.imageUrl.valueChanges.pipe(startWith('')), { initialValue: '' });
  readonly standardProducts = computed(() => (this.comboProducts.data()?.content ?? []).filter((p) => p.id !== Number(this.id())));

  readonly types = [
    { value: 'STANDARD', label: 'Standard' },
    { value: 'COMBO', label: 'Combo' },
  ] as const;
  readonly flags = [
    { key: 'available', label: 'In stock' },
    { key: 'visible', label: 'Visible' },
    { key: 'featured', label: 'Featured' },
    { key: 'bestseller', label: 'Bestseller' },
    { key: 'newArrival', label: 'New' },
    { key: 'vegetarian', label: 'Vegetarian' },
    { key: 'spicy', label: 'Spicy' },
  ] as const;

  constructor() {
    effect(() => {
      if (this.isEdit()) untracked(() => this.product.reload());
    });
    effect(() => {
      const p = this.product.data();
      if (p) untracked(() => patchProductForm(this.form, p));
    });
    this.form.controls.imageUrl.valueChanges.subscribe(() => this.imgBroken.set(false));
  }

  err(key: keyof typeof this.form.controls, label: string): string | null {
    return fieldError(this.form.controls[key], label);
  }

  autoSlug() {
    const slug = this.form.controls.slug;
    if (!this.isEdit() && !slug.value && this.form.controls.name.value) slug.setValue(slugify(this.form.controls.name.value));
  }

  setMainImage(url: string) {
    this.form.controls.imageUrl.setValue(url);
    this.form.controls.imageUrl.markAsDirty();
  }

  addGallery(url: string) {
    this.form.controls.galleryImages.push(galleryControl(url));
    this.form.controls.galleryImages.markAsDirty();
  }

  removeGallery(i: number) {
    this.form.controls.galleryImages.removeAt(i);
    this.form.controls.galleryImages.markAsDirty();
  }

  save() {
    this.formError.set(null);
    this.form.markAllAsTouched();
    const combo = this.form.controls.productType.value === 'COMBO';
    if (combo && this.form.controls.comboComponents.length === 0) {
      this.formError.set('A combo needs at least one component.');
      return;
    }
    const variants = this.form.controls.variants.value;
    if (variants.length && variants.filter((v) => v.defaultVariant).length !== 1) {
      this.formError.set('Pick exactly one default variant.');
      return;
    }
    // ignore combo validity for standard products (that array is not sent)
    const invalid = Object.entries(this.form.controls).some(([k, c]) => (k !== 'comboComponents' || combo) && c.invalid);
    if (invalid) {
      this.formError.set('Please fix the highlighted fields.');
      this.focusFirstInvalid();
      return;
    }
    const body = toProductRequest(this.form);
    this.saving.set(true);
    const req$ = this.isEdit() ? this.admin.updateProduct(Number(this.id()), body) : this.admin.createProduct(body);
    req$.subscribe({
      next: (p) => {
        this.saving.set(false);
        this.toast.success(this.isEdit() ? `${p.summary.name} saved` : `${p.summary.name} created`);
        if (this.isEdit()) {
          this.product.set(p);
        } else {
          this.form.markAsPristine();
          this.router.navigate(['/admin/products', p.summary.id], { replaceUrl: true });
        }
      },
      error: (err) => {
        this.saving.set(false);
        const msg = applyServerErrors(this.form, err);
        this.formError.set(msg ?? 'Please fix the highlighted fields.');
        this.toast.error(msg ?? 'Please fix the highlighted fields.');
        this.focusFirstInvalid();
      },
    });
  }

  async remove() {
    const p = this.product.data();
    if (!p) return;
    const ok = await this.confirm.ask({
      title: `Delete ${p.summary.name}?`,
      message: 'It will be removed from the menu. This cannot be undone.',
      confirmLabel: 'Delete product',
      danger: true,
    });
    if (!ok) return;
    this.admin.deleteProduct(p.summary.id).subscribe({
      next: () => {
        this.toast.success(`${p.summary.name} deleted`);
        this.router.navigate(['/admin/products']);
      },
      error: (err) => this.toast.error(applyServerErrors(this.form, err) ?? 'Could not delete the product.'),
    });
  }

  private focusFirstInvalid() {
    setTimeout(() => {
      const el = (this.host.nativeElement as HTMLElement).querySelector<HTMLElement>('.ng-invalid:not(form):not(fieldset):not([formarrayname]):not([formgroupname])');
      el?.focus();
      el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    });
  }
}
