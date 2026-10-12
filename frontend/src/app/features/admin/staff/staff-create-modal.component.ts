import { Component, inject, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Role, User } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal.component';
import { applyServerErrors, blankToNull, fieldError } from '../shared/form-utils';

@Component({
  selector: 'app-staff-create-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, ModalComponent],
  templateUrl: './staff-create-modal.component.html',
})
export class StaffCreateModalComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly closed = output<void>();
  readonly saved = output<User>();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly err = fieldError;
  readonly form = new FormGroup({
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(30)] }),
    role: new FormControl<Role>('KITCHEN_STAFF', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(100)] }),
  });

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);
    this.admin.createStaff({ fullName: v.fullName.trim(), email: v.email.trim(), phone: blankToNull(v.phone), role: v.role, password: v.password }).subscribe({
      next: (u) => {
        this.saving.set(false);
        this.toast.success(`${u.fullName} added`);
        this.saved.emit(u);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }
}
