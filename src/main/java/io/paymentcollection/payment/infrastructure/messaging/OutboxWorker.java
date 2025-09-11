package io.paymentcollection.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.paymentcollection.payment.infrastructure.persistence.ElasticsearchPaymentRepository;
import io.paymentcollection.payment.infrastructure.persistence.JpaOutboxRepository;
import io.paymentcollection.payment.infrastructure.persistence.OutboxEvent;
import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
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
  private final ElasticsearchPaymentRepository esRepository;
  private final ObjectMapper mapper;

  @Scheduled(fixedDelay = 5000) // every 5 seconds
  @Transactional
  public void processOutbox() {
    List<OutboxEvent> events = repository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : events) {
      try {
        PaymentDocument doc = mapper.convertValue(event.getPayload(), PaymentDocument.class);

        esRepository.save(doc);

        event.setProcessed(true);
        repository.save(event);
        log.info("Processed outbox event {}", event.getId());
      } catch (Exception e) {
        log.error("Failed to process event {}", event.getId(), e);
      }
    }
  }
}
