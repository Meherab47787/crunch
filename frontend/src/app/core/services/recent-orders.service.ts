import { Injectable, signal } from '@angular/core';
import { safeStorage } from '../utils/storage';

export interface RecentOrderRef {
  orderNumber: string;
  token: string;
  placedAt: string;
}

const KEY = 'cc.recentOrders.v1';

/** Remembers guest orders on this device so they can be tracked without an account. */
@Injectable({ providedIn: 'root' })
export class RecentOrdersService {
  readonly orders = signal<RecentOrderRef[]>(safeStorage.get<RecentOrderRef[]>(KEY) ?? []);

  remember(ref: RecentOrderRef): void {
    const next = [ref, ...this.orders().filter((o) => o.orderNumber !== ref.orderNumber)].slice(0, 10);
    this.orders.set(next);
    safeStorage.set(KEY, next);
  }
}
