import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Category } from '../../../core/models/api.models';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';
import { remote } from '../../../core/utils/remote';
import { ConfirmService } from '../../../core/services/confirm.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state.component';
import { ErrorStateComponent } from '../../../shared/components/error-state.component';
import { AdminPageHeaderComponent } from '../shared/page-header.component';
import { TableSkeletonComponent } from '../shared/table-skeleton.component';
import { CategoryFormModalComponent } from './category-form-modal.component';
import { IconCheck } from '../shared/icon-check';

@Component({
  selector: 'app-admin-categories',
  imports: [RouterLink, LucideAngularModule, EmptyStateComponent, ErrorStateComponent, AdminPageHeaderComponent, TableSkeletonComponent,
    CategoryFormModalComponent],
  templateUrl: './categories.component.html',
})
export class CategoriesComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  readonly icons = inject(IconCheck);
  readonly categories = remote(() => this.admin.categories());
  readonly editing = signal<{ category: Category | null } | null>(null);

  openEditor(category: Category | null) {
    this.editing.set({ category });
  }

  onSaved() {
    this.editing.set(null);
    this.categories.reload();
  }

  async remove(c: Category) {
    const ok = await this.confirm.ask({
      title: `Delete “${c.name}”?`,
      message: c.productCount > 0
        ? `This category still has ${c.productCount} product(s). Move or delete them first, or deactivate the category instead.`
        : 'This menu section will be removed permanently.',
      confirmLabel: 'Delete category',
      danger: true,
    });
    if (!ok) return;
    this.admin.deleteCategory(c.id).subscribe({
      next: () => {
        this.toast.success(`${c.name} deleted`);
        this.categories.reload();
      },
      error: (err) => this.toast.error(errorMessage(err, 'Could not delete the category.')),
    });
  }
}
