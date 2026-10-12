import { Component, computed, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

/** Zero-based pager: "Showing 21–40 of 207" + prev/next. */
@Component({
  selector: 'app-pagination',
  imports: [LucideAngularModule],
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly total = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();
  readonly from = computed(() => this.page() * this.size() + 1);
  readonly to = computed(() => Math.min(this.total(), (this.page() + 1) * this.size()));
}
