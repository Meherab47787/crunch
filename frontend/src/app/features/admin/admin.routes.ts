import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout.component';

const t = (s: string) => `${s} · Admin · Crust & Crunch`;

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: t('Dashboard'),
        loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'orders',
        title: t('Orders'),
        loadComponent: () => import('./orders/orders-list.component').then((m) => m.OrdersListComponent),
      },
      {
        path: 'orders/:id',
        title: t('Order'),
        loadComponent: () => import('./orders/order-detail.component').then((m) => m.OrderDetailComponent),
      },
      {
        path: 'products',
        title: t('Products'),
        loadComponent: () => import('./products/products-list.component').then((m) => m.ProductsListComponent),
      },
      {
        path: 'products/new',
        title: t('New product'),
        loadComponent: () => import('./products/product-editor.component').then((m) => m.ProductEditorComponent),
      },
      {
        path: 'products/:id',
        title: t('Edit product'),
        loadComponent: () => import('./products/product-editor.component').then((m) => m.ProductEditorComponent),
      },
      {
        path: 'categories',
        title: t('Categories'),
        loadComponent: () => import('./categories/categories.component').then((m) => m.CategoriesComponent),
      },
      {
        path: 'option-groups',
        title: t('Option groups'),
        loadComponent: () => import('./option-groups/option-groups.component').then((m) => m.OptionGroupsComponent),
      },
      {
        path: 'promotions',
        title: t('Promotions & coupons'),
        loadComponent: () => import('./promotions/promotions.component').then((m) => m.PromotionsComponent),
      },
      {
        path: 'customers',
        title: t('Customers'),
        loadComponent: () => import('./customers/customers-list.component').then((m) => m.CustomersListComponent),
      },
      {
        path: 'customers/:id',
        title: t('Customer'),
        loadComponent: () => import('./customers/customer-detail.component').then((m) => m.CustomerDetailComponent),
      },
      {
        path: 'staff',
        title: t('Staff'),
        loadComponent: () => import('./staff/staff.component').then((m) => m.StaffComponent),
      },
    ],
  },
];
