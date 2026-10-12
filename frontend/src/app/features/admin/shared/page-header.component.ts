import { Component, input } from '@angular/core';

/** Title row for admin pages; projected content becomes the action area. */
@Component({
  selector: 'app-admin-page-header',
  templateUrl: './page-header.component.html',
})
export class AdminPageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
  readonly eyebrow = input<string>();
}
