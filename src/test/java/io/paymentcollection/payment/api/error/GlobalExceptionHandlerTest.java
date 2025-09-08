package io.paymentcollection.payment.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.paymentcollection.payment.api.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@WebMvcTest(controllers = ErrorFixtureController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class, TraceIdFilter.class})
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mvc;

  @Test
  void validation_error_returns_problem_json() throws Exception {
    mvc.perform(
            post("/__test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nope\",\"age\":16}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.title").value("Validation Failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.traceId").exists())
        .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field=='age')]").exists());
  }

  @Test
  void not_found_returns_problem_json() throws Exception {
    mvc.perform(get("/__test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.type").value("https://yourapp.dev/errors/not-found"))
        .andExpect(jsonPath("$.detail").value("User 42 not found"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void internal_error_returns_problem_json() throws Exception {
    mvc.perform(get("/__test/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.title").value("Internal Server Error"))
        .andExpect(jsonPath("$.traceId").exists());
  }
}
