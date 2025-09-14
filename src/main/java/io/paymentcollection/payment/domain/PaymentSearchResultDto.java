package io.paymentcollection.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 13.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSearchResultDto {
    private String id;
    private Instant date;
    private Double amount;
    private String status;
    private String customerId;
}
