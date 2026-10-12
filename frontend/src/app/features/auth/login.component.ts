import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { errorMessage } from '../../core/utils/errors';
import { AuthShellComponent } from './auth-shell.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, AuthShellComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  readonly returnUrl = input<string>();
  readonly busy = signal(false);
  readonly show = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: (r) => {
        this.toast.success(`Welcome back, ${r.user.fullName.split(' ')[0]}!`);
        const target = this.returnUrl()?.startsWith('/') ? this.returnUrl()! : this.auth.homeFor(r.user);
        this.router.navigateByUrl(target);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
