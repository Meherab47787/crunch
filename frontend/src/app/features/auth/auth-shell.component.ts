import { Component, input } from '@angular/core';

/** Split layout shared by sign-in and registration. */
@Component({
  selector: 'app-auth-shell',
  templateUrl: './auth-shell.component.html',
})
export class AuthShellComponent {
  readonly heading = input.required<string>();
  readonly sub = input('');
}
