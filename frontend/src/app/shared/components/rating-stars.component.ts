import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-rating-stars',
  templateUrl: './rating-stars.component.html',
})
export class RatingStarsComponent {
  readonly rating = input.required<number>();
  readonly count = input<number | null>(null);
  readonly showValue = input(true);
  readonly pct = computed(() => Math.max(0, Math.min(100, (this.rating() / 5) * 100)));
  readonly label = computed(() => `Rated ${this.rating().toFixed(1)} out of 5`);
}
