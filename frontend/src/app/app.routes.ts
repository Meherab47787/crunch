import { Routes } from '@angular/router';
import { authGuard, guestOnlyGuard, roleGuard } from './core/guards/auth.guards';
import { StorefrontLayoutComponent } from './layout/storefront-layout.component';

export const routes: Routes = [
  {
    path: 'kitchen',
    canActivate: [roleGuard('ADMIN', 'KITCHEN_STAFF')],
    title: 'Kitchen display · Crust & Crunch',
    loadComponent: () => import('./features/kitchen/kitchen-display.component').then((m) => m.KitchenDisplayComponent),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    component: StorefrontLayoutComponent,
    children: [
      {
        path: '',
        title: 'Crust & Crunch — Pizza, Burgers & Fried Chicken',
        loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'menu/item/:slug',
        loadComponent: () => import('./features/product/product-page.component').then((m) => m.ProductPageComponent),
      },
      {
        path: 'menu',
        title: 'Menu · Crust & Crunch',
        loadComponent: () => import('./features/menu/menu-page.component').then((m) => m.MenuPageComponent),
      },
      {
        path: 'menu/:category',
        title: 'Menu · Crust & Crunch',
        loadComponent: () => import('./features/menu/menu-page.component').then((m) => m.MenuPageComponent),
      },
      {
        path: 'deals',
        title: 'Deals · Crust & Crunch',
        loadComponent: () => import('./features/menu/deals-page.component').then((m) => m.DealsPageComponent),
      },
      {
        path: 'cart',
        title: 'Your bag · Crust & Crunch',
        loadComponent: () => import('./features/cart/cart-page.component').then((m) => m.CartPageComponent),
      },
      {
        path: 'checkout',
        title: 'Checkout · Crust & Crunch',
        loadComponent: () => import('./features/checkout/checkout-page.component').then((m) => m.CheckoutPageComponent),
      },
      {
        path: 'track',
        title: 'Track your order · Crust & Crunch',
        loadComponent: () => import('./features/orders/track-lookup.component').then((m) => m.TrackLookupComponent),
      },
      {
        path: 'orders/:orderNumber',
        title: 'Order status · Crust & Crunch',
        loadComponent: () => import('./features/orders/order-tracking.component').then((m) => m.OrderTrackingComponent),
      },
      {
        path: 'login',
        title: 'Sign in · Crust & Crunch',
        canActivate: [guestOnlyGuard],
        loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'register',
        title: 'Create account · Crust & Crunch',
        canActivate: [guestOnlyGuard],
        loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'account',
        canActivate: [authGuard],
        loadChildren: () => import('./features/account/account.routes').then((m) => m.ACCOUNT_ROUTES),
      },
      {
        path: '**',
        title: 'Not found · Crust & Crunch',
        loadComponent: () => import('./features/not-found.component').then((m) => m.NotFoundComponent),
      },
    ],
  },
];
