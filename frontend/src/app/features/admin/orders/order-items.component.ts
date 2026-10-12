import { Component, computed, input } from '@angular/core';
import { OrderItem } from '../../../core/models/api.models';
import { MoneyPipe } from '../../../core/utils/money.pipe';

/** Line items with every option / combo choice, grouped by group name. */
@Component({
  selector: 'app-order-items',
  imports: [MoneyPipe],
  templateUrl: './order-items.component.html',
})
export class OrderItemsComponent {
  readonly items = input.required<OrderItem[]>();
  readonly rows = computed(() =>
    this.items().map((item) => {
      const map = new Map<string, { name: string; combo: boolean; choices: OrderItem['customizations'] }>();
      for (const c of item.customizations) {
        const key = c.kind + ':' + c.groupName;
        if (!map.has(key)) map.set(key, { name: c.groupName, combo: c.kind === 'COMBO_CHOICE', choices: [] });
        map.get(key)!.choices.push(c);
      }
      // combo choices first, then options
      const groups = [...map.values()].sort((a, b) => Number(b.combo) - Number(a.combo));
      return { item, groups };
    }),
  );
}
