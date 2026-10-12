import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
  action?: { label: string; run: () => void };
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  success(message: string, action?: Toast['action']) {
    this.push('success', message, action);
  }

  error(message: string) {
    this.push('error', message, undefined, 6000);
  }

  info(message: string) {
    this.push('info', message);
  }

  dismiss(id: number) {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: ToastKind, message: string, action?: Toast['action'], ttl = 3800) {
    const id = ++this.seq;
    this.toasts.update((list) => [...list.slice(-3), { id, kind, message, action }]);
    setTimeout(() => this.dismiss(id), ttl);
  }
}
