package io.paymentcollection.payment.application;

import io.paymentcollection.payment.domain.IdempotencyRecord;
import io.paymentcollection.payment.domain.Payment;
import io.paymentcollection.payment.infrastructure.persistence.JpaIdempotencyRepository;
import io.paymentcollection.payment.infrastructure.persistence.JpaPaymentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreatePaymentHandler {

  private final JpaPaymentRepository payments;
  private final JpaIdempotencyRepository idemRepo;

  public CreatePaymentHandler(JpaPaymentRepository payments, JpaIdempotencyRepository idemRepo) {
    this.payments = payments;
    this.idemRepo = idemRepo;
  }

  @Transactional
  public Result handle(Command c) {
    if (c.idempotencyKey() == null || c.idempotencyKey().isBlank()) {
      return Result.error(HttpStatus.BAD_REQUEST, "Missing Idempotency-Key header");
    }

    String reqHash = sha256(c.canonicalJson());

    // 1) Pre-check by key (avoid unique-constraint exception)
    var existingOpt = idemRepo.findByIdemKey(c.idempotencyKey());
    if (existingOpt.isPresent()) {
      var rec = existingOpt.get();

      // Same key, different body → 409
      if (!reqHash.equals(rec.getRequestHash())) {
        return Result.error(
            HttpStatus.CONFLICT, "Idempotency-Key conflict: different request body");
      }

      // Same key+body and we already have a payment → replay (200)
      if (rec.getPaymentId() != null) {
        var existing = payments.findById(rec.getPaymentId()).orElseThrow();
        return Result.replayed(existing);
      }

      // Same key+body but still in-flight (no payment yet)
      return Result.error(
          HttpStatus.CONFLICT, "Request with this Idempotency-Key is still being processed");
    }

    // 2) Not present → create idempotency record
    var rec = new IdempotencyRecord();
    rec.setIdemKey(c.idempotencyKey());
    rec.setRequestHash(reqHash);
    rec = idemRepo.save(rec); // no flush needed

    // 3) Create payment
    var p = new Payment();
    p.setAmount(c.amount());
    p.setCurrency(c.currency());
    p.setMethod(c.method());
    p.setCustomerId(c.customerId()); // entity uses String
    p.setStatus("CREATED"); // satisfy @NotBlank
    p = payments.save(p);

    // 4) Link and complete idempotency record
    rec.setPaymentId(p.getId());
    rec.setStatus("COMPLETED");
    idemRepo.save(rec);

    return Result.created(p);
  }

  private String sha256(String s) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ===== Use-case DTOs =====
  public record Command(
      java.math.BigDecimal amount,
      String currency,
      String method,
      String customerId,
      String description,
      String idempotencyKey,
      String canonicalJson) {}

  public record Result(org.springframework.http.HttpStatus status, Payment payment, String error) {
    public static Result created(Payment p) {
      return new Result(HttpStatus.CREATED, p, null);
    }

    public static Result replayed(Payment p) {
      return new Result(HttpStatus.OK, p, null);
    }

    public static Result error(HttpStatus s, String msg) {
      return new Result(s, null, msg);
    }
  }
}
