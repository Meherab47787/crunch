package com.crustcrunch.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.crustcrunch.enums.FulfillmentType.DELIVERY;
import static com.crustcrunch.enums.FulfillmentType.PICKUP;
import static com.crustcrunch.enums.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void deliveryHappyPathTransitionsAreAllowed() {
        assertThat(PENDING.canTransitionTo(CONFIRMED, DELIVERY)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(PREPARING, DELIVERY)).isTrue();
        assertThat(PREPARING.canTransitionTo(READY_FOR_PICKUP, DELIVERY)).isTrue();
        assertThat(READY_FOR_PICKUP.canTransitionTo(OUT_FOR_DELIVERY, DELIVERY)).isTrue();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(DELIVERED, DELIVERY)).isTrue();
    }

    @Test
    void pickupHappyPathSkipsOutForDelivery() {
        assertThat(PENDING.canTransitionTo(CONFIRMED, PICKUP)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(PREPARING, PICKUP)).isTrue();
        assertThat(PREPARING.canTransitionTo(READY_FOR_PICKUP, PICKUP)).isTrue();
        assertThat(READY_FOR_PICKUP.canTransitionTo(DELIVERED, PICKUP)).isTrue();
    }

    @Test
    void pickupOrdersCannotGoOutForDelivery() {
        assertThat(READY_FOR_PICKUP.canTransitionTo(OUT_FOR_DELIVERY, PICKUP)).isFalse();
    }

    @Test
    void deliveryOrdersCannotSkipOutForDelivery() {
        assertThat(READY_FOR_PICKUP.canTransitionTo(DELIVERED, DELIVERY)).isFalse();
    }

    @Test
    void cancellationOnlyAllowedBeforeOrderIsReady() {
        for (FulfillmentType f : FulfillmentType.values()) {
            assertThat(PENDING.canTransitionTo(CANCELLED, f)).isTrue();
            assertThat(CONFIRMED.canTransitionTo(CANCELLED, f)).isTrue();
            assertThat(PREPARING.canTransitionTo(CANCELLED, f)).isTrue();
            assertThat(READY_FOR_PICKUP.canTransitionTo(CANCELLED, f)).isFalse();
            assertThat(OUT_FOR_DELIVERY.canTransitionTo(CANCELLED, f)).isFalse();
        }
    }

    @Test
    void cannotSkipStepsOrLeaveTheKitchenBackwards() {
        assertThat(PENDING.canTransitionTo(PREPARING, DELIVERY)).isFalse();
        assertThat(PENDING.canTransitionTo(DELIVERED, PICKUP)).isFalse();
        assertThat(CONFIRMED.canTransitionTo(READY_FOR_PICKUP, PICKUP)).isFalse();
        assertThat(PREPARING.canTransitionTo(PENDING, DELIVERY)).isFalse();      // only one step back at a time
        assertThat(READY_FOR_PICKUP.canTransitionTo(CONFIRMED, PICKUP)).isFalse();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(READY_FOR_PICKUP, DELIVERY)).isFalse(); // already left the kitchen
        assertThat(PENDING.canTransitionTo(PENDING, DELIVERY)).isFalse();
    }

    @Test
    void kitchenCanUndoOneStepWhileTheOrderIsStillInTheKitchen() {
        for (FulfillmentType f : FulfillmentType.values()) {
            assertThat(CONFIRMED.canTransitionTo(PENDING, f)).isTrue();
            assertThat(PREPARING.canTransitionTo(CONFIRMED, f)).isTrue();
            assertThat(READY_FOR_PICKUP.canTransitionTo(PREPARING, f)).isTrue();
        }
    }

    @Test
    void previousIsAlwaysAValidTransitionAndNullOtherwise() {
        assertThat(PENDING.previous()).isNull();
        assertThat(OUT_FOR_DELIVERY.previous()).isNull();
        assertThat(DELIVERED.previous()).isNull();
        assertThat(CANCELLED.previous()).isNull();
        for (FulfillmentType f : FulfillmentType.values()) {
            for (OrderStatus s : OrderStatus.values()) {
                OrderStatus prev = s.previous();
                if (prev != null) {
                    assertThat(s.canTransitionTo(prev, f)).as("%s.previous() = %s (%s)", s, prev, f).isTrue();
                    assertThat(prev.next(f)).as("undo then redo returns to %s", s).isEqualTo(s);
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
    void terminalStatesHaveNoTransitions(OrderStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        for (FulfillmentType f : FulfillmentType.values()) {
            assertThat(terminal.next(f)).isNull();
            for (OrderStatus target : OrderStatus.values()) {
                assertThat(terminal.canTransitionTo(target, f))
                        .as("%s -> %s (%s)", terminal, target, f).isFalse();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    void nonTerminalStatesAreNotTerminal(OrderStatus status) {
        assertThat(status.isTerminal()).isFalse();
    }

    @Test
    void nextFollowsTheDeliveryHappyPath() {
        assertThat(PENDING.next(DELIVERY)).isEqualTo(CONFIRMED);
        assertThat(CONFIRMED.next(DELIVERY)).isEqualTo(PREPARING);
        assertThat(PREPARING.next(DELIVERY)).isEqualTo(READY_FOR_PICKUP);
        assertThat(READY_FOR_PICKUP.next(DELIVERY)).isEqualTo(OUT_FOR_DELIVERY);
        assertThat(OUT_FOR_DELIVERY.next(DELIVERY)).isEqualTo(DELIVERED);
    }

    @Test
    void nextFollowsThePickupHappyPath() {
        assertThat(READY_FOR_PICKUP.next(PICKUP)).isEqualTo(DELIVERED);
    }

    @Test
    void nextIsAlwaysAValidTransition() {
        for (FulfillmentType f : FulfillmentType.values()) {
            for (OrderStatus s : OrderStatus.values()) {
                OrderStatus next = s.next(f);
                if (next != null) {
                    assertThat(s.canTransitionTo(next, f)).as("%s.next(%s) = %s", s, f, next).isTrue();
                }
            }
        }
    }

    @Test
    void kitchenVisibleStatesExcludeDeliveryAndTerminalStates() {
        assertThat(KITCHEN_VISIBLE).containsExactlyInAnyOrder(PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP);
        assertThat(ACTIVE).doesNotContain(DELIVERED, CANCELLED);
    }
}
