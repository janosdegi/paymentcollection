package io.paymentcollection.payment.infrastructure.messaging;

import io.paymentcollection.payment.infrastructure.persistence.JpaOutboxRepository;
import io.paymentcollection.payment.infrastructure.persistence.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!base-integration-test")
public class OutboxWorker {

  private final JpaOutboxRepository repository;
  private final OutboxProcessor processor;

  @Scheduled(fixedDelay = 5000) // every 5 seconds
  @Transactional
  public int processOutbox() {
    List<OutboxEvent> events = repository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : events) {
      try {
        processor.indexEvent(event); // goes through proxy, so retry/recover works
      } catch (Exception e) {
        log.error("Failed to process event {}", event.getId(), e);
      }
    }
    return events.size();
  }
}
