# Customer-Backend

Spring Boot service for the **customer profile** slice of the lending case study: self-registration, KYC, credit profile initialization, loan-limit adjustments, and event-driven notifications. Persistence follows the provided **Customer ERD**; behaviour follows the **customer-service flowchart** (PENDING → KYC review → LOAN_READY / REJECTED).

## Prerequisites

- Java 17+ (project uses Java 17; CI may run on newer JDKs)
- Maven (wrapper included: `./mvnw`)

## Run locally

Default configuration uses an **in-memory H2** database and disables Eureka. No external broker is required; notifications are logged unless Kafka is enabled.

```bash
./mvnw spring-boot:run
```

API base URL: `http://localhost:8080` (WebFlux). OpenAPI UI (if enabled): `/swagger-ui.html`.

### PostgreSQL

Set standard Spring DataSource properties and keep `spring.jpa.hibernate.ddl-auto=update` so the schema is created/updated on startup (case study requirement). Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/customer
spring.datasource.username=customer
spring.datasource.password=customer
spring.jpa.hibernate.ddl-auto=update
```

### Kafka notifications (optional)

```properties
customer.notifications.kafka.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
customer.notifications.kafka.topic=customer-notifications
```

When disabled, `LoggingCustomerNotificationPublisher` records notification events.

## REST API (customer flow)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/customers` | Self-registration; validates national ID, email, phone; status `PENDING`; credit score **0** |
| `GET` | `/api/v1/customers/{id}` | Customer + embedded credit profile |
| `GET` | `/api/v1/customers/{id}/credit-profile` | Credit profile |
| `POST` | `/api/v1/customers/{id}/kyc/documents` | Upload KYC metadata (e.g. national ID file reference) |
| `GET` | `/api/v1/customers/{id}/kyc/documents` | List KYC documents |
| `POST` | `/api/v1/customers/{id}/kyc/submit` | Move to `KYC_UNDER_REVIEW` (requires national ID document) |
| `POST` | `/api/v1/customers/{id}/kyc/approve` | Approve KYC; initialise credit limit/score; tier **STANDARD**; status `LOAN_READY` |
| `POST` | `/api/v1/customers/{id}/kyc/reject` | Reject with reason; status `KYC_REJECTED` |

Approve endpoint accepts optional header `X-Reviewer` (defaults to `system`).

Configurable approval defaults:

- `customer.kyc.approved-credit-limit` (default `5000`)
- `customer.kyc.approved-credit-score` (default `620`)

## Architecture notes

- **Domain model** maps to the ERD: `Customer`, `KycDocument`, `CreditProfile`, `CreditHistory`, `CustomerSegment`, `PaymentSummary`, `LoanLimitAdjustment`.
- **Flowchart**: registration validation → `PENDING` + score 0 → document upload → submit for review → approve/reject → notifications on each transition.
- **REST + WebFlux**: JPA calls run on `Schedulers.boundedElastic()` from controllers.
- **Testing**: `./mvnw test` (includes Mockito unit tests and a Spring context smoke test).

## Case study alignment

Covers the **Customer Profile Module** and **Notification** aspects (event publishing). Product catalogue, loan lifecycle, and sweep jobs belong in separate services but can consume the same customer/credit data via APIs and Kafka topics.
