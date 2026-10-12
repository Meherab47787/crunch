import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Role, User } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import { ToggleSwitchComponent } from '../shared/toggle-switch.component';
import { StaffCreateModalComponent } from './staff-create-modal.component';

@Component({
  selector: 'app-admin-staff',
  imports: [DatePipe, LucideAngularModule, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent, TableSkeletonComponent,
    ToggleSwitchComponent, StaffCreateModalComponent],
  templateUrl: './staff.component.html',
})
export class StaffComponent {
  private readonly admin = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  readonly me = this.auth.user;
  readonly staff = remote(() => this.admin.staff());
  readonly filter = signal('');
  readonly creating = signal(false);
  readonly busy = signal(new Set<number>());
  readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const all = this.staff.data()?.content ?? [];
    return q ? all.filter((u) => u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)) : all;
  });

  async changeRole(u: User, role: Role, e: Event) {
    const select = e.target as HTMLSelectElement;
    if (role === u.role) return;
    if (role === 'CUSTOMER') {
      const ok = await this.confirm.ask({
        title: `Remove staff access for ${u.fullName}?`,
        message: 'They will become a regular customer account and lose access to the kitchen and admin screens.',
        confirmLabel: 'Remove access',
        danger: true,
      });
      if (!ok) {
        select.value = u.role;
        return;
      }
    }
    this.update(u, { role, enabled: u.enabled }, () => (select.value = u.role));
  }

  setEnabled(u: User, enabled: boolean) {
    this.update(u, { role: u.role, enabled });
  }

  private update(u: User, body: { role: Role; enabled: boolean }, rollback?: () => void) {
    this.setBusy(u.id, true);
    this.admin.updateUser(u.id, body).subscribe({
      next: (updated) => {
        this.setBusy(u.id, false);
        this.toast.success(`${updated.fullName} updated`);
        const page = this.staff.data();
        if (page) {
          const content = updated.role === 'CUSTOMER' ? page.content.filter((x) => x.id !== u.id) : page.content.map((x) => (x.id === u.id ? updated : x));
          this.staff.set({ ...page, content });
        }
      },
      error: (err) => {
        this.setBusy(u.id, false);
        rollback?.();
        this.toast.error(errorMessage(err, 'Could not update this account.'));
      },
    });
  }

  private setBusy(id: number, on: boolean) {
    const s = new Set(this.busy());
    if (on) s.add(id);
    else s.delete(id);
    this.busy.set(s);
  }
}
