package io.paymentcollection.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.paymentcollection.ITBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */

// ./mvnw clean test -Dtest=PaymentFlowIT

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIT extends ITBase {
  @Autowired MockMvc mvc;

  @Test
  void idempotent_replay_returns_existing_payment() throws Exception {
    var body =
        """
      {"amount": "15.00", "currency":"EUR","method":"CARD",
       "customerId":"00000000-0000-0000-0000-000000000001"}"""
            .replace(" ", "");

    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "KEY-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "KEY-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(header().string("Location", Matchers.containsString("/api/payments/")));
  }

  @Test
  void idempotent_conflict_on_different_body() throws Exception {
    String a =
        "{\"amount\":10,\"currency\":\"EUR\",\"method\":\"CARD\",\"customerId\":\"00000000-0000-0000-0000-000000000001\"}";
    String b =
        "{\"amount\":11,\"currency\":\"EUR\",\"method\":\"CARD\",\"customerId\":\"00000000-0000-0000-0000-000000000001\"}";
    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "KEY-ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(a))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "KEY-ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .content(b))
        .andExpect(status().isConflict());
  }
}
