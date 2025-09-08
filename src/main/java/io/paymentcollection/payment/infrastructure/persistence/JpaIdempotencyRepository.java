package io.paymentcollection.payment.infrastructure.persistence;

import io.paymentcollection.payment.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
public interface JpaIdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {
  Optional<IdempotencyRecord> findByIdemKey(String idemKey);
}
