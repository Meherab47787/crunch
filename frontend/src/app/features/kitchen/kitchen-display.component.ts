import { DatePipe } from '@angular/common';
import { Component, DestroyRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Subscription } from 'rxjs';
import { Order, OrderStatus } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { KitchenService } from '../../core/services/kitchen.service';
import { ToastService } from '../../core/services/toast.service';
import { errorMessage } from '../../core/utils/errors';
import { STATUS_SHORT } from '../../core/utils/order-status';
import { LogoComponent } from '../../shared/components/logo.component';
import { KitchenTicketCardComponent } from './kitchen-ticket-card.component';
import { KitchenTicketPrintComponent } from './kitchen-ticket-print.component';

const REFRESH_MS = 10_000;
const BOARD_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP'];

interface Column {
  status: OrderStatus;
  title: string;
  accent: string;
}

/** Client mirror of the server's happy-path transition (used for the optimistic update only). */
function nextOf(o: Order, s: OrderStatus): OrderStatus | null {
  switch (s) {
    case 'PENDING': return 'CONFIRMED';
    case 'CONFIRMED': return 'PREPARING';
    case 'PREPARING': return 'READY_FOR_PICKUP';
    case 'READY_FOR_PICKUP': return o.fulfillmentType === 'DELIVERY' ? 'OUT_FOR_DELIVERY' : 'DELIVERED';
    case 'OUT_FOR_DELIVERY': return 'DELIVERED';
    default: return null;
  }
}

/** Client mirror of the server's "undo one step" rule (optimistic update only). */
function previousOf(s: OrderStatus): OrderStatus | null {
  switch (s) {
    case 'CONFIRMED': return 'PENDING';
    case 'PREPARING': return 'CONFIRMED';
    case 'READY_FOR_PICKUP': return 'PREPARING';
    default: return null;
  }
}

@Component({
  selector: 'app-kitchen-display',
  imports: [DatePipe, RouterLink, LucideAngularModule, LogoComponent, KitchenTicketCardComponent, KitchenTicketPrintComponent],
  templateUrl: './kitchen-display.component.html',
})
export class KitchenDisplayComponent {
  readonly auth = inject(AuthService);
  private readonly kitchen = inject(KitchenService);
  private readonly toast = inject(ToastService);
  private readonly injector = inject(Injector);

  readonly columns: Column[] = [
    { status: 'PENDING', title: 'New', accent: 'bg-ember-400' },
    { status: 'CONFIRMED', title: 'Confirmed', accent: 'bg-sky-400' },
    { status: 'PREPARING', title: 'Preparing', accent: 'bg-violet-400' },
    { status: 'READY_FOR_PICKUP', title: 'Ready', accent: 'bg-emerald-400' },
  ];

  readonly now = signal(Date.now());
  readonly orders = signal<Order[] | null>(null);
  readonly error = signal<string | null>(null);
  readonly fetching = signal(false);
  readonly lastUpdated = signal<number | null>(null);
  readonly pending = signal(new Set<number>());
  readonly newIds = signal(new Set<number>());
  readonly newBanner = signal<number | null>(null);
  readonly flash = signal(false);
  readonly announcement = signal('');
  readonly printOrder = signal<Order | null>(null);

  private knownIds: Set<number> | null = null;
  private timer?: ReturnType<typeof setTimeout>;
  private fetchSub?: Subscription;
  private bannerTimer?: ReturnType<typeof setTimeout>;
  private flashTimer?: ReturnType<typeof setTimeout>;

  readonly byStatus = computed(() => {
    const map = Object.fromEntries(BOARD_STATUSES.map((s) => [s, [] as Order[]])) as Record<OrderStatus, Order[]>;
    for (const o of this.orders() ?? []) {
      if (map[o.status]) map[o.status].push(o);
    }
    for (const s of BOARD_STATUSES) map[s].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    return map;
  });

  readonly agoText = computed(() => {
    const lu = this.lastUpdated();
    if (!lu) return '';
    const s = Math.max(0, Math.round((this.now() - lu) / 1000));
    return s < 5 ? 'just now' : s < 60 ? `${s}s ago` : `${Math.floor(s / 60)}m ago`;
  });

