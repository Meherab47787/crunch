import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-table-skeleton',
  templateUrl: './table-skeleton.component.html',
})
export class TableSkeletonComponent {
  readonly rows = input(6);
  readonly cols = input(5);
  readonly rowsArr = computed(() => Array.from({ length: this.rows() }, (_, i) => i));
  readonly colsArr = computed(() => Array.from({ length: this.cols() }, (_, i) => i));
}
