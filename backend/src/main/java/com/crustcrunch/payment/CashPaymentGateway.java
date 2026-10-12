package com.crustcrunch.payment;

import com.crustcrunch.entity.Order;
import com.crustcrunch.enums.PaymentMethod;
import com.crustcrunch.enums.PaymentStatus;
import org.springframework.stereotype.Component;

/** Cash on delivery / at pickup: payment stays PENDING until the order is delivered. */
@Component
public class CashPaymentGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH;
    }

    @Override
    public String providerName() {
        return "cash";
    }

    @Override
    public PaymentResult initiate(Order order) {
        return new PaymentResult(PaymentStatus.PENDING, null, "Pay on delivery or at pickup");
    }

    @Override
    public PaymentResult refund(Order order, String providerReference) {
        return new PaymentResult(PaymentStatus.REFUNDED, providerReference, "No cash collected");
    }
}
