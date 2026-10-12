import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ChevronUp, LucideAngularModule } from 'lucide-angular';
import { CustomizationGroup, CustomizationOption, GroupAdmin, GroupRequest, SelectionType } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal.component';
import { applyServerErrors, blankToNull, fieldError } from '../shared/form-utils';

type OptionForm = FormGroup<{
  id: FormControl<number | null>;
  name: FormControl<string>;
  priceDelta: FormControl<number>;
  defaultOption: FormControl<boolean>;
  available: FormControl<boolean>;
}>;

function optionForm(o?: Partial<CustomizationOption>): OptionForm {
  return new FormGroup({
    id: new FormControl<number | null>(o?.id ?? null),
    name: new FormControl(o?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    priceDelta: new FormControl(o?.priceDelta ?? 0, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(1000)] }),
    defaultOption: new FormControl(o?.defaultOption ?? false, { nonNullable: true }),
    available: new FormControl(o?.available ?? true, { nonNullable: true }),
  });
}

@Component({
  selector: 'app-option-group-form-modal',
  imports: [ReactiveFormsModule, LucideAngularModule, ModalComponent],
  templateUrl: './option-group-form-modal.component.html',
})
export class OptionGroupFormModalComponent implements OnInit {
  readonly upIcon = ChevronUp;
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly group = input<CustomizationGroup | null>(null);
  readonly usage = input(0);
  readonly closed = output<void>();
  readonly saved = output<GroupAdmin>();
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  readonly err = fieldError;
  readonly selectionTypes: { value: SelectionType; label: string }[] = [
    { value: 'SINGLE', label: 'Single' },
    { value: 'MULTIPLE', label: 'Multiple' },
  ];

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    selectionType: new FormControl<SelectionType>('SINGLE', { nonNullable: true }),
    minSelect: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(20)] }),
    maxSelect: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(20)] }),
    options: new FormArray<OptionForm>([]),
  });

  ngOnInit() {
    const g = this.group();
    if (g) {
      this.form.patchValue({ name: g.name, description: g.description ?? '', selectionType: g.selectionType, minSelect: g.minSelect, maxSelect: g.maxSelect });
      [...g.options].sort((a, b) => a.sortOrder - b.sortOrder).forEach((o) => this.form.controls.options.push(optionForm(o)));
    } else {
      this.addOption();
    }
  }

  onTypeChange() {
    if (this.form.controls.selectionType.value === 'SINGLE') {
      this.form.controls.maxSelect.setValue(1);
      if (this.form.controls.minSelect.value > 1) this.form.controls.minSelect.setValue(1);
    }
  }

  addOption() {
    this.form.controls.options.push(optionForm());
  }
  removeOption(i: number) {
    this.form.controls.options.removeAt(i);
  }
  move(i: number, d: number) {
    const arr = this.form.controls.options;
    const c = arr.at(i);
    arr.removeAt(i, { emitEvent: false });
    arr.insert(i + d, c);
  }

  save() {
    this.form.markAllAsTouched();
    this.formError.set(null);
    const v = this.form.getRawValue();
    if (v.options.length === 0) return;
    if (v.minSelect > v.maxSelect) {
      this.formError.set('Min picks cannot be greater than max picks.');
      return;
    }
    if (this.form.invalid) return;
    const body: GroupRequest = {
      name: v.name.trim(),
      description: blankToNull(v.description),
      selectionType: v.selectionType,
      minSelect: Number(v.minSelect),
      maxSelect: v.selectionType === 'SINGLE' ? 1 : Number(v.maxSelect),
      options: v.options.map((o, i) => ({ id: o.id, name: o.name.trim(), priceDelta: Number(o.priceDelta), defaultOption: o.defaultOption, available: o.available, sortOrder: i })),
    };
    const g = this.group();
    this.saving.set(true);
    (g ? this.admin.updateGroup(g.id, body) : this.admin.createGroup(body)).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.toast.success(g ? 'Option group saved' : 'Option group created');
        this.saved.emit(res);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(applyServerErrors(this.form, err));
      },
    });
  }
}
