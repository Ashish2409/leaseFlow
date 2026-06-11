# LeaseFlow

A production-grade multi-tenant SaaS platform for residential lease lifecycle management. Covers the full journey from **Prospect → Application → Screening → Approval → Lease Generation → E-Signature → Active Resident → Renewal**.

Built with Spring Boot 3.3.5, Java 21, PostgreSQL 16, and Flyway. Designed as a modular monolith in Phase 1, planned for microservices decomposition in Phase 2.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java (JDK) | 21 | [Eclipse Temurin](https://adoptium.net/) recommended |
| Docker Desktop | Latest | Must be running with WSL2 / Linux engine enabled |
| Git | Any | |

You do **not** need Gradle installed — the project includes the Gradle wrapper (`./gradlew`).

---

## Quick Start (Docker — recommended)

This runs PostgreSQL + the Spring Boot app together in containers.

### 1. Clone and configure

```bash
git clone <repo-url>
cd leaseFlow
cp .env.example .env
```

Open `.env` and set a secure `JWT_SECRET`:

```bash
# Generate a strong secret (requires openssl)
openssl rand -base64 64
```

Paste the output as the value of `JWT_SECRET` in `.env`.

### 2. Build the JAR

```bash
./gradlew bootJar
```

On Windows use `gradlew.bat bootJar` if running outside a bash shell.

### 3. Start the stack

```bash
docker compose up
```

The app starts on **http://localhost:8080** once the postgres health check passes (usually ~20 seconds).

### 4. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Swagger UI: **http://localhost:8080/swagger-ui/index.html**

---

## Local Development (IDE / command line)

For faster iteration, run only PostgreSQL in Docker and the Spring Boot app directly on your machine.

### 1. Start just the database

```bash
docker compose up postgres -d
```

### 2. Run the app

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The `dev` profile connects to `localhost:5432` with the default `leaseflow/leaseflow` credentials that docker-compose sets up.

### 3. Hot reload (IntelliJ IDEA)

1. Import as a Gradle project
2. Set the active profile to `dev` in Run Configuration → VM options: `-Dspring.profiles.active=dev`
3. Run `LeaseFlowApplication`

---

## Environment Variables

| Variable | Required | Default (dev) | Description |
|----------|----------|---------------|-------------|
| `JWT_SECRET` | Yes | See `.env.example` | HS256 signing key — minimum 64 characters |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Use `prod` for production |
| `SPRING_DATASOURCE_URL` | Prod only | `localhost:5432` in dev | Full JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Prod only | `leaseflow` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | Prod only | `leaseflow` | DB password |

Copy `.env.example` to `.env` — it is gitignored and never committed.

---

## Running Tests

```bash
# All tests (unit + integration) with coverage report
./gradlew test

# Integration tests require Docker to pull postgres:16-alpine via Testcontainers
# Make sure Docker Desktop is running before executing tests
```

Coverage report is generated at `build/reports/jacoco/test/html/index.html`. The CI gate enforces **80% minimum** line coverage.

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

Full interactive documentation is available at `/swagger-ui/index.html` when running locally.

### Authentication endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register/tenant` | Public | Register a new property management company + admin user |
| `POST` | `/auth/login` | Public | Login and receive access + refresh tokens |
| `POST` | `/auth/refresh` | Public | Exchange a refresh token for a new token pair |
| `POST` | `/auth/register/user` | Bearer (Admin/Manager) | Add a user to the current tenant |
| `POST` | `/auth/logout` | Bearer | Revoke the current refresh token |

### Token behaviour

- Access tokens expire in **15 minutes**
- Refresh tokens expire in **7 days** and rotate on every use
- Reusing a rotated (consumed) refresh token immediately revokes **all** tokens for that user (theft detection)

### Example: register a tenant

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/tenant \
  -H 'Content-Type: application/json' \
  -d '{
    "companyName": "Acme Properties LLC",
    "adminFirstName": "John",
    "adminLastName": "Smith",
    "adminEmail": "admin@acme.com",
    "adminPassword": "SecurePass1"
  }'
```

### Example: login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@acme.com",
    "password": "SecurePass1"
  }'
```

All responses use the envelope format:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-06-11T18:00:00Z"
}
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/abm/leaseFlow/
│   │   ├── LeaseFlowApplication.java
│   │   ├── common/
│   │   │   ├── audit/          # AuditService, AuditLog, AuditEvent
│   │   │   ├── config/         # SecurityConfig, JpaConfig, OpenApiConfig
│   │   │   ├── dto/            # ApiResponse envelope
│   │   │   ├── entity/         # BaseEntity (UUID PK, tenantId, soft-delete)
│   │   │   ├── exception/      # GlobalExceptionHandler + typed exceptions
│   │   │   ├── filter/         # CorrelationIdFilter
│   │   │   └── security/       # JwtTokenProvider, JwtAuthFilter, TenantContextHolder
│   │   ├── auth/
│   │   │   ├── controller/     # AuthController
│   │   │   ├── dto/            # Request / response DTOs
│   │   │   ├── entity/         # RefreshToken
│   │   │   ├── repository/
│   │   │   └── service/        # AuthService
│   │   ├── tenant/
│   │   │   ├── entity/         # Tenant (SubscriptionPlan, TenantStatus)
│   │   │   └── repository/
│   │   └── user/
│   │       ├── entity/         # User, Role, RoleName
│   │       └── repository/
│   └── resources/
│       ├── application.properties          # Shared / base config
│       ├── application-dev.properties      # Local dev overrides
│       ├── application-prod.properties     # Production (reads from env vars)
│       ├── logback-spring.xml              # Human-readable (dev) / JSON (prod)
│       └── db/migration/
│           ├── V1__create_core_schema.sql  # tenants, users, roles, refresh_tokens, audit_logs
│           └── V2__create_business_schema.sql  # properties, units, leases, signatures, ...
└── test/
    └── java/com/abm/leaseFlow/
        ├── common/BaseIntegrationTest.java  # Shared Testcontainers setup
        └── auth/AuthIntegrationTest.java    # 10 ordered auth flow tests
```

---

## Multi-tenancy

Every business entity carries a `tenant_id` column. The `TenantContextHolder` (thread-local) is populated from the JWT on each request by `JwtAuthenticationFilter`. All queries should filter by tenant — never cross-tenant data access is possible through the API layer.

---

## Roles

| Role | Description |
|------|-------------|
| `ROLE_PLATFORM_ADMIN` | Created automatically when a tenant registers. Full access within their tenant. |
| `ROLE_PROPERTY_MANAGER` | Manages properties, units, and leasing staff. |
| `ROLE_LEASING_AGENT` | Handles prospects, applications, and lease drafts. |
| `ROLE_RESIDENT` | Read-only access to their own lease and documents. |

---

## Docker commands reference

```bash
# Start everything (requires JAR to be built first)
./gradlew bootJar && docker compose up

# Start only the database (for local dev)
docker compose up postgres -d

# Rebuild app image after code changes
docker compose up --build

# Stop and remove containers
docker compose down

# Stop and remove containers + database volume (full reset)
docker compose down -v

# View logs
docker compose logs -f app
docker compose logs -f postgres
```

---

## CI / CD

GitHub Actions runs on every push and pull request to `main` / `develop`:

1. Compile
2. Unit tests
3. Integration tests (Testcontainers spins up PostgreSQL automatically)
4. Jacoco coverage gate (80% minimum)
5. Docker build smoke test

Workflow file: `.github/workflows/ci.yml`
