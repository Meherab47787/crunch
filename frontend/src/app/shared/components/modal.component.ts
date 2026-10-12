import { Component, ElementRef, HostListener, afterNextRender, inject, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

/** Accessible dialog shell: focus moves in on open, Esc / backdrop closes. */
@Component({
  selector: 'app-modal',
  imports: [LucideAngularModule],
  templateUrl: './modal.component.html',
})
export class ModalComponent {
  private readonly el = inject(ElementRef<HTMLElement>);
  readonly title = input.required<string>();
  readonly size = input<'md' | 'lg' | 'xl'>('md');
  readonly closed = output<void>();

  constructor() {
    afterNextRender(() => {
      const panel = this.el.nativeElement.querySelector('[role=dialog]') as HTMLElement | null;
      const first = panel?.querySelector<HTMLElement>('input, select, textarea, button:not([aria-label=Close])');
      (first ?? panel)?.focus();
    });
  }

  @HostListener('document:keydown.escape')
  onEsc() {
    this.closed.emit();
  }

  onBackdrop(e: MouseEvent) {
    if (e.target === e.currentTarget) this.closed.emit();
  }
}
