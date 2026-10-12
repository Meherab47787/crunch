import { Component, computed, input } from '@angular/core';

export interface BarRow {
  key: string;
  label: string;
  value: number;
  /** Formatted value printed at the bar tip. */
  display: string;
  /** Optional secondary text (e.g. "36 sold"). */
  secondary?: string;
}

/**
 * Horizontal single-series bar list (HTML, not SVG, so long labels wrap cleanly).
 * Every value is printed at the bar tip, so nothing is gated behind hover.
 */
@Component({
  selector: 'app-bar-list',
  host: { class: 'block' },
  templateUrl: './bar-list.component.html',
})
export class BarListComponent {
  readonly data = input.required<BarRow[]>();
  readonly ariaLabel = input('Bar chart');
  readonly rows = computed(() => {
    const max = Math.max(0, ...this.data().map((d) => d.value));
    return this.data().map((d) => ({ ...d, pct: max > 0 ? (d.value / max) * 100 : 0 }));
  });
}
