import { Injectable, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { Order } from '../models/api.models';
import { isActive } from '../utils/order-status';
import { safeStorage } from '../utils/storage';
import { OrderService } from './order.service';

export interface RecentOrderRef {
  orderNumber: string;
  token: string;
  placedAt: string;
}

const KEY = 'cc.recentOrders.v1';
/** Orders older than this are never "in progress" any more, so they aren't polled. */
const ACTIVE_WINDOW_MS = 24 * 60 * 60 * 1000;

/** Remembers guest orders on this device so they can be tracked without an account. */
@Injectable({ providedIn: 'root' })
export class RecentOrdersService {
  private readonly api = inject(OrderService);

  readonly orders = signal<RecentOrderRef[]>(safeStorage.get<RecentOrderRef[]>(KEY) ?? []);
  /** Remembered orders that are still being prepared or delivered, with their latest status. */
  readonly active = signal<Order[]>([]);

  remember(ref: RecentOrderRef): void {
    const next = [ref, ...this.orders().filter((o) => o.orderNumber !== ref.orderNumber)].slice(0, 10);
    this.orders.set(next);
    safeStorage.set(KEY, next);
    this.refreshActive();
  }

  tokenFor(orderNumber: string): string | null {
    return this.orders().find((o) => o.orderNumber === orderNumber)?.token ?? null;
  }

  refreshActive(): void {
    const cutoff = Date.now() - ACTIVE_WINDOW_MS;
    const candidates = this.orders().filter((o) => Date.parse(o.placedAt) > cutoff);
    if (!candidates.length) {
      this.active.set([]);
      return;
    }
    forkJoin(candidates.map((r) => this.api.track(r.orderNumber, r.token).pipe(catchError(() => of(null))))).subscribe(
      (list) => this.active.set(list.filter((o): o is Order => !!o && isActive(o.status))),
    );
  }
}
