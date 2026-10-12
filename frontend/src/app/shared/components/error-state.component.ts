import { Component, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-error-state',
  imports: [LucideAngularModule],
  templateUrl: './error-state.component.html',
})
export class ErrorStateComponent {
  readonly message = input('We couldn’t load this right now.');
  readonly retry = output<void>();
}
