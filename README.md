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

