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
class ActuatorHealthIT {

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
}
