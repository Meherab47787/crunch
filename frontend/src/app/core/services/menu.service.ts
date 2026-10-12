import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { Category, Page, ProductDetail, ProductQuery, ProductSummary, Promotion, Review } from '../models/api.models';

export function toParams(query: object): HttpParams {
  let params = new HttpParams();
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
  }
  return params;
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly http = inject(HttpClient);
  private categories$?: Observable<Category[]>;

  categories(): Observable<Category[]> {
    this.categories$ ??= this.http.get<Category[]>('/api/categories').pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.categories$;
  }

  products(query: ProductQuery): Observable<Page<ProductSummary>> {
    return this.http.get<Page<ProductSummary>>('/api/products', { params: toParams(query) });
  }

  product(slug: string): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`/api/products/${encodeURIComponent(slug)}`);
  }

  reviews(slug: string, page = 0, size = 5): Observable<Page<Review>> {
    return this.http.get<Page<Review>>(`/api/products/${encodeURIComponent(slug)}/reviews`, { params: { page, size } });
  }

  highlightedReviews(limit = 6): Observable<Review[]> {
    return this.http.get<Review[]>('/api/reviews/highlights', { params: { limit } });
  }

  promotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>('/api/promotions');
  }
}
