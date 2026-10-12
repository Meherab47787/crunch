import { Component, input } from '@angular/core';
import { FormArray, ReactiveFormsModule } from '@angular/forms';
import { ChevronUp, LucideAngularModule } from 'lucide-angular';
import { ProductSummary } from '../../../core/models/api.models';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { fieldError } from '../shared/form-utils';
import { ChoiceForm, ComboComponentForm, choiceForm, comboComponentForm } from './product-form';

/** COMBO builder: components (e.g. "Main", "Side", "Drink") each with selectable products. Ids preserved. */
@Component({
  selector: 'app-combo-editor',
  imports: [ReactiveFormsModule, LucideAngularModule, MoneyPipe],
  templateUrl: './combo-editor.component.html',
})
export class ComboEditorComponent {
  readonly upIcon = ChevronUp;
  readonly components = input.required<FormArray<ComboComponentForm>>();
  /** Selectable products (standard items). */
  readonly products = input.required<ProductSummary[]>();
  readonly uid = Math.random().toString(36).slice(2, 8);
  readonly err = fieldError;

  addComponent() {
    const c = comboComponentForm();
    c.controls.choices.push(choiceForm({ productId: null, extraPrice: 0, defaultChoice: true }));
    this.components().push(c);
    this.components().markAsDirty();
  }
  removeComponent(i: number) {
    this.components().removeAt(i);
    this.components().markAsDirty();
  }
  moveComponent(i: number, d: number) {
    const arr = this.components();
    const ctrl = arr.at(i);
    arr.removeAt(i, { emitEvent: false });
    arr.insert(i + d, ctrl);
    arr.markAsDirty();
  }
  addChoice(c: ComboComponentForm) {
    const first = c.controls.choices.length === 0;
    c.controls.choices.push(choiceForm({ productId: null, extraPrice: 0, defaultChoice: first }));
    c.markAsDirty();
  }
  removeChoice(c: ComboComponentForm, j: number) {
    const wasDefault = c.controls.choices.at(j).controls.defaultChoice.value;
    c.controls.choices.removeAt(j);
    c.controls.choices.markAsTouched();
    if (wasDefault && c.controls.choices.length) this.makeDefault(c, 0);
    c.markAsDirty();
  }
  makeDefault(c: ComboComponentForm, j: number) {
    c.controls.choices.controls.forEach((ch: ChoiceForm, k: number) => ch.controls.defaultChoice.setValue(k === j));
    c.markAsDirty();
  }
}
