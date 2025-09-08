package io.paymentcollection.payment.api.error;

import org.springframework.web.bind.annotation.*;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */

@RestController
@RequestMapping("/__test")
class ErrorFixtureController {

    record CreateReq(@jakarta.validation.constraints.Email String email,
                     @jakarta.validation.constraints.Min(18) Integer age) {}

    @PostMapping("/validate")
    public String validate(@RequestBody @jakarta.validation.Valid CreateReq req) { return "ok"; }

    @GetMapping("/not-found")
    public String notFound() { throw new java.util.NoSuchElementException("User 42 not found"); }

    @GetMapping("/boom")
    public String boom() { throw new RuntimeException("kaboom"); }
}
