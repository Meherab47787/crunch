import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { LogoComponent } from '../../shared/components/logo.component';

interface NavLink {
  path: string;
  label: string;
  icon: string;
  exact?: boolean;
}

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule, LogoComponent],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent {
  readonly auth = inject(AuthService);
  readonly open = signal(false);
  readonly links: NavLink[] = [
    { path: '/admin', label: 'Dashboard', icon: 'layout-dashboard', exact: true },
    { path: '/admin/orders', label: 'Orders', icon: 'receipt' },
    { path: '/admin/products', label: 'Products', icon: 'pizza' },
    { path: '/admin/categories', label: 'Categories', icon: 'grid-2x2' },
    { path: '/admin/option-groups', label: 'Option groups', icon: 'sliders-horizontal' },
    { path: '/admin/promotions', label: 'Promotions & coupons', icon: 'percent' },
    { path: '/admin/customers', label: 'Customers', icon: 'users' },
    { path: '/admin/staff', label: 'Staff', icon: 'lock' },
  ];

  constructor() {
    inject(Router)
      .events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.open.set(false));
  }

  initial(): string {
    return (this.auth.user()?.fullName ?? 'A').charAt(0).toUpperCase();
  }
}
