package io.paymentcollection.payment.infrastructure.messaging;

import io.paymentcollection.payment.domain.Payment;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 11.
 */
@Component
public class PaymentOutboxMapper {

  public static PaymentOutboxPayload toPayload(Payment payment) {
    return new PaymentOutboxPayload(
        payment.getId().toString(),
        Instant.now(), // or payment.getDate() if you have a field
        payment.getAmount().doubleValue(),
        payment.getStatus(),
        payment.getCurrency(),
        payment.getCustomerId(),
        "TODO", // map customer name once available
        payment.getMetadata());
  }
}
