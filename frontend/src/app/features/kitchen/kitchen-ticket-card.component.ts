import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Order, OrderItem, OrderStatus } from '../../core/models/api.models';

const ADVANCE_LABEL: Partial<Record<OrderStatus, string>> = {
  CONFIRMED: 'Accept order',
  PREPARING: 'Start preparing',
  READY_FOR_PICKUP: 'Mark ready',
  OUT_FOR_DELIVERY: 'Hand to driver',
  DELIVERED: 'Collected',
};

const COLUMN_NAME: Partial<Record<OrderStatus, string>> = {
  PENDING: 'New',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Preparing',
  READY_FOR_PICKUP: 'Ready',
};

/** One order ticket on the kitchen board. Purely presentational. */
@Component({
  selector: 'app-kitchen-ticket-card',
  imports: [DatePipe, LucideAngularModule],
  templateUrl: './kitchen-ticket-card.component.html',
})
export class KitchenTicketCardComponent {
  readonly order = input.required<Order>();
  /** Current time in ms (shared ticking clock). */
  readonly now = input.required<number>();
  readonly pending = input(false);
  readonly isNew = input(false);
  readonly advance = output<Order>();
  readonly back = output<Order>();
  readonly print = output<Order>();

  readonly shortNumber = computed(() => {
    const n = this.order().orderNumber;
    const parts = n.split('-');
    return parts.length > 1 ? parts[parts.length - 1] : n;
  });

  private readonly seconds = computed(() => Math.max(0, Math.floor((this.now() - new Date(this.order().createdAt).getTime()) / 1000)));
  readonly elapsed = computed(() => {
    const s = this.seconds();
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    const mm = String(m).padStart(h ? 2 : 1, '0');
    return h ? `${h}:${mm}:${String(sec).padStart(2, '0')}` : `${mm}:${String(sec).padStart(2, '0')}`;
  });
  readonly elapsedLabel = computed(() => {
    const m = Math.floor(this.seconds() / 60);
    return m < 1 ? 'under a minute' : `${m} minute${m === 1 ? '' : 's'}`;
  });
  readonly level = computed<'ok' | 'warn' | 'late'>(() => {
    const m = this.seconds() / 60;
    return m > 20 ? 'late' : m > 10 ? 'warn' : 'ok';
  });
  readonly frameClass = computed(() => {
    const base = 'flex flex-col overflow-hidden rounded-2xl border-2 bg-charcoal-800 shadow-lg transition';
    const ring = this.isNew() ? ' ring-4 ring-ember-400/70' : '';
    switch (this.level()) {
      case 'late': return `${base} border-tomato-500${ring}`;
      case 'warn': return `${base} border-ember-400${ring}`;
      default: return `${base} border-white/10${ring}`;
    }
  });
  readonly timerTextClass = computed(() => {
    const base = 'flex items-center justify-end gap-1 font-display text-2xl font-extrabold tabular-nums';
    switch (this.level()) {
      case 'late': return `${base} text-tomato-400`;
      case 'warn': return `${base} text-ember-300`;
      default: return `${base} text-cream-100`;
    }
  });
  backLabel(status: OrderStatus): string {
    return COLUMN_NAME[status] ?? status;
  }

  readonly advanceLabel = computed(() => {
    const n = this.order().nextStatus;
    if (!n) return '';
    if (n === 'DELIVERED' && this.order().fulfillmentType === 'PICKUP') return 'Picked up';
    return ADVANCE_LABEL[n] ?? 'Next';
  });
  readonly advanceClass = computed(() => {
    const base = 'flex h-14 flex-1 items-center justify-center gap-2 rounded-xl text-lg font-extrabold transition active:scale-[0.98] disabled:cursor-wait disabled:opacity-60';
    switch (this.order().status) {
      case 'PENDING': return `${base} bg-ember-400 text-charcoal-900 hover:bg-ember-300`;
      case 'CONFIRMED': return `${base} bg-sky-400 text-sky-950 hover:bg-sky-300`;
      case 'PREPARING': return `${base} bg-emerald-400 text-emerald-950 hover:bg-emerald-300`;
      default: return `${base} bg-cream-100 text-charcoal-900 hover:bg-white`;
    }
  });

  readonly items = computed(() =>
    this.order().items.map((item: OrderItem) => ({
      item,
      combo: item.customizations.filter((c) => c.kind === 'COMBO_CHOICE'),
      options: item.customizations.filter((c) => c.kind === 'OPTION'),
    })),
  );
}
