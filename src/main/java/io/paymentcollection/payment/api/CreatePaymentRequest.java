package io.paymentcollection.payment.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
public record CreatePaymentRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank @Pattern(regexp = "USD|EUR|GBP|HUF") String currency,
    @NotBlank @Pattern(regexp = "CARD|PAYPAL|SEPA") String method,
    @NotNull String customerId,
    String description) {}
