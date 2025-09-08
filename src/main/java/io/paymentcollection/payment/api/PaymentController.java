package io.paymentcollection.payment.api;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paymentcollection.payment.application.CreatePaymentHandler;
import io.paymentcollection.payment.domain.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final CreatePaymentHandler handler;
  private final ObjectMapper objectMapper;

  public PaymentController(CreatePaymentHandler handler, ObjectMapper objectMapper) {
    this.handler = handler;
    this.objectMapper = objectMapper;
  }

  @PostMapping
  @Operation(
      summary = "Create payment",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Created",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "Replayed (duplicate idempotent request)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(
            responseCode = "409",
            description = "Idempotency conflict",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(
            responseCode = "500",
            description = "Internal error",
            content = @Content(mediaType = "application/problem+json"))
      })
  public ResponseEntity<?> create(
      @Valid @RequestBody CreatePaymentRequest request,
      @Parameter(description = "Idempotency key for safe retries")
          @RequestHeader(name = "Idempotency-Key", required = false)
          String idemKey) {

    var cmd =
        new CreatePaymentHandler.Command(
            request.amount(),
            request.currency(),
            request.method(),
            request.customerId(),
            request.description(),
            idemKey,
            canonicalJson(request));

    var res = handler.handle(cmd);

    if (res.payment() != null) {
      var body = toResponse(res.payment());
      URI location =
          ServletUriComponentsBuilder.fromCurrentRequest()
              .path("/{id}")
              .buildAndExpand(body.id())
              .toUri();

      return ResponseEntity.status(res.status()).location(location).body(body);
    }

    // Minimal fallback; alternatively throw and let GlobalExceptionHandler format RFC7807.
    return ResponseEntity.status(res.status())
        .contentType(MediaType.valueOf("application/problem+json"))
        .body(new SimpleProblem(res.status().value(), res.error()));
  }

  private String canonicalJson(Object o) {
    try {
      return objectMapper.writeValueAsString(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private PaymentResponse toResponse(Payment p) {
    return new PaymentResponse(
        p.getId(), p.getAmount(), p.getCurrency(), p.getMethod(), p.getCustomerId(), p.getStatus());
  }

  // Simple problem payload if you don’t want to throw Exceptions here
  record SimpleProblem(int status, String detail) {}
}
