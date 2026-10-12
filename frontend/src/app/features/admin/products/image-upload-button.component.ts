import { Component, inject, input, output, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { errorMessage } from '../../../core/utils/errors';

/** File picker that uploads an image via AdminService.upload and emits the stored URL. */
@Component({
  selector: 'app-image-upload-button',
  imports: [LucideAngularModule],
  templateUrl: './image-upload-button.component.html',
})
export class ImageUploadButtonComponent {
  private readonly admin = inject(AdminService);
  private readonly toast = inject(ToastService);
  readonly label = input('Upload');
  readonly uploaded = output<string>();
  readonly busy = signal(false);

  onFile(e: Event) {
    const el = e.target as HTMLInputElement;
    const file = el.files?.[0];
    el.value = '';
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.toast.error('Please choose an image file.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toast.error('Images must be 5 MB or smaller.');
      return;
    }
    this.busy.set(true);
    this.admin.upload(file).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.uploaded.emit(r.url);
        this.toast.success('Image uploaded');
      },
      error: (err) => {
        this.busy.set(false);
        this.toast.error(errorMessage(err, 'Upload failed.'));
      },
    });
  }
}
