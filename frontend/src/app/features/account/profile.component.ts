import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { errorMessage } from '../../core/utils/errors';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent {
  readonly auth = inject(AuthService);
  private readonly account = inject(AccountService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  readonly savingProfile = signal(false);
  readonly savingPassword = signal(false);

  readonly profile = this.fb.nonNullable.group({
    fullName: [this.auth.user()?.fullName ?? '', [Validators.required, Validators.maxLength(120)]],
    phone: [this.auth.user()?.phone ?? '', Validators.pattern(/^[+0-9 ()-]{7,30}$/)],
  });
  readonly password = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  saveProfile() {
    if (this.profile.invalid) return this.profile.markAllAsTouched();
    this.savingProfile.set(true);
    const v = this.profile.getRawValue();
    this.account.updateProfile({ fullName: v.fullName.trim(), phone: v.phone.trim() || null }).subscribe({
      next: (u) => {
        this.auth.updateUser(u);
        this.profile.markAsPristine();
        this.savingProfile.set(false);
        this.toast.success('Profile updated');
      },
      error: (e) => {
        this.savingProfile.set(false);
        this.toast.error(errorMessage(e));
      },
    });
  }

  savePassword() {
    this.savingPassword.set(true);
    this.account.changePassword(this.password.getRawValue()).subscribe({
      next: () => {
        this.password.reset();
        this.savingPassword.set(false);
        this.toast.success('Password changed');
      },
      error: (e) => {
        this.savingPassword.set(false);
        this.toast.error(errorMessage(e));
      },
    });
  }
}
