package io.paymentcollection.payment.infrastructure.messaging;

import java.time.Instant;
import java.util.Map;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
public record PaymentOutboxPayload(
    String id,
    Instant date,
    Double amount,
    String status,
    String currency,
    String customerId,
    String customerName,
    Map<String, Object> metadata) {}
