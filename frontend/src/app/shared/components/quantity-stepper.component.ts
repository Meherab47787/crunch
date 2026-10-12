import { Component, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-quantity-stepper',
  imports: [LucideAngularModule],
  templateUrl: './quantity-stepper.component.html',
})
export class QuantityStepperComponent {
  readonly value = input.required<number>();
  readonly min = input(1);
  readonly max = input(50);
  readonly size = input<'sm' | 'md'>('md');
  readonly label = input('item');
  readonly allowRemove = input(false);
  readonly change = output<number>();
}
