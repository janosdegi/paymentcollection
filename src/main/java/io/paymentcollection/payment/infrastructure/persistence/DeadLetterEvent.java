package io.paymentcollection.payment.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
@Entity
@Table(name = "dead_letter")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long outboxId;
  private String aggregateType;
  private String aggregateId;
  private String eventType;

  @Column(name = "payload", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> payload;

  private String errorMessage;
  private Instant failedAt;
}
