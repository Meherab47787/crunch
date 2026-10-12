import { OrderStatus } from '../models/api.models';

export const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: 'Order received',
  CONFIRMED: 'Confirmed',
  PREPARING: 'In the kitchen',
  READY_FOR_PICKUP: 'Ready',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Completed',
  CANCELLED: 'Cancelled',
};

/** Short labels for staff screens. */
export const STATUS_SHORT: Record<OrderStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  PREPARING: 'Preparing',
  READY_FOR_PICKUP: 'Ready',
  OUT_FOR_DELIVERY: 'Out for delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

export const STATUS_CLASS: Record<OrderStatus, string> = {
  PENDING: 'bg-ember-100 text-ember-700',
  CONFIRMED: 'bg-sky-100 text-sky-800',
  PREPARING: 'bg-violet-100 text-violet-800',
  READY_FOR_PICKUP: 'bg-emerald-100 text-emerald-800',
  OUT_FOR_DELIVERY: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-charcoal-800/10 text-charcoal-700',
  CANCELLED: 'bg-tomato-100 text-tomato-700',
};

export const ALL_STATUSES: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PREPARING',
  'READY_FOR_PICKUP',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED',
];

export function isActive(status: OrderStatus): boolean {
  return status !== 'DELIVERED' && status !== 'CANCELLED';
}
