import { Component, inject } from '@angular/core';
import { ConfirmService } from '../../core/services/confirm.service';
import { ModalComponent } from './modal.component';

@Component({
  selector: 'app-confirm-host',
  imports: [ModalComponent],
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmHostComponent {
  readonly svc = inject(ConfirmService);
}
