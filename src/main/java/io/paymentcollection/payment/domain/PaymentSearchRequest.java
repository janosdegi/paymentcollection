package io.paymentcollection.payment.domain;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 13.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSearchRequest {
  private Instant fromDate;
  private Instant toDate;
  private List<String> status;
  private Double minAmount;
  private Double maxAmount;
  private String customerId;
  private int page;
  private int size;
  private String sortBy; // e.g. "date"
  private String sortOrder; // ASC or DESC
}
