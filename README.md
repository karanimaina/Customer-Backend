# Customer Backend Service

Customer Backend is a Spring Boot WebFlux service for customer registration, KYC processing, and credit profile initialization.

## Table of Contents
- Setup
- Architecture and Process Flow
- Entity Mapping
- API Endpoints
- Sample Requests and Responses
- Swagger / OpenAPI
- Run and Verify

## Setup

### Prerequisites
- JDK 17
- Internet connection for first Maven dependency download

### Build
```bash
./mvnw clean install
```

### Default configuration
From `src/main/resources/application.yml`:
- Port: `8081`
- DB: file-based H2 at `./data/mydb`
- Kafka broker: `${KAFKA_BOOTSTRAP_SERVER:localhost:9092}`
- Eureka client: disabled

### Optional PostgreSQL configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/customer
    username: customer
    password: customer
    driver-class-name: org.postgresql.Driver
```

Keep:
- `spring.jpa.hibernate.ddl-auto: update`

## Architecture and Process Flow

### Process flow
1. Register customer -> status `PENDING`.
2. Upload KYC documents.
3. Submit KYC -> status `KYC_UNDER_REVIEW`.
4. Review outcome:
   - Approve -> status `LOAN_READY`, KYC `VERIFIED`, credit initialized.
   - Reject -> status `KYC_REJECTED`, KYC `REJECTED`.

### Notes
- API layer uses Spring WebFlux.
- Persistence uses Spring Data JPA.
- Notifications publish through Spring Cloud Stream with Kafka binder.

## Entity Mapping

Core entities in this service:
- `Customer`
- `KycDocument`
- `CreditProfile`
- `CreditHistory`
- `CustomerSegment`
- `PaymentSummary`
- `LoanLimitAdjustment`

Main enums:
- `PreferredChannel`: `EMAIL`, `SMS`, `PUSH`
- `KycDocumentType`: `NATIONAL_ID`, `PASSPORT`, `OTHER`
- `CustomerStatus`: `PENDING`, `KYC_UNDER_REVIEW`, `KYC_REJECTED`, `LOAN_READY`, `SUSPENDED`, `CLOSED`

## API Endpoints

Base URL: `http://localhost:8081/api/v1/customers`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/register` | Register a new customer |
| `GET` | `/{id}` | Get customer details |
| `GET` | `/{id}/credit-profile` | Get credit profile |
| `POST` | `/{id}/kyc/documents` | Upload KYC document (multipart/form-data) |
| `GET` | `/{custmerid}/list/kyc/documents` | List KYC documents |
| `POST` | `/{customerid}/kyc/submit` | Submit KYC for review |
| `POST` | `/{customerid}/kyc/approve` | Approve KYC (`X-Reviewer` optional) |
| `POST` | `/{id}/kyc/reject` | Reject KYC with reason |

## Sample Requests and Responses

### Register customer
`POST /register`

```bash
curl -X POST "http://localhost:8081/api/v1/customers/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Felix",
    "lastName": "Maina",
    "nationalId": "33221144",
    "email": "karani.maina2010@gmail.com",
    "phoneNumber": "+254795861420",
    "preferredChannel": "EMAIL"
  }'
```

```json
{
  "status": 201,
  "message": "Customer registered successfully",
  "data": {
    "id": 1,
    "firstName": "Felix",
    "lastName": "Maina",
    "status": "PENDING",
    "kycStatus": "PENDING"
  }
}
```

### Upload KYC document
`POST /{id}/kyc/documents`

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/documents" \
  -H "Content-Type: multipart/form-data" \
  -F 'documentType=NATIONAL_ID' \
  -F "file=@/absolute/path/to/national-id.png"
```

```json
{
  "status": 201,
  "message": "KYC document uploaded successfully",
  "data": {
    "id": 10,
    "documentType": "NATIONAL_ID",
    "verificationStatus": "PENDING"
  }
}
```

### Submit KYC
`POST /{customerid}/kyc/submit`

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/submit"
```

```json
{
  "status": 200,
  "message": "KYC submitted for review successfully",
  "data": {
    "id": 1,
    "status": "KYC_UNDER_REVIEW",
    "kycStatus": "UNDER_REVIEW"
  }
}
```

### Approve KYC
`POST /{customerid}/kyc/approve`

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/approve" \
  -H "X-Reviewer: compliance.officer"
```

```json
{
  "status": 200,
  "message": "KYC approved successfully",
  "data": {
    "id": 1,
    "status": "LOAN_READY",
    "kycStatus": "VERIFIED"
  }
}
```

### Reject KYC
`POST /{id}/kyc/reject`

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/reject" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Image is unclear. Please upload a clear national ID."}'
```

```json
{
  "status": 200,
  "message": "KYC rejected successfully",
  "data": {
    "id": 1,
    "status": "KYC_REJECTED",
    "kycStatus": "REJECTED"
  }
}
```

### Common error response examples

Validation failure (`400`):
```json
{
  "status": 400,
  "message": "Validation failed",
  "data": {
    "code": "VALIDATION_FAILED",
    "path": "/api/v1/customers/register",
    "fieldViolations": [
      { "field": "email", "message": "must be a well-formed email address" }
    ]
  }
}
```

Customer not found (`404`):
```json
{
  "status": 404,
  "message": "Customer not found",
  "data": {
    "code": "CUSTOMER_NOT_FOUND",
    "path": "/api/v1/customers/999999"
  }
}
```

Invalid KYC state (`409`):
```json
{
  "status": 409,
  "message": "KYC is not under review.",
  "data": {
    "code": "CONFLICT",
    "path": "/api/v1/customers/1/kyc/approve"
  }
}
```

## Swagger / OpenAPI

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Alternate UI path: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Run and Verify

### Run service
```bash
./mvnw spring-boot:run
```

### Verify health and docs
```bash
curl -X GET "http://localhost:8081/actuator/health"
curl -X GET "http://localhost:8081/v3/api-docs"
```

### Run tests
```bash
./mvnw test
```

### Build and run jar
```bash
./mvnw clean package
java -jar target/Customer-Backend-0.0.1-SNAPSHOT.jar
```
