import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckoutRequest, Order, OrderSummary, Page, ReorderResponse, Review } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  /** The idempotency key must be reused for retries of the same checkout attempt. */
  checkout(req: CheckoutRequest, idempotencyKey: string): Observable<Order> {
    return this.http.post<Order>('/api/orders', req, { headers: { 'Idempotency-Key': idempotencyKey } });
  }

  myOrders(page = 0, size = 10): Observable<Page<OrderSummary>> {
    return this.http.get<Page<OrderSummary>>('/api/orders/my', { params: { page, size } });
  }

  get(id: number): Observable<Order> {
    return this.http.get<Order>(`/api/orders/${id}`);
  }

  track(orderNumber: string, token: string): Observable<Order> {
    return this.http.get<Order>(`/api/orders/track/${encodeURIComponent(orderNumber)}`, { params: { token } });
  }

  cancel(id: number, reason: string | null): Observable<Order> {
    return this.http.post<Order>(`/api/orders/${id}/cancel`, { reason });
  }

  reorder(id: number): Observable<ReorderResponse> {
    return this.http.post<ReorderResponse>(`/api/orders/${id}/reorder`, {});
  }

  review(body: { orderId: number; productId: number; rating: number; comment: string | null }): Observable<Review> {
    return this.http.post<Review>('/api/reviews', body);
  }
}
