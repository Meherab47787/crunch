import { Component, ElementRef, OnDestroy, afterNextRender, computed, inject, input, signal } from '@angular/core';

export interface ColumnPoint {
  /** Short axis label, e.g. "Sep 28". */
  label: string;
  /** Long label for tooltip / table, e.g. "Mon, Sep 28". */
  fullLabel: string;
  value: number;
  /** Secondary metric shown in tooltip + table (e.g. "12 orders"). */
  secondary?: string;
}

const PLOT_H = 200;
const AXIS_H = 28;
const PAD_TOP = 22;
const Y_AXIS_W = 52;

/**
 * Single-series column chart, hand-rolled SVG.
 * Dataviz spec: <=24px columns, 4px rounded data-end, square at baseline, hairline solid grid,
 * per-column hover/focus tooltip (hit target = full band), selective direct labels (max + latest),
 * and a table-view twin so the tooltip never gates a value.
 */
@Component({
  selector: 'app-column-chart',
  host: { class: 'block w-full' },
  templateUrl: './column-chart.component.html',
})
export class ColumnChartComponent implements OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>);
  readonly points = input.required<ColumnPoint[]>();
  readonly format = input.required<(v: number) => string>();
  readonly formatCompact = input.required<(v: number) => string>();
  readonly ariaLabel = input('Column chart');
  readonly valueName = input('Value');
  readonly secondaryName = input<string>();
  readonly tableView = input(false);
  /** Hold the previous render at reduced opacity while refetching. */
  readonly dim = input(false);

  readonly plotH = PLOT_H;
  readonly padTop = PAD_TOP;
  readonly yAxisW = Y_AXIS_W;
  readonly totalH = PAD_TOP + PLOT_H + AXIS_H;
  readonly baseY = PAD_TOP + PLOT_H;

  readonly width = signal(600);
  readonly active = signal<number | null>(null);
  private ro?: ResizeObserver;

  constructor() {
    afterNextRender(() => {
      const el = this.host.nativeElement as HTMLElement;
      this.width.set(Math.max(240, el.clientWidth));
      this.ro = new ResizeObserver((entries) => {
        const w = entries[0]?.contentRect.width;
        if (w) this.width.set(Math.max(240, Math.floor(w)));
      });
      this.ro.observe(el);
    });
  }

  ngOnDestroy(): void {
    this.ro?.disconnect();
  }

  /** Clean tick step (1/2/2.5/5 × 10^n) and the axis top as a whole number of steps. */
  private readonly scale = computed(() => {
    const max = Math.max(0, ...this.points().map((p) => p.value));
    if (max <= 0) return { step: 1, top: 4 };
    const step = niceStep(max / 4);
    return { step, top: Math.ceil(max / step) * step };
  });
  private readonly maxNice = computed(() => this.scale().top);

  readonly ticks = computed(() => {
    const { step, top } = this.scale();
    const out: { value: number; y: number; label: string }[] = [];
    for (let v = 0; v <= top + step / 2; v += step) {
      out.push({ value: v, y: this.baseY - (v / top) * PLOT_H, label: this.formatCompact()(v) });
    }
    return out;
  });

  readonly bars = computed(() => {
    const pts = this.points();
    const n = Math.max(1, pts.length);
    const plotW = this.width() - Y_AXIS_W;
    const band = plotW / n;
    const barW = Math.min(24, band * 0.62);
    const top = this.maxNice();
    const every = Math.max(1, Math.ceil((n * 56) / plotW));
    const maxVal = Math.max(...pts.map((p) => p.value));
    const maxIdx = pts.findIndex((p) => p.value === maxVal);
    return pts.map((p, i) => {
      const cx = Y_AXIS_W + band * i + band / 2;
      const h = (p.value / top) * PLOT_H;
      const x = cx - barW / 2;
      const y = this.baseY - h;
      const r = Math.min(4, h, barW / 2);
      const path = h <= 0 ? '' :
        `M${x},${this.baseY} V${y + r} Q${x},${y} ${x + r},${y} H${x + barW - r} Q${x + barW},${y} ${x + barW},${y + r} V${this.baseY} Z`;
      return {
        i, cx, path, top: y, value: p.value, label: p.label, fullLabel: p.fullLabel, secondary: p.secondary,
        bandX: Y_AXIS_W + band * i, bandW: band,
        short: this.formatCompact()(p.value), formatted: this.format()(p.value),
        labelled: i === maxIdx || i === n - 1,
        showX: (n - 1 - i) % every === 0,
      };
    });
  });

  readonly tooltip = computed(() => {
    const i = this.active();
    if (i == null) return null;
    const b = this.bars()[i];
    if (!b) return null;
    const x = Math.min(Math.max(b.cx, 70), this.width() - 70);
    return { x, y: Math.min(b.top, this.baseY - 4), formatted: b.formatted, fullLabel: b.fullLabel, secondary: b.secondary };
  });

  readonly summary = computed(() => {
    const pts = this.points();
    if (!pts.length) return `${this.ariaLabel()}: no data`;
    const total = pts.reduce((s, p) => s + p.value, 0);
    const max = pts.reduce((a, b) => (b.value > a.value ? b : a));
    return `${this.ariaLabel()}. ${pts.length} periods from ${pts[0].fullLabel} to ${pts[pts.length - 1].fullLabel}. ` +
      `Total ${this.format()(total)}. Highest ${this.format()(max.value)} on ${max.fullLabel}.`;
  });
}

function niceStep(raw: number): number {
  const mag = Math.pow(10, Math.floor(Math.log10(raw)));
  const n = raw / mag;
  const nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
  return nice * mag;
}
