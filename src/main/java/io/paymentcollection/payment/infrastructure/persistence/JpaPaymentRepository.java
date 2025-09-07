package io.paymentcollection.payment.infrastructure.persistence;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 07.
 */
import io.paymentcollection.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentRepository extends JpaRepository<Payment, Long> {}
