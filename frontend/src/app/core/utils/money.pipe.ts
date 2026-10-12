import { Pipe, PipeTransform, inject } from '@angular/core';
import { SettingsService } from '../services/settings.service';

/** Formats money in the restaurant's configured currency. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly settings = inject(SettingsService);

  transform(value: number | null | undefined, signed = false): string {
    if (value == null) return '';
    const fmt = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: this.settings.currency(),
      minimumFractionDigits: 2,
    });
    const s = fmt.format(Math.abs(value));
    if (signed && value > 0) return `+${s}`;
    if (value < 0) return `−${s}`;
    return s;
  }
}
