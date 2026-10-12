package com.crustcrunch.payment;

import com.crustcrunch.entity.Order;
import com.crustcrunch.enums.PaymentMethod;
import com.crustcrunch.enums.PaymentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Demo online gateway that approves every payment. Replace with a real provider (e.g. Stripe
 * Checkout / PaymentIntents) by implementing {@link PaymentGateway} for {@link PaymentMethod#ONLINE}
 * and disabling this bean with {@code app.payments.simulated=false}.
 */
@Component
@ConditionalOnProperty(name = "app.payments.simulated", havingValue = "true", matchIfMissing = true)
public class SimulatedOnlinePaymentGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.ONLINE;
    }

    @Override
    public String providerName() {
        return "simulated";
    }

    @Override
    public PaymentResult initiate(Order order) {
        return new PaymentResult(PaymentStatus.PAID, "sim_" + UUID.randomUUID(), "Approved (simulated)");
    }

    @Override
    public PaymentResult refund(Order order, String providerReference) {
        return new PaymentResult(PaymentStatus.REFUNDED, providerReference, "Refunded (simulated)");
    }
}
