import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmptyStateComponent } from '../shared/components/empty-state.component';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, EmptyStateComponent],
  templateUrl: './not-found.component.html',
})
export class NotFoundComponent {}
