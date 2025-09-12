package io.paymentcollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaymentCollectionApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentCollectionApplication.class, args);
  }
}
