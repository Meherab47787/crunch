import { Routes } from '@angular/router';
import { AccountLayoutComponent } from './account-layout.component';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    component: AccountLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'orders' },
      { path: 'orders', title: 'My orders · Crust & Crunch', loadComponent: () => import('./my-orders.component').then((m) => m.MyOrdersComponent) },
      { path: 'orders/:id', title: 'Order details · Crust & Crunch', loadComponent: () => import('./my-order-detail.component').then((m) => m.MyOrderDetailComponent) },
      { path: 'profile', title: 'Profile · Crust & Crunch', loadComponent: () => import('./profile.component').then((m) => m.ProfileComponent) },
      { path: 'addresses', title: 'Addresses · Crust & Crunch', loadComponent: () => import('./addresses.component').then((m) => m.AddressesComponent) },
    ],
  },
];
