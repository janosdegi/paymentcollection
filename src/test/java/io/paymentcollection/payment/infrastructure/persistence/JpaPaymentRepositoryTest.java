package io.paymentcollection.payment.infrastructure.persistence;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 07.
 */
import static org.assertj.core.api.Assertions.*;

import io.paymentcollection.payment.domain.Payment;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlywayAutoConfiguration.class) // ensure Flyway runs with DataJpaTest
class JpaPaymentRepositoryTest {

  @Container
  static PostgreSQLContainer<?> pg =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("appdb")
          .withUsername("appuser")
          .withPassword("apppass");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add(
        "spring.jpa.hibernate.ddl-auto",
        () -> "create-drop"); // validate - doesn't work, create-drop
    r.add("spring.flyway.enabled", () -> "true");
  }

  private final JpaPaymentRepository repo;

  @Autowired
  JpaPaymentRepositoryTest(JpaPaymentRepository repo) {
    this.repo = repo;
  }

  @Test
  void save_and_load_payment_roundtrips_including_metadata_and_timestamps() {
    Payment p = new Payment();
    p.setAmount(new BigDecimal("12.34"));
    p.setCurrency("EUR");
    p.setStatus("CREATED");
    p.setMethod("CARD");
    p.setProviderRef("prov-123");
    p.setCustomerId("cust-42");
    p.setMetadata(Map.of("device", "mobile", "ip", "127.0.0.1"));

    Payment saved = repo.save(p);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Payment found = repo.findById(saved.getId()).orElseThrow();
    assertThat(found.getAmount()).isEqualByComparingTo("12.34");
    assertThat(found.getCurrency()).isEqualTo("EUR");
    assertThat(found.getMetadata()).containsEntry("device", "mobile");
  }

  @Test
  void validation_rejects_non_positive_amount() {
    Payment p = new Payment();
    p.setAmount(new BigDecimal("0.00")); // invalid
    p.setCurrency("EUR");
    p.setStatus("CREATED");
    p.setMethod("CARD");

    assertThatThrownBy(() -> repo.saveAndFlush(p)).isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void validation_rejects_bad_currency() {
    Payment p = new Payment();
    p.setAmount(new BigDecimal("5.00"));
    p.setCurrency("Eu"); // invalid: not 3 uppercase letters
    p.setStatus("CREATED");
    p.setMethod("CARD");

    assertThatThrownBy(() -> repo.saveAndFlush(p)).isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void pre_update_sets_updatedAt() throws InterruptedException {
    Payment p = new Payment();
    p.setAmount(new BigDecimal("10.00"));
    p.setCurrency("EUR");
    p.setStatus("CREATED");
    p.setMethod("CARD");
    Payment saved = repo.saveAndFlush(p);
    OffsetDateTime firstUpdated = saved.getUpdatedAt();

    // mutate and update
    saved.setStatus("AUTHORIZED");
    Payment updated = repo.saveAndFlush(saved);

    assertThat(updated.getUpdatedAt()).isAfter(firstUpdated);
  }
}
