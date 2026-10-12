import { Component, ElementRef, afterNextRender, computed, effect, inject, input, signal, untracked, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ProductQuery, ProductSummary } from '../../core/models/api.models';
import { MenuService } from '../../core/services/menu.service';
import { errorMessage } from '../../core/utils/errors';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../shared/components/error-state.component';
import { ProductCardSkeletonComponent } from '../../shared/components/product-card-skeleton.component';
import { ProductCardComponent } from '../../shared/components/product-card.component';

const PAGE_SIZE = 12;
const ALLERGENS = ['Gluten', 'Dairy', 'Egg', 'Nuts', 'Soy', 'Sesame'];
const SORTS = [
  { value: '', label: 'Recommended' },
  { value: 'popular', label: 'Most popular' },
  { value: 'rating', label: 'Top rated' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'newest', label: 'Newest' },
];

interface Filters {
  q: string;
  sort: string;
  vegetarian: boolean;
  spicy: boolean | null;
  available: boolean;
  allergenFree: string[];
  maxPrice: number | null;
}

@Component({
  selector: 'app-menu-page',
  imports: [FormsModule, RouterLink, LucideAngularModule, ProductCardComponent, ProductCardSkeletonComponent,
    EmptyStateComponent, ErrorStateComponent],
  templateUrl: './menu-page.component.html',
})
export class MenuPageComponent {
  private readonly menu = inject(MenuService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('search');

  /** Bound from the :category path segment. */
  readonly category = input<string | undefined>();

  readonly sorts = SORTS;
  readonly allergens = ALLERGENS;
  readonly skeletons = [0, 1, 2, 3, 4, 5];
  readonly categories = toSignal(this.menu.categories());
  readonly activeCategory = computed(() => this.categories()?.find((c) => c.slug === this.category()) ?? null);

  private readonly queryParams = toSignal(this.route.queryParamMap, { requireSync: true });
  readonly filters = computed<Filters>(() => {
    const p = this.queryParams();
    const max = Number(p.get('maxPrice'));
    return {
      q: p.get('q') ?? '',
      sort: p.get('sort') ?? '',
      vegetarian: p.get('vegetarian') === 'true',
      spicy: p.get('spicy') === 'true' ? true : p.get('spicy') === 'false' ? false : null,
      available: p.get('available') === 'true',
      allergenFree: (p.get('allergenFree') ?? '').split(',').filter(Boolean),
      maxPrice: max > 0 ? max : null,
    };
  });
  readonly activeFilterCount = computed(() => {
    const f = this.filters();
    return [f.vegetarian, f.spicy !== null, f.available, f.maxPrice !== null].filter(Boolean).length + f.allergenFree.length;
  });

  readonly showFilters = signal(false);
  readonly items = signal<ProductSummary[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly hasMore = signal(false);
  private page = 0;
  private requestSeq = 0;
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    // Reload from page 0 whenever the category or any filter changes.
    effect(() => {
      this.category();
      this.filters();
      untracked(() => this.load(true));
    });
    afterNextRender(() => {
      if (this.route.snapshot.queryParamMap.get('focus') === 'search') this.searchInput()?.nativeElement.focus();
    });
  }

  load(reset: boolean): void {
    if (reset) {
      this.page = 0;
      this.items.set([]);
    }
    const seq = ++this.requestSeq;
    const f = this.filters();
    const query: ProductQuery = {
      q: f.q || undefined,
      category: this.category(),
      sort: f.sort || undefined,
      vegetarian: f.vegetarian || undefined,
      spicy: f.spicy ?? undefined,
      available: f.available || undefined,
      allergenFree: f.allergenFree.join(',') || undefined,
      maxPrice: f.maxPrice ?? undefined,
      page: this.page,
      size: PAGE_SIZE,
    };
    this.loading.set(true);
    this.error.set(null);
    this.menu.products(query).subscribe({
      next: (res) => {
        if (seq !== this.requestSeq) return; // a newer request superseded this one
        this.items.update((list) => (reset ? res.content : [...list, ...res.content]));
        this.total.set(res.totalElements);
        this.hasMore.set(!res.last);
        this.loading.set(false);
      },
      error: (err) => {
        if (seq !== this.requestSeq) return;
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    this.page++;
    this.load(false);
  }

  onSearch(q: string): void {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.patch({ q }), 300);
  }

  toggleAllergen(a: string): void {
    const cur = this.filters().allergenFree;
    this.patch({ allergenFree: cur.includes(a) ? cur.filter((x) => x !== a) : [...cur, a] });
  }

  resetFilters(includeSearch = false): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: includeSearch ? {} : { q: this.filters().q || null, sort: this.filters().sort || null },
    });
  }

  patch(change: Partial<Filters>): void {
    const next = { ...this.filters(), ...change };
    this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        q: next.q || null,
        sort: next.sort || null,
        vegetarian: next.vegetarian || null,
        spicy: next.spicy ?? null,
        available: next.available || null,
        allergenFree: next.allergenFree.join(',') || null,
        maxPrice: next.maxPrice,
        focus: null,
      },
    });
  }
}
