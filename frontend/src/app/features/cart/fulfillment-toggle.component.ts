import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CartService } from '../../core/services/cart.service';
import { SettingsService } from '../../core/services/settings.service';

@Component({
  selector: 'app-fulfillment-toggle',
  imports: [LucideAngularModule],
  templateUrl: './fulfillment-toggle.component.html',
})
export class FulfillmentToggleComponent {
  readonly cart = inject(CartService);
  readonly settings = inject(SettingsService);
  readonly options = [
    { value: 'DELIVERY' as const, label: 'Delivery', icon: 'bike' },
    { value: 'PICKUP' as const, label: 'Pickup', icon: 'store' },
  ];
}
