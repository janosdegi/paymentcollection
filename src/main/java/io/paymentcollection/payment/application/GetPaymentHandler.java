package io.paymentcollection.payment.application;

import io.paymentcollection.payment.domain.Payment;
import io.paymentcollection.payment.infrastructure.persistence.JpaPaymentRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */

@Component
public class GetPaymentHandler {
    private final JpaPaymentRepository payments;

    public GetPaymentHandler(JpaPaymentRepository payments) {
        this.payments = payments;
    }

    public Optional<Payment> byId(Long id) {
        return payments.findById(id);
    }
}
