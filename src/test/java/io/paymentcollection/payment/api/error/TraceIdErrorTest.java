package io.paymentcollection.payment.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.paymentcollection.payment.application.GetPaymentHandler;
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
@WebMvcTest(controllers = io.paymentcollection.payment.api.PaymentController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class TraceIdErrorTest {

  @Autowired MockMvc mvc;

  @MockitoBean GetPaymentHandler getPaymentHandler;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  io.paymentcollection.payment.application.CreatePaymentHandler handler;

  @Test
  void error_contains_traceId_header_and_body() throws Exception {
    mvc.perform(
            post("/api/payments")
                .header("Idempotency-Key", "bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"amount\":0,\"currency\":\"XXX\",\"method\":\"NOPE\",\"customerId\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
  }
}
