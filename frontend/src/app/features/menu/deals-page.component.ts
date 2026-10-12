import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { map } from 'rxjs';
import { MenuService } from '../../core/services/menu.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { remote } from '../../core/utils/remote';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import { ProductCardSkeletonComponent } from '../../shared/components/product-card-skeleton.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-deals-page',
  imports: [RouterLink, DatePipe, LucideAngularModule, MoneyPipe, ProductCardComponent, ProductCardSkeletonComponent, EmptyStateComponent],
  templateUrl: './deals-page.component.html',
})
export class DealsPageComponent {
  private readonly menu = inject(MenuService);
  readonly promotions = remote(() => this.menu.promotions());
  readonly combos = remote(() => this.menu.products({ type: 'COMBO', size: 8 }).pipe(map((p) => p.content)));
  readonly onSale = remote(() =>
    this.menu.products({ size: 60, sort: 'popular' }).pipe(map((p) => p.content.filter((x) => x.salePrice != null).slice(0, 8))),
  );
}
