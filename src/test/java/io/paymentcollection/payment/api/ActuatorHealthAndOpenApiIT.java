package io.paymentcollection.payment.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.paymentcollection.AbstractElasticsearchIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthAndOpenApiIT extends AbstractElasticsearchIntegrationTest {

  @Autowired TestRestTemplate rest;

  @Test
  void actuator_should_expose_only_health_and_info_by_default() {

    // exposed
    assertThat(
            rest.getForEntity("/actuator/health", String.class).getStatusCode().is2xxSuccessful())
        .isTrue();
    assertThat(rest.getForEntity("/actuator/info", String.class).getStatusCode().is2xxSuccessful())
        .isTrue();
    assertThat(rest.getForEntity("/actuator/env", String.class).getStatusCode().is2xxSuccessful())
        .isTrue();
    assertThat(rest.getForEntity("/actuator/beans", String.class).getStatusCode().is2xxSuccessful())
        .isTrue();
  }

  @Test
  void swaggerUi_should_be_enabled_by_default() {
    ResponseEntity<String> res = rest.getForEntity("/swagger-ui.html", String.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  void apiDocs_should_be_enabled_by_default() {
    ResponseEntity<String> res = rest.getForEntity("/v3/api-docs", String.class);
    assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(res.getBody()).contains("\"openapi\"");
  }
}
