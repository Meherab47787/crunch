import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Category, CategoryRequest } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal.component';
import { SLUG_PATTERN, applyServerErrors, blankToNull, fieldError, slugify } from '../shared/form-utils';
import { ImageUploadButtonComponent } from '../products/image-upload-button.component';
import { IconCheck } from '../shared/icon-check';

@Component({
  selector: 'app-category-form-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, ModalComponent, ImageUploadButtonComponent],
  templateUrl: './category-form-modal.component.html',
})
export class CategoryFormModalComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly icons = inject(IconCheck);
  readonly category = input<Category | null>(null);
  readonly closed = output<void>();
  readonly saved = output<Category>();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly iconSuggestions = ['pizza', 'beef', 'drumstick', 'sandwich', 'popcorn', 'cup-soda', 'cake-slice', 'soup', 'leaf', 'flame', 'utensils', 'package', 'wheat', 'star'];

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(80), Validators.pattern(SLUG_PATTERN)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    imageUrl: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    icon: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(40)] }),
    sortOrder: new FormControl(0, { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  ngOnInit() {
    const c = this.category();
    if (c) {
      this.form.reset({ name: c.name, slug: c.slug, description: c.description ?? '', imageUrl: c.imageUrl ?? '', icon: c.icon ?? '', sortOrder: c.sortOrder, active: c.active });
    }
  }

  err(k: keyof typeof this.form.controls, label: string) {
    return fieldError(this.form.controls[k], label);
  }

  autoSlug() {
    if (!this.category() && !this.form.controls.slug.value && this.form.controls.name.value) {
      this.form.controls.slug.setValue(slugify(this.form.controls.name.value));
    }
  }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const body: CategoryRequest = {
      name: v.name.trim(),
      slug: blankToNull(v.slug),
      description: blankToNull(v.description),
      imageUrl: blankToNull(v.imageUrl),
      icon: blankToNull(v.icon),
      sortOrder: Number(v.sortOrder) || 0,
      active: v.active,
    };
    const c = this.category();
    this.saving.set(true);
    this.formError.set(null);
    (c ? this.admin.updateCategory(c.id, body) : this.admin.createCategory(body)).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.toast.success(c ? 'Category saved' : 'Category created');
        this.saved.emit(res);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }
}
