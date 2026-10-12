import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ProductSummary } from '../../core/models/api.models';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { DietBadgesComponent } from './diet-badges.component';
import { RatingStarsComponent } from './rating-stars.component';

@Component({
  selector: 'app-product-card',
  imports: [RouterLink, LucideAngularModule, MoneyPipe, RatingStarsComponent, DietBadgesComponent],
  templateUrl: './product-card.component.html',
})
export class ProductCardComponent {
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);

  readonly product = input.required<ProductSummary>();
  readonly compact = input(false);
  readonly hasRange = computed(() => this.product().customizable);

  quickAdd(): void {
    const p = this.product();
    this.cart.add({
      productId: p.id,
      variantId: null,
      optionIds: [],
      comboSelections: [],
      quantity: 1,
      notes: null,
      slug: p.slug,
      name: p.name,
      imageUrl: p.imageUrl,
      categoryName: p.categoryName,
      summary: [],
      displayUnitPrice: p.salePrice ?? p.fromPrice,
    });
    this.toast.success(`${p.name} added to your bag`, { label: 'View bag', run: () => this.cart.drawerOpen.set(true) });
  }
}
