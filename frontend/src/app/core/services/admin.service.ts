import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Category,
  CategoryRequest,
  Coupon,
  CouponRequest,
  Customer,
  DashboardStats,
  GroupAdmin,
  GroupRequest,
  Order,
  OrderStatus,
  OrderSummary,
  Page,
  ProductDetail,
  ProductRequest,
  ProductSummary,
  Promotion,
  PromotionRequest,
  Role,
  StaffCreateRequest,
  User,
} from '../models/api.models';
import { toParams } from './menu.service';

export interface AdminOrderQuery {
  q?: string;
  status?: OrderStatus | '';
  fulfillmentType?: string;
  paymentMethod?: string;
  category?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin';

  dashboard(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.base}/dashboard`);
  }

  // orders
  orders(q: AdminOrderQuery): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(`${this.base}/orders`, { params: toParams(q) });
  }
  order(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.base}/orders/${id}`);
  }
  updateOrderStatus(id: number, status: OrderStatus, note: string | null): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/orders/${id}/status`, { status, note });
  }

  // categories
  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }
  createCategory(body: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(`${this.base}/categories`, body);
  }
  updateCategory(id: number, body: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.base}/categories/${id}`, body);
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/categories/${id}`);
  }

  // products
  products(q: { q?: string; category?: string; type?: string; available?: boolean; page?: number; size?: number; sort?: string }): Observable<Page<ProductSummary>> {
    return this.http.get<Page<ProductSummary>>(`${this.base}/products`, { params: toParams(q) });
  }
  product(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.base}/products/${id}`);
  }
  createProduct(body: ProductRequest): Observable<ProductDetail> {
    return this.http.post<ProductDetail>(`${this.base}/products`, body);
  }
  updateProduct(id: number, body: ProductRequest): Observable<ProductDetail> {
    return this.http.put<ProductDetail>(`${this.base}/products/${id}`, body);
  }
  setAvailability(id: number, body: { available?: boolean; visible?: boolean }): Observable<ProductSummary> {
    return this.http.patch<ProductSummary>(`${this.base}/products/${id}/availability`, body);
  }
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }

  // option groups
  groups(): Observable<GroupAdmin[]> {
    return this.http.get<GroupAdmin[]>(`${this.base}/customization-groups`);
  }
  createGroup(body: GroupRequest): Observable<GroupAdmin> {
    return this.http.post<GroupAdmin>(`${this.base}/customization-groups`, body);
  }
  updateGroup(id: number, body: GroupRequest): Observable<GroupAdmin> {
    return this.http.put<GroupAdmin>(`${this.base}/customization-groups/${id}`, body);
  }
  deleteGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/customization-groups/${id}`);
  }

  upload(file: File): Observable<{ url: string }> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}/uploads`, fd);
  }

  // promotions & coupons
  coupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${this.base}/coupons`);
  }
  createCoupon(body: CouponRequest): Observable<Coupon> {
    return this.http.post<Coupon>(`${this.base}/coupons`, body);
  }
  updateCoupon(id: number, body: CouponRequest): Observable<Coupon> {
    return this.http.put<Coupon>(`${this.base}/coupons/${id}`, body);
  }
  deleteCoupon(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/coupons/${id}`);
  }
  promotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.base}/promotions`);
  }
  createPromotion(body: PromotionRequest): Observable<Promotion> {
    return this.http.post<Promotion>(`${this.base}/promotions`, body);
  }
  updatePromotion(id: number, body: PromotionRequest): Observable<Promotion> {
    return this.http.put<Promotion>(`${this.base}/promotions/${id}`, body);
  }
  deletePromotion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/promotions/${id}`);
  }

  // users
  customers(q: string, page = 0, size = 20): Observable<Page<Customer>> {
    return this.http.get<Page<Customer>>(`${this.base}/customers`, { params: toParams({ q, page, size }) });
  }
  customer(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.base}/customers/${id}`);
  }
  customerOrders(id: number, page = 0, size = 20): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>(`${this.base}/customers/${id}/orders`, { params: { page, size } });
  }
  staff(q = ''): Observable<Page<User>> {
    return this.http.get<Page<User>>(`${this.base}/staff`, { params: toParams({ q, size: 100 }) });
  }
  createStaff(body: StaffCreateRequest): Observable<User> {
    return this.http.post<User>(`${this.base}/staff`, body);
  }
  updateUser(id: number, body: { role: Role; enabled: boolean }): Observable<User> {
    return this.http.put<User>(`${this.base}/users/${id}`, body);
  }
}
