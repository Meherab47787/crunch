import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';
import { RecentOrdersService } from '../../core/services/recent-orders.service';

@Component({
  selector: 'app-track-lookup',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, LucideAngularModule],
  templateUrl: './track-lookup.component.html',
})
export class TrackLookupComponent {
  readonly auth = inject(AuthService);
  readonly recent = inject(RecentOrdersService);
  private readonly router = inject(Router);
  readonly form = inject(FormBuilder).nonNullable.group({
    orderNumber: ['', Validators.required],
    token: ['', Validators.required],
  });

  go() {
    const v = this.form.getRawValue();
    this.router.navigate(['/orders', v.orderNumber.trim().toUpperCase()], { queryParams: { token: v.token.trim() } });
  }
}
