import { Injectable, inject } from '@angular/core';
import { LUCIDE_ICONS, LucideIconProviderInterface } from 'lucide-angular';

/** lucide-angular throws on unknown names; use this before rendering a user-typed icon name. */
@Injectable({ providedIn: 'root' })
export class IconCheck {
  private readonly providers = inject(LUCIDE_ICONS) as unknown as LucideIconProviderInterface[] | LucideIconProviderInterface;

  has(name: string | null | undefined): boolean {
    if (!name) return false;
    const pascal = name
      .split(/[-_\s]+/)
      .filter(Boolean)
      .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
      .join('');
    const list = Array.isArray(this.providers) ? this.providers : [this.providers];
    return list.some((p) => p.hasIcon(pascal));
  }
}
