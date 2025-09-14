package io.paymentcollection.payment.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 13.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSearchResponse {
    private List<PaymentSearchResultDto> payments;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
