import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { PublicSettings } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  readonly settings = signal<PublicSettings | null>(null);
  readonly currency = computed(() => this.settings()?.currency ?? 'USD');

  load(): void {
    if (this.settings()) return;
    this.http.get<PublicSettings>('/api/settings').subscribe({
      next: (s) => this.settings.set(s),
      error: () => {
        /* storefront still works with defaults */
      },
    });
  }
}
