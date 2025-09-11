package io.paymentcollection.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEvent, Long> {
  List<OutboxEvent> findTop50ByProcessedFalseOrderByCreatedAtAsc();

  Optional<OutboxEvent> findByAggregateId(String aggregateId);
}