  constructor() {
    const clock = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(clock);
      clearTimeout(this.timer);
      clearTimeout(this.bannerTimer);
      clearTimeout(this.flashTimer);
      this.fetchSub?.unsubscribe();
    });
    this.load();
  }

  refresh() {
    this.load();
  }

  /** Fetch the queue, then schedule the next poll. Polls are skipped while a status update is in flight. */
  private load() {
    clearTimeout(this.timer);
    if (this.pending().size > 0) {
      this.schedule(1500);
      return;
    }
    this.fetchSub?.unsubscribe();
    this.fetching.set(true);
    this.fetchSub = this.kitchen.queue().subscribe({
      next: (list) => {
        this.fetching.set(false);
        // an advance may have started while we were fetching: keep the optimistic copies
        if (this.pending().size > 0) {
          const local = new Map((this.orders() ?? []).map((o) => [o.id, o]));
          list = list.map((o) => (this.pending().has(o.id) ? local.get(o.id) ?? o : o)).filter((o) => BOARD_STATUSES.includes(o.status));
        }
        this.detectNew(list);
        this.orders.set(list);
        this.error.set(null);
        this.lastUpdated.set(Date.now());
        this.schedule(REFRESH_MS);
      },
      error: (err) => {
        this.fetching.set(false);
        this.error.set(errorMessage(err, 'Could not load the kitchen queue.'));
        this.schedule(REFRESH_MS);
      },
    });
  }

  private schedule(ms: number) {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.load(), ms);
  }

  private detectNew(list: Order[]) {
    const ids = new Set(list.map((o) => o.id));
    if (this.knownIds) {
      const fresh = list.filter((o) => !this.knownIds!.has(o.id) && o.status === 'PENDING').map((o) => o.id);
      if (fresh.length) {
        this.newIds.set(new Set([...this.newIds(), ...fresh]));
        this.newBanner.set(fresh.length);
        this.flash.set(true);
        this.announcement.set(`${fresh.length} new order${fresh.length === 1 ? '' : 's'} received`);
        clearTimeout(this.flashTimer);
        this.flashTimer = setTimeout(() => this.flash.set(false), 1600);
        clearTimeout(this.bannerTimer);
        this.bannerTimer = setTimeout(() => this.newBanner.set(null), 6000);
        // "new" highlight lasts ~30s
        setTimeout(() => {
          const s = new Set(this.newIds());
          fresh.forEach((id) => s.delete(id));
          this.newIds.set(s);
        }, 30_000);
      }
    }
    this.knownIds = ids;
  }

  advance(o: Order) {
    this.move(o, o.nextStatus);
  }

  /** Undo the last step, e.g. a ticket advanced by mistake. */
  back(o: Order) {
    this.move(o, o.previousStatus);
  }

  private move(o: Order, target: OrderStatus | null) {
    if (!target || this.pending().has(o.id)) return;
    const before = this.orders() ?? [];
    const optimistic: Order = { ...o, status: target, nextStatus: nextOf(o, target), previousStatus: previousOf(target) };
    this.setPending(o.id, true);
    this.replace(o.id, BOARD_STATUSES.includes(target) ? optimistic : null);
    // keep the "new" flag off once someone has touched it
    if (this.newIds().has(o.id)) {
      const s = new Set(this.newIds());
      s.delete(o.id);
      this.newIds.set(s);
    }

    this.kitchen.updateStatus(o.id, target).subscribe({
      next: (updated) => {
        this.setPending(o.id, false);
        this.replace(o.id, BOARD_STATUSES.includes(updated.status) ? updated : null);
        this.toast.success(`#${o.orderNumber} → ${STATUS_SHORT[updated.status]}`);
      },
      error: (err) => {
        this.setPending(o.id, false);
        // roll back: put the original order back (if it is still not on the board)
        const current = this.orders() ?? [];
        const restored = current.some((x) => x.id === o.id) ? current.map((x) => (x.id === o.id ? o : x)) : [...current, o];
        this.orders.set(restored.length ? restored : before);
        this.toast.error(`#${o.orderNumber}: ${errorMessage(err, 'could not update status.')}`);
        this.refresh();
      },
    });
  }

  print(o: Order) {
    if (this.printOrder()?.id === o.id && this.printOrder() === o) {
      window.print();
      return;
    }
    this.printOrder.set(o);
    afterNextRender(() => window.print(), { injector: this.injector });
  }

  private replace(id: number, next: Order | null) {
    const list = this.orders() ?? [];
    this.orders.set(next ? list.map((x) => (x.id === id ? next : x)) : list.filter((x) => x.id !== id));
  }

  private setPending(id: number, on: boolean) {
    const s = new Set(this.pending());
    if (on) s.add(id);
    else s.delete(id);
    this.pending.set(s);
  }
}
