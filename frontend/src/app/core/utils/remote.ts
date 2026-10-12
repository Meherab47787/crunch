import { DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subscription } from 'rxjs';
import { errorMessage } from './errors';

export interface Remote<T> {
  data: () => T | null;
  loading: () => boolean;
  error: () => string | null;
  reload: () => void;
  set: (value: T) => void;
}

/**
 * Loading / error / data state for a request, tied to the calling component's lifetime.
 * Must be created in an injection context (e.g. a field initializer).
 */
export function remote<T>(factory: () => Observable<T>, opts: { lazy?: boolean } = {}): Remote<T> {
  const destroyRef = inject(DestroyRef);
  const state = signal<{ data: T | null; loading: boolean; error: string | null }>({
    data: null,
    loading: !opts.lazy,
    error: null,
  });
  let sub: Subscription | undefined;

  const reload = () => {
    sub?.unsubscribe();
    state.update((s) => ({ ...s, loading: true, error: null }));
    sub = factory()
      .pipe(takeUntilDestroyed(destroyRef))
      .subscribe({
        next: (data) => state.set({ data, loading: false, error: null }),
        error: (err) => state.set({ data: null, loading: false, error: errorMessage(err) }),
      });
  };

  if (!opts.lazy) reload();

  return {
    data: computed(() => state().data),
    loading: computed(() => state().loading),
    error: computed(() => state().error),
    reload,
    set: (data: T) => state.set({ data, loading: false, error: null }),
  };
}
