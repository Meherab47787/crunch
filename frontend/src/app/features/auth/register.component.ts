import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { apiError, errorMessage } from '../../core/utils/errors';
import { AuthShellComponent } from './auth-shell.component';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, AuthShellComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  readonly returnUrl = input<string>();
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.pattern(/^[+0-9 ()-]{7,30}$/)],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
  });

  bad(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return c.invalid && c.touched;
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.auth.register({ ...v, phone: v.phone.trim() || null }).subscribe({
      next: (r) => {
        this.toast.success(`Welcome to Crust & Crunch, ${r.user.fullName.split(' ')[0]}!`);
        this.router.navigateByUrl(this.returnUrl()?.startsWith('/') ? this.returnUrl()! : '/menu');
      },
      error: (err) => {
        this.busy.set(false);
        const fe = apiError(err)?.fieldErrors;
        if (fe) for (const k of Object.keys(fe)) this.form.get(k)?.setErrors({ server: fe[k] });
        this.error.set(errorMessage(err));
      },
    });
  }
}
