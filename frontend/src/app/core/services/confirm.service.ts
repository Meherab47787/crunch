import { Injectable, signal } from '@angular/core';

interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
  resolve: (ok: boolean) => void;
}

/** Promise-based confirmation dialog: `if (await confirm.ask({...}))`. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly current = signal<ConfirmRequest | null>(null);

  ask(opts: { title: string; message: string; confirmLabel?: string; danger?: boolean }): Promise<boolean> {
    return new Promise((resolve) =>
      this.current.set({ confirmLabel: 'Confirm', danger: false, ...opts, resolve }),
    );
  }

  settle(ok: boolean) {
    this.current()?.resolve(ok);
    this.current.set(null);
  }
}
