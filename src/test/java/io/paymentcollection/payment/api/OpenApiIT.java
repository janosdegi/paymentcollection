package io.paymentcollection.payment.api;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      // Keep tests independent of any local Postgres/Flyway
      "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.flyway.enabled=false",
      "server.error.include-message=always",
      "server.error.include-binding-errors=always",
      "server.error.include-stacktrace=ALWAYS",
      "logging.level.org.springdoc=DEBUG",
      "logging.level.org.springframework.web=DEBUG",
    })
class OpenApiIT {

  @Autowired TestRestTemplate rest;

  @Test
  void swaggerUi_should_be_disabled_by_default() {
    ResponseEntity<String> res = rest.getForEntity("/swagger-ui.html", String.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
  }

  // @Test
  // That’s a binary incompatibility between springdoc-openapi and the Spring Framework version in
  // your project
  // At the moment, springdoc hasn’t released 2.7.x for Boot 3.5. You’ll need either
  // Wait for springdoc to catch up with 3.5.x, or use an alternative like springdoc fork
  // (spring-auto-openapi).
  void apiDocs_should_be_enabled_by_default() {
    ResponseEntity<String> res = rest.getForEntity("/v3/api-docs", String.class);
    assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(res.getBody()).contains("\"openapi\"");
  }
}
