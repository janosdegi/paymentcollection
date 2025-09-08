package io.paymentcollection.payment.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idem_key"))
public class IdempotencyRecord {
  @Id @GeneratedValue private UUID id;

  @Column(name = "idem_key", nullable = false, length = 128)
  private String idemKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash; // SHA-256 hex

  @Column(name = "payment_id")
  private Long paymentId; // FK to payments.id

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "status", nullable = false, length = 16)
  private String status = "CREATED"; // or COMPLETED
}
