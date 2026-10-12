import { Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  templateUrl: './logo.component.html',
})
export class LogoComponent {
  readonly inverse = input(false);
  readonly tagline = input(true);
}
