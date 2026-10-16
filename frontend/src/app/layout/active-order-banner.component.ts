import { Component, DestroyRef, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { filter, map } from 'rxjs';
import { RecentOrdersService } from '../core/services/recent-orders.service';
import { STATUS_LABEL } from '../core/utils/order-status';

const POLL_MS = 30_000;

/** Keeps a returning guest's in-progress orders one tap away on every storefront page. */
@Component({
  selector: 'app-active-order-banner',
  imports: [RouterLink, LucideAngularModule],
  template: `
    @for (o of visible(); track o.orderNumber) {
      <a [routerLink]="['/orders', o.orderNumber]" [queryParams]="{ token: recent.tokenFor(o.orderNumber) }"
         class="block border-b border-ember-200 bg-ember-100 text-charcoal-800 transition hover:bg-ember-200 no-print">
        <div class="container-page flex items-center gap-3 py-2.5 text-sm">
          <lucide-icon name="chef-hat" [size]="16" class="shrink-0 text-tomato-600" />
          <span class="min-w-0 flex-1 truncate"><b>Order {{ o.orderNumber }}</b> · {{ label[o.status] }}</span>
          <span class="flex shrink-0 items-center gap-1 font-semibold text-tomato-600">Track <lucide-icon name="chevron-right" [size]="16" /></span>
        </div>
      </a>
    }
  `,
})
export class ActiveOrderBannerComponent {
  readonly recent = inject(RecentOrdersService);
  readonly label = STATUS_LABEL;
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url },
  );
  /** Not shown on the order's own tracking page. */
  readonly visible = computed(() =>
    this.recent.active().filter((o) => !this.url().startsWith(`/orders/${encodeURIComponent(o.orderNumber)}`)),
  );

  constructor() {
    this.recent.refreshActive();
    const timer = setInterval(() => {
      if (this.recent.active().length && document.visibilityState === 'visible') this.recent.refreshActive();
    }, POLL_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}
