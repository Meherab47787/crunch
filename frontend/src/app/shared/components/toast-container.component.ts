import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  imports: [LucideAngularModule],
  templateUrl: './toast-container.component.html',
})
export class ToastContainerComponent {
  readonly toasts = inject(ToastService);
}
