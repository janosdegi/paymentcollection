package io.paymentcollection.payment.infrastructure.seed;

import io.paymentcollection.payment.application.CreatePaymentHandler;
import io.paymentcollection.payment.infrastructure.messaging.OutboxWorker;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PaymentSeeder implements CommandLineRunner {

  private final CreatePaymentHandler handler;
  private final OutboxWorker outboxWorker; // optional

  @Value("${app.seed.count:10000}")
  int count;

  @Value("${app.seed.runOutboxAfter:true}")
  boolean runOutboxAfter;

  String[] currencies = {"EUR", "USD", "GBP", "HUF"};
  String[] methods = {"CARD", "PAYPAL", "BANK_TRANSFER", "CASH"};

  @Override
  public void run(String... args) {
    var rnd = ThreadLocalRandom.current();
    for (int i = 0; i < count; i++) {
      String currency = currencies[rnd.nextInt(currencies.length)];
      String method = methods[rnd.nextInt(methods.length)];
      var cmd =
          new CreatePaymentHandler.Command(
              BigDecimal.valueOf(rnd.nextDouble(1, 1000)),
              currency,
              method,
              "CUST-" + (i % 500),
              "Seed payment " + i,
              "IDEMP-" + i,
              "{}" // canonicalJson — keep minimal or serialize Payment
              );
      handler.handle(cmd);
    }
    if (runOutboxAfter) {
      int processed;
      do {
        processed = outboxWorker.processOutbox();
      } while (processed > 0);
    }
    log.info("Seeded {} payments.", count);
  }
}
