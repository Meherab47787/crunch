import { Component, input } from '@angular/core';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { ChevronUp, LucideAngularModule } from 'lucide-angular';
import { fieldError } from '../shared/form-utils';
import { VariantForm, variantForm } from './product-form';

/** Sizes / variants FormArray editor. Existing ids are kept on the row's hidden `id` control. */
@Component({
  selector: 'app-variants-editor',
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './variants-editor.component.html',
})
export class VariantsEditorComponent {
  readonly upIcon = ChevronUp;
  readonly variants = input.required<FormArray<VariantForm>>();
  readonly radioName = 'default-variant-' + Math.random().toString(36).slice(2, 8);
  readonly err = (g: VariantForm, k: 'name' | 'priceDelta', label: string) => fieldError(g.controls[k], label);

  add() {
    const first = this.variants().length === 0;
    this.variants().push(variantForm({ defaultVariant: first }));
    this.variants().markAsDirty();
  }

  remove(i: number) {
    const wasDefault = this.variants().at(i).controls.defaultVariant.value;
    this.variants().removeAt(i);
    if (wasDefault && this.variants().length) this.makeDefault(0);
    this.variants().markAsDirty();
  }

  move(i: number, delta: number) {
    const arr = this.variants();
    const ctrl = arr.at(i);
    arr.removeAt(i, { emitEvent: false });
    arr.insert(i + delta, ctrl);
    arr.markAsDirty();
  }

  makeDefault(i: number) {
    this.variants().controls.forEach((c, j) => c.controls.defaultVariant.setValue(i === j));
    this.variants().markAsDirty();
  }
}
