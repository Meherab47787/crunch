import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { Address } from '../../core/models/api.models';
import { AccountService } from '../../core/services/account.service';
import { ToastService } from '../../core/services/toast.service';
import { errorMessage } from '../../core/utils/errors';
import { remote } from '../../core/utils/remote';
import { ConfirmService } from '../../core/services/confirm.service';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ModalComponent } from '../../shared/components/modal.component';

@Component({
  selector: 'app-addresses',
  imports: [ReactiveFormsModule, LucideAngularModule, EmptyStateComponent, ModalComponent],
  templateUrl: './addresses.component.html',
})
export class AddressesComponent {
  private readonly account = inject(AccountService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  readonly list = remote(() => this.account.addresses());
  /** undefined = closed, null = new, Address = editing */
  readonly editing = signal<Address | null | undefined>(undefined);
  readonly saving = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(50)]],
    line1: ['', [Validators.required, Validators.maxLength(200)]],
    line2: [''],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    postalCode: [''],
    instructions: [''],
    defaultAddress: [false],
  });

  open(a: Address | null) {
    this.form.reset({
      label: a?.label ?? '', line1: a?.line1 ?? '', line2: a?.line2 ?? '', city: a?.city ?? '',
      postalCode: a?.postalCode ?? '', instructions: a?.instructions ?? '', defaultAddress: a?.defaultAddress ?? false,
    });
    this.editing.set(a);
  }

  save() {
    const v = this.form.getRawValue();
    const body = { ...v, line2: v.line2 || null, postalCode: v.postalCode || null, instructions: v.instructions || null };
    const current = this.editing();
    this.saving.set(true);
    (current ? this.account.updateAddress(current.id, body) : this.account.createAddress(body)).subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(undefined);
        this.toast.success('Address saved');
        this.list.reload();
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error(errorMessage(e));
      },
    });
  }

  async remove(a: Address) {
    if (!(await this.confirm.ask({ title: 'Delete address?', message: `Remove “${a.label}”?`, confirmLabel: 'Delete', danger: true }))) return;
    this.account.deleteAddress(a.id).subscribe({
      next: () => {
        this.toast.success('Address deleted');
        this.list.reload();
      },
      error: (e) => this.toast.error(errorMessage(e)),
    });
  }
}
