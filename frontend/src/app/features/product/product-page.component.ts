import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { LucideAngularModule } from 'lucide-angular';
import { map } from 'rxjs';
import { ProductDetail, Review } from '../../core/models/api.models';
import { CartService } from '../../core/services/cart.service';
import { MenuService } from '../../core/services/menu.service';
import { ToastService } from '../../core/services/toast.service';
import { MoneyPipe } from '../../core/utils/money.pipe';
import { remote } from '../../core/utils/remote';
import { DietBadgesComponent } from '../../shared/components/diet-badges.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { QuantityStepperComponent } from '../../shared/components/quantity-stepper.component';
import { RatingStarsComponent } from '../../shared/components/rating-stars.component';
import { ProductCustomizer } from './product-customizer';
import { OptionGroupComponent } from './option-group.component';
import { ComboSlotComponent } from './combo-slot.component';

@Component({
  selector: 'app-product-page',
  imports: [FormsModule, RouterLink, DatePipe, LucideAngularModule, MoneyPipe, DietBadgesComponent, RatingStarsComponent,
    QuantityStepperComponent, EmptyStateComponent, OptionGroupComponent, ComboSlotComponent],
  providers: [ProductCustomizer],
  templateUrl: './product-page.component.html',
})
export class ProductPageComponent {
  private readonly menu = inject(MenuService);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  readonly c = inject(ProductCustomizer);

  readonly slug = input.required<string>();
  /** Cart line key when editing an existing bag item (?edit=...). */
  readonly edit = input<string | undefined>();

  readonly detail = remote<ProductDetail>(() => this.menu.product(this.slug()), { lazy: true });
  readonly reviews = remote<Review[]>(() => this.menu.reviews(this.slug(), 0, 6).pipe(map((page) => page.content)), { lazy: true });
  readonly activeImage = signal<string | null>(null);
  readonly triedSubmit = signal(false);
  readonly editKey = computed(() => {
    const key = this.edit();
    const d = this.detail.data();
    const line = key ? this.cart.find(key) : undefined;
    return line && d && line.productId === d.summary.id ? key! : null;
  });

  constructor() {
    effect(() => {
      this.slug();
      untracked(() => {
        this.activeImage.set(null);
        this.triedSubmit.set(false);
        this.detail.reload();
        this.reviews.reload();
      });
    });
    effect(() => {
      const d = this.detail.data();
      if (!d) return;
      untracked(() => {
        this.title.setTitle(`${d.summary.name} · Crust & Crunch`);
        const key = this.editKey();
        this.c.init(d, key ? this.cart.find(key) : undefined);
      });
    });
  }

  submit(): void {
    this.triedSubmit.set(true);
    const d = this.detail.data();
    if (!d || !d.summary.available || this.c.problems().length) return;
    const line = this.c.toCartLine();
    const key = this.editKey();
    if (key) {
      this.cart.replace(key, line);
      this.toast.success(`Updated ${d.summary.name}`);
      this.router.navigateByUrl('/cart');
    } else {
      this.cart.add(line);
      this.toast.success(`${line.quantity}× ${d.summary.name} added to your bag`, {
        label: 'View bag',
        run: () => this.cart.drawerOpen.set(true),
      });
      this.triedSubmit.set(false);
    }
  }
}
