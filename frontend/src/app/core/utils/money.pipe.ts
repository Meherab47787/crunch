import { Pipe, PipeTransform, inject } from '@angular/core';
import { SettingsService } from '../services/settings.service';

/** Formats an amount in the given currency: "৳1,250", or "৳52.50" when it isn't whole. */
export function formatMoney(value: number, currency: string, options: Intl.NumberFormatOptions = {}): string {
  const digits = Number.isInteger(value) ? 0 : 2;
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
    // narrowSymbol shows "৳" instead of "BDT"
    currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
    ...options,
  }).format(value);
}

/** Formats money in the restaurant's configured currency. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly settings = inject(SettingsService);

  transform(value: number | null | undefined, signed = false): string {
    if (value == null) return '';
    const s = formatMoney(Math.abs(value), this.settings.currency());
    if (signed && value > 0) return `+${s}`;
    if (value < 0) return `−${s}`;
    return s;
  }
}
