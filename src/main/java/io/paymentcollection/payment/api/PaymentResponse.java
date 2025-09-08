package io.paymentcollection.payment.api;

import java.math.BigDecimal;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
public record PaymentResponse(
    Long id, BigDecimal amount, String currency, String method, String customerId, String status) {}
