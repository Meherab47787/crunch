import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-account-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './account-layout.component.html',
})
export class AccountLayoutComponent {
  readonly auth = inject(AuthService);
  readonly tabs = [
    { path: 'orders', label: 'Orders', icon: 'clipboard-list' },
    { path: 'profile', label: 'Profile', icon: 'user' },
    { path: 'addresses', label: 'Addresses', icon: 'map-pin' },
  ];
}
