package com.crustcrunch.payment;

import com.crustcrunch.entity.Order;
import com.crustcrunch.enums.PaymentMethod;
import com.crustcrunch.enums.PaymentStatus;

/**
 * Integration point for payment providers. Implementations must never receive or store raw card
 * data: online providers are expected to use hosted checkout / tokenisation and return only a reference.
 */
public interface PaymentGateway {

    record PaymentResult(PaymentStatus status, String providerReference, String message) {
    }

    PaymentMethod method();

    String providerName();

    /** Called once when the order is placed. */
    PaymentResult initiate(Order order);

    /** Called when a paid order is cancelled. */
    PaymentResult refund(Order order, String providerReference);
}
