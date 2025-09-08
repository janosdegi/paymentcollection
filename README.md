# Payment Collection

Spring Boot project with Maven and CI.

## Requirements
- JDK 21 (required for build & run)
- Docker & Docker Compose (for PostgreSQL, Elasticsearch, Kibana in local dev)
- Maven Wrapper (`./mvnw` is included, no need to install Maven manually)

## Build & Run

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

## Local Development (Docker Compose)

### Start services

```bash
docker compose up -d
```

### Stop services

```bash
docker compose down
```

### Run the app (local profile)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Code Style

- Formatting enforced by **Spotless** (Google Java Format).
- Style checks by **Checkstyle** (Google rules).
- CI runs `./mvnw clean verify` and will fail on violations.
- Optional: enable local pre-commit hook:
```bash
.git/hooks/pre-commit
```
- Before commit
```bash
./mvnw spotless:apply checkstyle:check
```
```bash
./mvnw clean verify
```

## Database Schema

- Managed by **Flyway** migrations in `src/main/resources/db/migration`.
- Initial schema (`V2__add_payment_indexes.sql`) creates the `payments` table with:
    - id, amount, currency, status, method, providerRef, customerId, timestamps, metadata
- JPA entity: `io.paymentcollection.payment.domain.Payment`

## Error Handling (RFC 7807)

We use RFC 7807 (application/problem+json) with a global exception 
handler so that all errors in the API return a consistent, 
machine-readable format (type, title, status, detail, traceId, and 
validation errors[]).

All errors return `application/problem+json` with:
- `type`, `title`, `status`, `detail`, `traceId`
- Validation adds `errors[]` with `{field, message}`

Clients should log and/or display `detail` and include `X-Trace-Id` when reporting issues.

## POST /api/payments (Idempotent)

- Requires `Idempotency-Key` header.
- First request → `201 Created` + `Location`.
- Retry with same key + same body → `200 OK` (returns existing).
- Same key + different body → `409 Conflict`.
- Validation: `amount > 0`, supported `currency/method`, `customerId` required.
- Errors are `application/problem+json` (RFC 7807).