package io.paymentcollection.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
@Repository
public interface JpaDeadLetterRepository extends JpaRepository<DeadLetterEvent, Long> {}
