package io.paymentcollection.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import io.paymentcollection.AbstractElasticsearchIntegrationTest;
import io.paymentcollection.payment.infrastructure.persistence.ElasticsearchPaymentRepository;
import io.paymentcollection.payment.infrastructure.persistence.document.PaymentDocument;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 10.
 */
@SpringBootTest
class PaymentElasticsearchIT extends AbstractElasticsearchIntegrationTest {

  @Autowired private ElasticsearchClient client;

  @Autowired private ElasticsearchPaymentRepository repository;

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

    repository.save(payment);

    // when
    var found = repository.findById("test-1");

    // then
    assertThat(found).isPresent();
    assertThat(found.get().getCustomerName()).isEqualTo("Alice Johnson");
  }
}
