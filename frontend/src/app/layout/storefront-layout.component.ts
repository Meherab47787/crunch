import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { filter } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { CartService } from '../core/services/cart.service';
import { MenuService } from '../core/services/menu.service';
import { SettingsService } from '../core/services/settings.service';
import { LogoComponent } from '../shared/components/logo.component';
import { CartDrawerComponent } from './cart-drawer.component';
import { MoneyPipe } from '../core/utils/money.pipe';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-storefront-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule, LogoComponent, CartDrawerComponent, MoneyPipe],
  templateUrl: './storefront-layout.component.html',
})
export class StorefrontLayoutComponent {
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  readonly settings = inject(SettingsService);
  private readonly menu = inject(MenuService);
  private readonly router = inject(Router);

  readonly mobileOpen = signal(false);
  readonly categories = toSignal(this.menu.categories());
  readonly year = new Date().getFullYear();
  readonly links = [
    { path: '/', label: 'Home', exact: true },
    { path: '/menu', label: 'Menu', exact: false },
    { path: '/deals', label: 'Deals', exact: false },
    { path: '/track', label: 'Track order', exact: false },
  ];

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.mobileOpen.set(false));
  }

  firstName(): string {
    return this.auth.user()?.fullName.split(' ')[0] ?? 'Account';
  }
}
