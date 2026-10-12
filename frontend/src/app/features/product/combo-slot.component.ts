import { Component, computed, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ComboComponent } from '../../core/models/api.models';
import { MoneyPipe } from '../../core/utils/money.pipe';

/** One combo component ("Choose a side"), with one picker per required slot. */
@Component({
  selector: 'app-combo-slot',
  imports: [LucideAngularModule, MoneyPipe],
  templateUrl: './combo-slot.component.html',
})
export class ComboSlotComponent {
  readonly component = input.required<ComboComponent>();
  readonly picks = input.required<(number | null)[]>();
  readonly showErrors = input(false);
  readonly pick = output<{ slot: number; productId: number }>();

  readonly slots = computed(() => Array.from({ length: this.component().quantity }, (_, i) => i));
  readonly invalid = computed(() => this.showErrors() && this.picks().some((p) => p == null));
}
