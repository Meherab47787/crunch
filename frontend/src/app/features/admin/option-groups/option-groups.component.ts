import { Component, computed, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { GroupAdmin } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { MoneyPipe } from '../../../core/utils/money.pipe';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { OptionGroupFormModalComponent } from './option-group-form-modal.component';

@Component({
  selector: 'app-admin-option-groups',
  imports: [LucideAngularModule, MoneyPipe, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent, OptionGroupFormModalComponent],
  templateUrl: './option-groups.component.html',
})
export class OptionGroupsComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  readonly groups = remote(() => this.admin.groups());
  readonly filter = signal('');
  readonly editing = signal<{ g: GroupAdmin | null } | null>(null);
  readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const all = this.groups.data() ?? [];
    if (!q) return all;
    return all.filter((g) => g.group.name.toLowerCase().includes(q) || g.group.options.some((o) => o.name.toLowerCase().includes(q)));
  });

  async remove(ga: GroupAdmin) {
    const ok = await this.confirm.ask({
      title: `Delete “${ga.group.name}”?`,
      message: ga.usedByProducts > 0
        ? `It is attached to ${ga.usedByProducts} product(s); detach it from those products first.`
        : 'This option group will be removed permanently.',
      confirmLabel: 'Delete group',
      danger: true,
    });
    if (!ok) return;
    this.admin.deleteGroup(ga.group.id).subscribe({
      next: () => {
        this.toast.success(`${ga.group.name} deleted`);
        this.groups.reload();
      },
      error: (err) => this.toast.error(errorMessage(err, 'Could not delete the option group.')),
    });
  }
}
