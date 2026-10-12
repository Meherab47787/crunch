import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { SettingsService } from './core/services/settings.service';
import { ConfirmHostComponent } from './shared/components/confirm-dialog.component';
import { ToastContainerComponent } from './shared/components/toast-container.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainerComponent, ConfirmHostComponent],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    inject(SettingsService).load();
    inject(AuthService).refreshMe();
  }
}
