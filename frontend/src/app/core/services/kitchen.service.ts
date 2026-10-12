import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Order, OrderStatus } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class KitchenService {
  private readonly http = inject(HttpClient);

  queue(): Observable<Order[]> {
    return this.http.get<Order[]>('/api/kitchen/orders');
  }

  updateStatus(id: number, status: OrderStatus, note: string | null = null): Observable<Order> {
    return this.http.patch<Order>(`/api/kitchen/orders/${id}/status`, { status, note });
  }
}
