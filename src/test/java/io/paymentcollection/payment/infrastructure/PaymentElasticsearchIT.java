package io.paymentcollection.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import io.paymentcollection.AbstractElasticsearchIntegrationTest;
import io.paymentcollection.payment.domain.Payment;
import io.paymentcollection.payment.infrastructure.messaging.OutboxWorker;
import io.paymentcollection.payment.infrastructure.persistence.ElasticsearchPaymentRepository;
import io.paymentcollection.payment.infrastructure.persistence.JpaOutboxRepository;
import io.paymentcollection.payment.infrastructure.persistence.JpaPaymentRepository;
import io.paymentcollection.payment.infrastructure.persistence.OutboxEvent;
import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 10.
 */
@SpringBootTest
class PaymentElasticsearchIT extends AbstractElasticsearchIntegrationTest {

  @Autowired MockMvc mvc;

  @Autowired private ElasticsearchClient client;
  @Autowired JpaPaymentRepository paymentRepo;
  @Autowired JpaOutboxRepository outboxRepo;
  @Autowired OutboxWorker outboxWorker;
  @Autowired private ElasticsearchPaymentRepository esRepo;

  @Test
  void shouldCreatePaymentsIndexOnStartup() throws Exception {
    var response = client.indices().get(new GetIndexRequest.Builder().index("payments").build());

    assertThat(response.result().containsKey("payments")).isTrue();
  }

  @Test
  void shouldSaveAndRetrievePaymentDocument() {
    // given
    PaymentDocument payment =
        PaymentDocument.builder()
            .id("test-1")
            .date(Instant.now())
            .amount(99.99)
            .status("COMPLETED")
            .customerId("CUST123")
            .customerName("Alice Johnson")
            .build();

    esRepo.save(payment);

    // when
    var found = esRepo.findById("test-1");

    // then
    assertThat(found).isPresent();
    assertThat(found.get().getCustomerName()).isEqualTo("Alice Johnson");
  }

  @Test
  void shouldProcessOutboxAndWriteToElasticsearch() throws Exception {

    // Arrange: what the handler should return
    var p = new Payment();
    p.setId(1L);
    p.setAmount(new BigDecimal("12.50"));
    p.setCurrency("EUR");
    p.setMethod("CARD");
    p.setCustomerId("00000000-0000-0000-0000-000000000001");
    p.setStatus("CREATED");

    // Act + Assert
    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                        {"amount":12.50,"currency":"EUR","method":"CARD",
                         "customerId":"00000000-0000-0000-0000-000000000001"}"""
                    .replace(" ", "")));

    Payment paymentFound = paymentRepo.findById(p.getId()).orElseThrow();
    assertThat(paymentFound.getId()).isEqualTo(p.getId());
    assertThat(paymentFound.getAmount()).isEqualByComparingTo("12.50");
    assertThat(paymentFound.getCurrency()).isEqualTo("EUR");

    OutboxEvent outboxEventFound = outboxRepo.findByAggregateId(p.getId().toString()).orElseThrow();
    assertThat(outboxEventFound).isNotNull();
    assertThat(outboxEventFound.isProcessed()).isFalse();
    assertThat(outboxEventFound.getAggregateId()).isEqualTo(p.getId().toString());

    // write data into ES
    outboxWorker.processOutbox();

    var found = esRepo.findById(paymentFound.getId().toString());

    // then
    assertThat(found).isPresent();
    assertThat(found.get().getCustomerId()).isEqualTo(paymentFound.getCustomerId());
  }
}
