import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { map } from 'rxjs';
import { MenuService } from '../../core/services/menu.service';
import { SettingsService } from '../../core/services/settings.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { remote } from '../../core/utils/remote';
import { ErrorStateComponent } from '../../shared/components/error-state.component';
import { ProductCardSkeletonComponent } from '../../shared/components/product-card-skeleton.component';
import { ProductCardComponent } from '../../shared/components/product-card.component';
import { RatingStarsComponent } from '../../shared/components/rating-stars.component';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe, LucideAngularModule, MoneyPipe, ProductCardComponent, ProductCardSkeletonComponent,
    ErrorStateComponent, RatingStarsComponent],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private readonly menu = inject(MenuService);
  readonly settings = inject(SettingsService);

  readonly categories = remote(() => this.menu.categories());
  readonly promotions = remote(() => this.menu.promotions());
  readonly bestsellers = remote(() =>
    this.menu.products({ bestseller: true, type: 'STANDARD', sort: 'popular', size: 8 }).pipe(map((p) => p.content)),
  );
  readonly combos = remote(() => this.menu.products({ type: 'COMBO', sort: 'popular', size: 4 }).pipe(map((p) => p.content)));
  readonly fresh = remote(() => this.menu.products({ newArrival: true, sort: 'newest', size: 4 }).pipe(map((p) => p.content)));
  readonly reviews = remote(() => this.menu.highlightedReviews(6));
  readonly topReviews = computed(() => (this.reviews.data() ?? []).slice(0, 3));

  placeholders(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i);
  }
}
