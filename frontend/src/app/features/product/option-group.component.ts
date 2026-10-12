import { Component, computed, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CustomizationGroup } from '../../core/models/api.models';
import { MoneyPipe } from '../../core/utils/money.pipe';

/** Renders one customization group as radio cards (SINGLE) or checkbox chips (MULTIPLE). */
@Component({
  selector: 'app-option-group',
  imports: [LucideAngularModule, MoneyPipe],
  templateUrl: './option-group.component.html',
})
export class OptionGroupComponent {
  readonly group = input.required<CustomizationGroup>();
  readonly selected = input.required<number[]>();
  readonly showErrors = input(false);
  readonly toggle = output<number>();

  readonly atMax = computed(() => this.selected().length >= this.group().maxSelect);
  readonly invalid = computed(() => this.showErrors() && this.selected().length < this.group().minSelect);
  readonly hint = computed(() => {
    const g = this.group();
    if (g.selectionType === 'SINGLE') return g.description ?? (g.required ? 'Pick one' : 'Pick one if you like');
    const n = this.selected().length;
    return `${g.description ?? 'Pick any'} · ${n}/${g.maxSelect} selected`;
  });

  onClick(e: Event, optionId: number): void {
    // let the parent own the state (radio inputs would otherwise stay checked when cleared)
    e.preventDefault();
    this.toggle.emit(optionId);
  }
}
