import { Component, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-diet-badges',
  imports: [LucideAngularModule],
  templateUrl: './diet-badges.component.html',
})
export class DietBadgesComponent {
  readonly vegetarian = input(false);
  readonly spicy = input(false);
}
