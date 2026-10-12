package com.crustcrunch.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle. Allowed transitions are defined here so every caller
 * (customer, kitchen, admin) goes through the same rules.
 *
 * <pre>
 * PENDING -> CONFIRMED -> PREPARING -> READY_FOR_PICKUP -> DELIVERED          (pickup)
 *                                                       -> OUT_FOR_DELIVERY -> DELIVERED (delivery)
 * PENDING / CONFIRMED / PREPARING -> CANCELLED
 * </pre>
 *
 * The kitchen may also undo a step (e.g. a ticket advanced by mistake) while the order is still in the
 * kitchen: CONFIRMED -> PENDING, PREPARING -> CONFIRMED, READY_FOR_PICKUP -> PREPARING. Once an order is
 * out for delivery, delivered or cancelled it can no longer move.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public static final Set<OrderStatus> ACTIVE = EnumSet.of(PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP, OUT_FOR_DELIVERY);
    public static final Set<OrderStatus> KITCHEN_VISIBLE = EnumSet.of(PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP);

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean canTransitionTo(OrderStatus target, FulfillmentType fulfillment) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PREPARING || target == PENDING || target == CANCELLED;
            case PREPARING -> target == READY_FOR_PICKUP || target == CONFIRMED || target == CANCELLED;
            case READY_FOR_PICKUP -> target == PREPARING || (fulfillment == FulfillmentType.DELIVERY
                    ? target == OUT_FOR_DELIVERY
                    : target == DELIVERED);
            case OUT_FOR_DELIVERY -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /** Next "happy path" status, used by the kitchen display's one-tap advance button. */
    public OrderStatus next(FulfillmentType fulfillment) {
        return switch (this) {
            case PENDING -> CONFIRMED;
            case CONFIRMED -> PREPARING;
            case PREPARING -> READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> fulfillment == FulfillmentType.DELIVERY ? OUT_FOR_DELIVERY : DELIVERED;
            case OUT_FOR_DELIVERY -> DELIVERED;
            case DELIVERED, CANCELLED -> null;
        };
    }

    /** The status an order returns to when the kitchen undoes its last step, or null if it can't be undone. */
    public OrderStatus previous() {
        return switch (this) {
            case CONFIRMED -> PENDING;
            case PREPARING -> CONFIRMED;
            case READY_FOR_PICKUP -> PREPARING;
            case PENDING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED -> null;
        };
    }
}
