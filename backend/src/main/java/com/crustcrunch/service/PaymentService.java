package com.crustcrunch.service;

import com.crustcrunch.entity.Order;
import com.crustcrunch.entity.Payment;
import com.crustcrunch.enums.PaymentMethod;
import com.crustcrunch.enums.PaymentStatus;
import com.crustcrunch.exception.BusinessRuleException;
import com.crustcrunch.payment.PaymentGateway;
import com.crustcrunch.payment.PaymentGateway.PaymentResult;
import com.crustcrunch.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {

    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);
    private final PaymentRepository payments;

    public PaymentService(List<PaymentGateway> gatewayBeans, PaymentRepository payments) {
        gatewayBeans.forEach(g -> gateways.put(g.method(), g));
        this.payments = payments;
    }

    public Set<PaymentMethod> supportedMethods() {
        return gateways.keySet();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void initiate(Order order) {
        PaymentGateway gateway = gateway(order.getPaymentMethod());
        PaymentResult result = gateway.initiate(order);
        if (result.status() == PaymentStatus.FAILED) {
            throw new BusinessRuleException("Payment was declined: " + result.message());
        }
        record(order, gateway, result);
    }

    /** Marks cash payments collected when the order is handed over. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markCollected(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.CASH && order.getPaymentStatus() == PaymentStatus.PENDING) {
            record(order, gateway(PaymentMethod.CASH), new PaymentResult(PaymentStatus.PAID, null, "Cash collected"));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refundIfPaid(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return;
        }
        PaymentGateway gateway = gateway(order.getPaymentMethod());
        String ref = payments.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .map(Payment::getProviderReference).filter(r -> r != null).findFirst().orElse(null);
        record(order, gateway, gateway.refund(order, ref));
    }

    private void record(Order order, PaymentGateway gateway, PaymentResult result) {
        Payment p = new Payment();
        p.setOrder(order);
        p.setMethod(order.getPaymentMethod());
        p.setStatus(result.status());
        p.setAmount(order.getTotal());
        p.setProvider(gateway.providerName());
        p.setProviderReference(result.providerReference());
        payments.save(p);
        order.setPaymentStatus(result.status());
    }

    private PaymentGateway gateway(PaymentMethod method) {
        PaymentGateway g = gateways.get(method);
        if (g == null) {
            throw new BusinessRuleException("Payment method " + method + " is not available");
        }
        return g;
    }
}
