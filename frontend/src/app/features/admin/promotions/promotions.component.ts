import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { map } from 'rxjs';
import { Coupon, Promotion } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import { CouponFormModalComponent } from './coupon-form-modal.component';
import { CouponsTableComponent } from './coupons-table.component';
import { PromotionFormModalComponent } from './promotion-form-modal.component';

type Tab = 'promotions' | 'coupons';

@Component({
  selector: 'app-admin-promotions',
  imports: [DatePipe, LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent, TableSkeletonComponent,
    CouponsTableComponent, CouponFormModalComponent, PromotionFormModalComponent],
  templateUrl: './promotions.component.html',
})
export class PromotionsComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly tabs: { key: Tab; label: string; icon: string }[] = [
    { key: 'promotions', label: 'Promotions', icon: 'percent' },
    { key: 'coupons', label: 'Coupons', icon: 'ticket' },
  ];
  private readonly tabParam = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('tab'))), { initialValue: null });
  readonly tab = computed<Tab>(() => (this.tabParam() === 'coupons' ? 'coupons' : 'promotions'));

  readonly promotions = remote(() => this.admin.promotions());
  readonly coupons = remote(() => this.admin.coupons());
  readonly categories = remote(() => this.admin.categories());
  readonly products = remote(() => this.admin.products({ size: 200 }));
  readonly couponEdit = signal<{ c: Coupon | null } | null>(null);
  readonly promoEdit = signal<{ p: Promotion | null } | null>(null);

  setTab(t: Tab) {
    this.router.navigate([], { relativeTo: this.route, queryParams: { tab: t === 'promotions' ? null : t }, replaceUrl: true });
  }

  onTabKey(e: KeyboardEvent) {
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    const next: Tab = this.tab() === 'promotions' ? 'coupons' : 'promotions';
    this.setTab(next);
    setTimeout(() => document.getElementById('tab-' + next)?.focus());
  }

  async removeCoupon(c: Coupon) {
    const ok = await this.confirm.ask({ title: `Delete coupon ${c.code}?`, message: 'Customers will no longer be able to use this code.', confirmLabel: 'Delete coupon', danger: true });
    if (!ok) return;
    this.admin.deleteCoupon(c.id).subscribe({
      next: () => {
        this.toast.success(`Coupon ${c.code} deleted`);
        this.coupons.reload();
      },
      error: (err) => this.toast.error(errorMessage(err, 'Could not delete the coupon.')),
    });
  }

  async removePromotion(p: Promotion) {
    const ok = await this.confirm.ask({ title: `Delete “${p.name}”?`, message: 'The discount will stop applying immediately.', confirmLabel: 'Delete promotion', danger: true });
    if (!ok) return;
    this.admin.deletePromotion(p.id).subscribe({
      next: () => {
        this.toast.success(`${p.name} deleted`);
        this.promotions.reload();
      },
      error: (err) => this.toast.error(errorMessage(err, 'Could not delete the promotion.')),
    });
  }
}
