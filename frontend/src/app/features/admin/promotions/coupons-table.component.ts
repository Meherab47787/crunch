import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Coupon } from '../../../core/models/api.models';
import { MoneyPipe } from '../../../core/utils/money.pipe';

@Component({
  selector: 'app-coupons-table',
  imports: [DatePipe, LucideAngularModule, MoneyPipe],
  templateUrl: './coupons-table.component.html',
})
export class CouponsTableComponent {
  readonly coupons = input.required<Coupon[]>();
  readonly edit = output<Coupon>();
  readonly remove = output<Coupon>();

  state(c: Coupon): 'live' | 'scheduled' | 'expired' | 'used' | 'inactive' {
    if (!c.active) return 'inactive';
    const now = Date.now();
    if (c.expiresAt && new Date(c.expiresAt).getTime() < now) return 'expired';
    if (c.maxRedemptions && c.redemptionCount >= c.maxRedemptions) return 'used';
    if (c.startsAt && new Date(c.startsAt).getTime() > now) return 'scheduled';
    return 'live';
  }
}
