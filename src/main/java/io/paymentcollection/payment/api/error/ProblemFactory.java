package io.paymentcollection.payment.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */

/*
RFC 7807 defines a standard JSON error format called application/problem+json.
It ensures APIs return errors consistently with fields like:
{
  "type": "https://yourapp.dev/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed.",
  "traceId": "1f3a2c0d8b7e4c5f",
  "errors": [
    {"field": "email", "message": "must be a well-formed email address"},
    {"field": "age", "message": "must be greater than or equal to 18"}
  ]
}
*/
public final class ProblemFactory {

  public static ProblemDetail problem(
      HttpStatus status, String title, String detail, String typeUri, String traceId) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    if (typeUri != null && !typeUri.isBlank()) {
      pd.setType(java.net.URI.create(typeUri));
    }
    if (traceId != null) {
      pd.setProperty("traceId", traceId);
    }
    return pd;
  }
}
