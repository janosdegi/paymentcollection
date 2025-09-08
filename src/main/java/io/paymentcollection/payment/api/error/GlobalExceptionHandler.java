package io.paymentcollection.payment.api.error;

import static io.paymentcollection.payment.api.error.ProblemFactory.problem;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

  private static final String APP_ERRORS = "https://yourapp.dev/errors/";

  private String traceId(HttpServletRequest req) {
    Object v = req.getAttribute(TraceIdFilter.TRACE_ID_KEY);
    return v != null ? v.toString() : null;
  }

  private ResponseEntity<ProblemDetail> respond(ProblemDetail pd) {
    return ResponseEntity.status(pd.getStatus())
        .contentType(MediaType.valueOf("application/problem+json"))
        .body(pd);
  }

  // 400: JSON parse errors, missing body, etc.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handle(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "Malformed Request",
            ex.getMostSpecificCause().getMessage(),
            APP_ERRORS + "bad-request",
            traceId(req));
    return respond(pd);
  }

  // 400: @Valid on @RequestBody
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handle(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "Request validation failed.",
            APP_ERRORS + "validation",
            traceId(req));
    pd.setProperty("errors", fieldErrors(ex.getBindingResult()));
    return respond(pd);
  }

  // 400: @Validated on @PathVariable/@RequestParam
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handle(
      ConstraintViolationException ex, HttpServletRequest req) {
    var errors =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    Map.of(
                        "field",
                            Optional.ofNullable(cv.getPropertyPath())
                                .map(Object::toString)
                                .orElse(""),
                        "message", cv.getMessage()))
            .toList();

    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            "Request validation failed.",
            APP_ERRORS + "validation",
            traceId(req));
    pd.setProperty("errors", errors);
    return respond(pd);
  }

  // 400: type mismatch (?page=foo)
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handle(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "Invalid Parameter",
            "Parameter '%s' has invalid value '%s'".formatted(ex.getName(), ex.getValue()),
            APP_ERRORS + "invalid-parameter",
            traceId(req));
    return respond(pd);
  }

  // 404: not found
  @ExceptionHandler({java.util.NoSuchElementException.class, EntityNotFoundException.class})
  public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex, HttpServletRequest req) {
    var pd =
        problem(
            HttpStatus.NOT_FOUND,
            "Not Found",
            ex.getMessage() == null ? "Resource not found." : ex.getMessage(),
            APP_ERRORS + "not-found",
            traceId(req));
    return respond(pd);
  }

  // Fallback: 500
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex, HttpServletRequest req) {
    var pd =
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred.",
            APP_ERRORS + "internal",
            traceId(req));
    // Optional: add a sanitized cause or error code
    return respond(pd);
  }

  private List<Map<String, String>> fieldErrors(BindingResult binding) {
    List<Map<String, String>> errors = new ArrayList<>();
    for (FieldError fe : binding.getFieldErrors()) {
      errors.add(Map.of("field", fe.getField(), "message", fe.getDefaultMessage()));
    }
    for (ObjectError oe : binding.getGlobalErrors()) {
      errors.add(Map.of("field", "", "message", oe.getDefaultMessage()));
    }
    return errors;
  }

  /*
  @ExceptionHandler(EmailAlreadyUsedException.class)
  public ResponseEntity<ProblemDetail> handle(EmailAlreadyUsedException ex, HttpServletRequest req) {
      var pd = problem(HttpStatus.CONFLICT, "Email Already Used", ex.getMessage(),
              APP_ERRORS + "email-already-used", traceId(req));
      return respond(pd);
  }
  */
}
