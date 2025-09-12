package io.paymentcollection.payment.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paymentcollection.payment.infrastructure.persistence.*;
import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 12.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!base-integration-test")
public class OutboxProcessor {

  private final JpaOutboxRepository repository;
  private final ElasticsearchPaymentRepository esRepository;
  private final ObjectMapper mapper;
  private final JpaDeadLetterRepository deadLetterRepository;

  @Retryable(
      value = {Exception.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2))
  public void indexEvent(OutboxEvent event) throws JsonProcessingException {
    log.info("Indexing outbox event {} attempt. event: {}", event.getId(), event);

    PaymentDocument doc = mapper.convertValue(event.getPayload(), PaymentDocument.class);
    esRepository.save(doc);
    event.setProcessed(true);
    repository.save(event);

    log.info("Processed outbox event {}", event.getId());
  }

  @Recover
  public void recover(Exception e, OutboxEvent event) {
    log.error("Max retries reached for outbox event {}", event.getId(), e);

    DeadLetterEvent deadLetter =
        DeadLetterEvent.builder()
            .outboxId(event.getId())
            .aggregateType(event.getAggregateType())
            .aggregateId(event.getAggregateId())
            .eventType(event.getEventType())
            .payload(event.getPayload())
            .errorMessage(e.getMessage())
            .failedAt(Instant.now())
            .build();

    deadLetterRepository.save(deadLetter);
  }
}
