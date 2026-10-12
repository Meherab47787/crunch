import { Component, input, output } from '@angular/core';

/** Accessible on/off switch (role="switch") for quick toggles in tables. */
@Component({
  selector: 'app-toggle-switch',
  templateUrl: './toggle-switch.component.html',
})
export class ToggleSwitchComponent {
  readonly checked = input.required<boolean>();
  readonly label = input.required<string>();
  readonly disabled = input(false);
  readonly toggled = output<boolean>();
}
