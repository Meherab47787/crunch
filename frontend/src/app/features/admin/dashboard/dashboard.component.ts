import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { OrderStatus, SalesPoint } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { SettingsService } from '../../../core/services/settings.service';
import { MoneyPipe, formatMoney } from '../../../core/utils/money.pipe';
import { ALL_STATUSES, STATUS_SHORT } from '../../../core/utils/order-status';
import { remote } from '../../../core/utils/remote';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { BarListComponent, BarRow } from './bar-list.component';
import { ColumnChartComponent, ColumnPoint } from './column-chart.component';

type Granularity = 'daily' | 'weekly' | 'monthly';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, RouterLink, LucideAngularModule, MoneyPipe, StatusBadgeComponent, ErrorStateComponent, EmptyStateComponent,
    AdminPageHeaderComponent, ColumnChartComponent, BarListComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly admin = inject(AdminService);
  private readonly settings = inject(SettingsService);
  readonly stats = remote(() => this.admin.dashboard());
  readonly today = new Date();

  readonly granularity = signal<Granularity>('daily');
  readonly tableView = signal(false);
  readonly granularities: { key: Granularity; label: string }[] = [
    { key: 'daily', label: 'Daily' },
    { key: 'weekly', label: 'Weekly' },
    { key: 'monthly', label: 'Monthly' },
  ];

  readonly fmt = (v: number) => formatMoney(v, this.settings.currency());
  readonly fmtCompact = (v: number) =>
    formatMoney(v, this.settings.currency(), { notation: 'compact', minimumFractionDigits: 0, maximumFractionDigits: v >= 1000 ? 1 : 0 });

  readonly granularityText = computed(() => {
    const s = this.stats.data();
    const g = this.granularity();
    const n = g === 'daily' ? s?.daily.length : g === 'weekly' ? s?.weekly.length : s?.monthly.length;
    return g === 'daily' ? `Last ${n ?? 14} days` : g === 'weekly' ? `Last ${n ?? 12} weeks` : `Last ${n ?? 12} months`;
  });

  readonly series = computed<ColumnPoint[]>(() => {
    const s = this.stats.data();
    if (!s) return [];
    const g = this.granularity();
    const pts: SalesPoint[] = g === 'daily' ? s.daily : g === 'weekly' ? s.weekly : s.monthly;
    return pts.map((p) => {
      const d = parseDay(p.label);
      const orders = `${p.orders} ${p.orders === 1 ? 'order' : 'orders'}`;
      if (g === 'daily') {
        return { label: fmtDate(d, { month: 'short', day: 'numeric' }), fullLabel: fmtDate(d, { weekday: 'short', month: 'short', day: 'numeric' }), value: p.revenue, secondary: orders };
      }
      if (g === 'weekly') {
        return { label: fmtDate(d, { month: 'short', day: 'numeric' }), fullLabel: 'Week of ' + fmtDate(d, { month: 'short', day: 'numeric', year: 'numeric' }), value: p.revenue, secondary: orders };
      }
      return { label: fmtDate(d, { month: 'short' }), fullLabel: fmtDate(d, { month: 'long', year: 'numeric' }), value: p.revenue, secondary: orders };
    });
  });

  readonly seriesTotal = computed(() => this.series().reduce((a, p) => a + p.value, 0));

  readonly statusRows = computed<BarRow[]>(() => {
    const counts = this.stats.data()?.statusCounts;
    if (!counts) return [];
    return ALL_STATUSES.map((st: OrderStatus) => ({
      key: st,
      label: STATUS_SHORT[st],
      value: counts[st] ?? 0,
      display: String(counts[st] ?? 0),
    }));
  });

  readonly topRows = computed<BarRow[]>(() =>
    [...(this.stats.data()?.topProducts ?? [])].sort((a, b) => b.revenue - a.revenue).map((p, i) => ({
      key: `${p.productId ?? 'x'}-${i}`,
      label: p.name,
      value: p.revenue,
      display: this.fmt(p.revenue),
      secondary: `${p.quantity} sold`,
    })),
  );

  readonly categoryRows = computed<BarRow[]>(() =>
    [...(this.stats.data()?.categorySales ?? [])].sort((a, b) => b.revenue - a.revenue).map((c) => ({
      key: c.name,
      label: c.name,
      value: c.revenue,
      display: this.fmt(c.revenue),
      secondary: `${c.quantity} items`,
    })),
  );
}

function parseDay(label: string): Date {
  const [y, m, d] = label.split('-').map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

function fmtDate(d: Date, opts: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(undefined, opts).format(d);
}
