import { Component, computed, effect, input, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ChevronUp, LucideAngularModule } from 'lucide-angular';
import { startWith } from 'rxjs';
import { GroupAdmin } from '../../../core/models/api.models';

/** Ordered list of attached option groups, bound to a FormControl<number[]>. */
@Component({
  selector: 'app-groups-picker',
  imports: [LucideAngularModule, RouterLink],
  templateUrl: './groups-picker.component.html',
})
export class GroupsPickerComponent {
  readonly upIcon = ChevronUp;
  readonly control = input.required<FormControl<number[]>>();
  readonly groups = input.required<GroupAdmin[]>();
  readonly selected = signal('');
  /** Mirror of the control value so computed() can react to it. */
  private readonly ids = signal<number[]>([]);

  constructor() {
    effect((onCleanup) => {
      const ctrl = this.control();
      const sub = ctrl.valueChanges.pipe(startWith(ctrl.value)).subscribe((v) => this.ids.set(v ?? []));
      onCleanup(() => sub.unsubscribe());
    });
  }

  readonly attached = computed(() => {
    const byId = new Map(this.groups().map((g) => [g.group.id, g]));
    return this.ids()
      .map((id) => byId.get(id))
      .filter((g): g is GroupAdmin => !!g);
  });
  readonly available = computed(() => {
    const set = new Set(this.ids());
    return this.groups().filter((g) => !set.has(g.group.id));
  });

  optionNames(g: GroupAdmin): string {
    return g.group.options.map((o) => o.name).join(', ');
  }

  add() {
    const id = Number(this.selected());
    if (!id) return;
    this.set([...this.control().value, id]);
    this.selected.set('');
  }
  remove(id: number) {
    this.set(this.control().value.filter((x) => x !== id));
  }
  move(i: number, delta: number) {
    const arr = [...this.control().value];
    const [x] = arr.splice(i, 1);
    arr.splice(i + delta, 0, x);
    this.set(arr);
  }
  private set(ids: number[]) {
    this.control().setValue(ids);
    this.control().markAsDirty();
  }
}
