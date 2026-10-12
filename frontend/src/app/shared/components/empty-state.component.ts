import { Component, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-empty-state',
  imports: [LucideAngularModule],
  templateUrl: './empty-state.component.html',
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly text = input<string>();
}
