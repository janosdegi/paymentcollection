package io.paymentcollection.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.paymentcollection.payment.application.CreatePaymentHandler;
import io.paymentcollection.payment.application.GetPaymentHandler;
import io.paymentcollection.payment.application.SearchPaymentsHandler;
import io.paymentcollection.payment.domain.Payment;
import io.paymentcollection.payment.domain.PaymentSearchRequest;
import io.paymentcollection.payment.domain.PaymentSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final CreatePaymentHandler createHandler;
  private final GetPaymentHandler getHandler;
  private final SearchPaymentsHandler searchHandler;
  private final ObjectMapper objectMapper;

  public PaymentController(CreatePaymentHandler handler, GetPaymentHandler getHandler,
                           SearchPaymentsHandler searchHandler, ObjectMapper objectMapper) {
    this.createHandler = handler;
    this.getHandler = getHandler;
    this.searchHandler = searchHandler;
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

    var res = createHandler.handle(cmd);

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

  @GetMapping("/{id}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Get payment by id",
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema =
                        @io.swagger.v3.oas.annotations.media.Schema(
                            implementation = PaymentResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not found",
            content =
                @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/problem+json"))
      })
  public ResponseEntity<?> getById(@PathVariable Long id) {
    return getHandler
        .byId(id)
        .<ResponseEntity<?>>map(p -> ResponseEntity.ok(toResponse(p)))
        .orElseGet(
            () -> {
              var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
              pd.setTitle("Payment Not Found");
              pd.setDetail("Payment %d not found".formatted(id));
              pd.setType(URI.create("https://yourapp.dev/errors/not-found"));
              return ResponseEntity.status(HttpStatus.NOT_FOUND)
                  .contentType(MediaType.valueOf("application/problem+json"))
                  .body(pd);
            });
  }

  @Operation(summary = "Search payments", description = "Search payments by filters, pagination and sorting.")
  @ApiResponse(responseCode = "200", description = "Payments found")
  @GetMapping("/api/payments/search")
  public ResponseEntity<PaymentSearchResponse> search(PaymentSearchRequest request) {
    return ResponseEntity.ok(searchHandler.handle(request));
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
