package io.paymentcollection.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import io.paymentcollection.AbstractElasticsearchIntegrationTest;
import io.paymentcollection.payment.domain.Payment;
import io.paymentcollection.payment.infrastructure.messaging.OutboxWorker;
import io.paymentcollection.payment.infrastructure.persistence.*;
import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @Autowired private JpaDeadLetterRepository deadLetterRepository;

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

  @Test
  void shouldMoveEventToDeadLetterAfterMaxRetries() {

    // given: invalid payload - payload as map (not matching PaymentDocument)
    Map<String, Object> badPayload = new HashMap<>();
    badPayload.put("id", 21331);
    badPayload.put("date", "this-is-not-a-date");
    badPayload.put("amount", "not-a-number\"");

    OutboxEvent badEvent =
        OutboxEvent.builder()
            .aggregateType("Payment")
            .aggregateId("bad-1")
            .eventType("CREATED")
            .payload(badPayload) // <-- will fail deserialization
            .processed(false)
            .createdAt(Instant.now())
            .build();

    outboxRepo.save(badEvent);

    // when
    outboxWorker.processOutbox(); // will trigger retries, then @Recover

    // then (wait until retries + recover finish)
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<DeadLetterEvent> deadLetters = deadLetterRepository.findAll();
              assertThat(deadLetters).isNotEmpty();
              assertThat(deadLetters.get(0).getAggregateId()).isEqualTo("bad-1");
            });
  }

  // @Test
  void shouldSearchByStatus() throws Exception {

    // --- fill in phase ---
    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-100")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":100.0,"currency":"EUR","method":"CARD","customerId":"CUST-1"}
            """
                    .replace(" ", "")));

    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-101")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":200.0,"currency":"EUR","method":"CARD","customerId":"CUST-2"}
            """
                    .replace(" ", "")));

    outboxWorker.processOutbox(); // flush events into ES

    // --- exercise + assert phase ---
    mvc.perform(
            get("/api/payments/search")
                .param("page", "0")
                .param("size", "10")
                .param("status", "CREATED")) // assuming "CREATED" is the status you saved
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payments", hasSize(2)))
        .andExpect(jsonPath("$.payments[*].customerId", containsInAnyOrder("CUST-1", "CUST-2")));

    mvc.perform(
            get("/api/payments/search")
                .param("page", "0")
                .param("size", "10")
                .param("status", "SUCCESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payments", hasSize(2)))
        .andExpect(jsonPath("$.payments[*].id", containsInAnyOrder("p1", "p3")));
  }

  // @Test
  void shouldFilterByDateRange() throws Exception {
    // --- arrange ---
    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-201")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":120.0,"currency":"EUR","method":"CARD",
                 "customerId":"CUST-1","status":"CREATED"}
            """
                    .replace(" ", "")));

    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-202")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":220.0,"currency":"EUR","method":"CARD",
                 "customerId":"CUST-2","status":"CREATED"}
            """
                    .replace(" ", "")));

    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-203")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":320.0,"currency":"EUR","method":"CARD",
                 "customerId":"CUST-3","status":"CREATED"}
            """
                    .replace(" ", "")));

    outboxWorker.processOutbox();

    mvc.perform(
            get("/api/payments/search")
                .param("page", "0")
                .param("size", "10")
                .param("fromDate", "2024-01-01T00:00:00Z")
                .param("toDate", "2024-01-31T23:59:59Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payments[*].id", containsInAnyOrder("p1", "p2")));
  }

  // @Test
  void shouldApplyPagination() throws Exception {
    // --- arrange ---
    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-301")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":150.0,"currency":"EUR","method":"CARD",
                 "customerId":"CUST-1","status":"SUCCESS"}
            """
                    .replace(" ", "")));

    mvc.perform(
        post("/api/payments")
            .header("Idempotency-Key", "k-302")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"amount":250.0,"currency":"EUR","method":"CARD",
                 "customerId":"CUST-2","status":"SUCCESS"}
            """
                    .replace(" ", "")));

    outboxWorker.processOutbox();

    mvc.perform(
            get("/api/payments/search")
                .param("page", "0")
                .param("size", "1")
                .param("status", "SUCCESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payments", hasSize(1)))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
  }
}
