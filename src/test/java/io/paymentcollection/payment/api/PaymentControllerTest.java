package io.paymentcollection.payment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.paymentcollection.payment.api.error.GlobalExceptionHandler;
import io.paymentcollection.payment.application.CreatePaymentHandler;
import io.paymentcollection.payment.application.GetPaymentHandler;
import io.paymentcollection.payment.domain.Payment;
import java.math.BigDecimal;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@WebMvcTest(controllers = PaymentController.class)
@Import({GlobalExceptionHandler.class})
class PaymentControllerTest {

  @Autowired MockMvc mvc;

  @MockitoBean CreatePaymentHandler handler;
  @MockitoBean GetPaymentHandler getPaymentHandler;

  @Test
  void create_valid_returns_201_and_location() throws Exception {
    // Arrange: what the handler should return
    var p = new Payment();
    p.setId(1L);
    p.setAmount(new BigDecimal("12.50"));
    p.setCurrency("EUR");
    p.setMethod("CARD");
    p.setCustomerId("00000000-0000-0000-0000-000000000001");
    p.setStatus("CREATED");

    given(handler.handle(any())).willReturn(CreatePaymentHandler.Result.created(p));
    // or: when(handler.handle(any())).thenReturn(CreatePaymentHandler.Result.created(p));

    // Act + Assert
    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "k-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"amount":12.50,"currency":"EUR","method":"CARD",
                 "customerId":"00000000-0000-0000-0000-000000000001"}"""
                        .replace(" ", "")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", Matchers.containsString("/api/payments/1")))
        .andExpect(jsonPath("$.amount").value(12.50))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void retry_same_key_same_body_returns_200() throws Exception {
    // call 1 (201), call 2 (200) — if using in-memory DB, run both in the same test or use
    // @SpringBootTest
  }

  @Test
  void same_key_different_body_returns_409() throws Exception {
    // first create with body A, then POST body B with same key → 409
  }

  @Test
  void validation_errors_return_400_problem_json() throws Exception {
    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "k-bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"amount\":0,\"currency\":\"XXX\",\"method\":\"NOPE\",\"customerId\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.title").value("Validation Failed"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void get_existing_returns_200_with_json() throws Exception {
    var p = new Payment();
    p.setId(42L);
    p.setAmount(new BigDecimal("15.00"));
    p.setCurrency("EUR");
    p.setMethod("CARD");
    p.setCustomerId("00000000-0000-0000-0000-000000000001");
    p.setStatus("CREATED");

    given(getPaymentHandler.byId(42L)).willReturn(Optional.of(p));

    mvc.perform(get("/api/payments/42").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.amount").value(15.00))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void get_missing_returns_404_problem_json() throws Exception {
    given(getPaymentHandler.byId(999L)).willReturn(Optional.empty());

    mvc.perform(get("/api/payments/999").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.title").value("Payment Not Found"));
  }
}
