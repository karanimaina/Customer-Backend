## Author
- Name: `FELIX MAINA`
- Email: `karani.maina2010@gmail.com`

# Customer Backend Service

Customer Backend is a Spring Boot WebFlux service that manages customer registration, KYC workflow, and credit profile setup for the lending platform.

## What This Service Does

- Registers new customers.
- Stores and lists KYC documents.
- Submits KYC for review.
- Approves or rejects KYC.
- Initializes customer credit limits and scores on approval.
- Publishes notifications through Spring Cloud Stream (Kafka binder).

## Tech Stack

- Java 17
- Spring Boot
- Spring WebFlux + Spring Data JPA
- H2 (default local DB) / PostgreSQL (optional)
- Spring Cloud Stream (Kafka binder)
- Maven Wrapper (`./mvnw`)

## Project Setup

### 1) Prerequisites

- JDK 17 installed
- Internet connection for first Maven dependency download

Check Java version:

```bash
java -version
```

### 2) Build the service

```bash
./mvnw clean install
```

## Run the Service

Start the app:

```bash
./mvnw spring-boot:run
```

Default local URL:

- Base URL: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

## Configuration

The default `application.yml` uses:

- Port `8081`
- File-based H2 database at `./data/mydb`
- Kafka binder pointing to `localhost:9092`
- Eureka client disabled

### Common environment override

You can override Kafka broker without editing files:

```bash
export KAFKA_BOOTSTRAP_SERVER=localhost:9092
./mvnw spring-boot:run
```

### Switching to PostgreSQL (optional)

Update datasource values in `src/main/resources/application.yml`:

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

## API Endpoints

Base path: `/api/v1/customers`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/register` | Register new customer |
| `GET` | `/{id}` | Get customer by ID |
| `GET` | `/{id}/credit-profile` | Get customer credit profile |
| `POST` | `/{id}/kyc/documents` | Upload KYC document (multipart form data) |
| `GET` | `/{custmerid}/list/kyc/documents` | List KYC documents |
| `POST` | `/{customerid}/kyc/submit` | Submit KYC for review |
| `POST` | `/{customerid}/kyc/approve` | Approve KYC (`X-Reviewer` header optional) |
| `POST` | `/{id}/kyc/reject` | Reject KYC with reason |

## Swagger Endpoints With Sample Requests/Responses

Use Swagger UI at `http://localhost:8081/swagger-ui.html` for interactive execution.

Base URL used in samples:

```text
http://localhost:8081/api/v1/customers
```

### 1) Register Customer

`POST /register`

Sample request:

```bash
curl -X POST "http://localhost:8081/api/v1/customers/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Felix",
    "lastName": "Maina",
    "nationalId": "33221144",
    "email": "karani.maina2010.doe@gmail.com",
    "phoneNumber": "+254795861420",
    "preferredChannel": "EMAIL"
  }'
```

Sample response:

```json
{
  "status": 201,
  "message": "Customer registered successfully",
  "data": {
    "id": 1,
    "firstName": "Felix",
    "lastName": "Maina",
    "nationalId": "33221144",
    "email": "karani.maina2010.doe@gmail.com",
    "phoneNumber": "+254795861420",
    "kycStatus": "PENDING",
    "status": "PENDING",
    "preferredChannel": "EMAIL",
    "registeredAt": "2026-04-27T10:00:00Z",
    "creditProfile": {
      "id": 1,
      "creditLimit": 0.0,
      "outstandingBalance": 0.0,
      "creditScore": 0,
      "risk": "HIGH",
      "tier": "STANDARD",
      "lastEvaluatedAt": "2026-04-27T10:00:00Z"
    }
  }
}
```

### 2) Get Customer By ID

`GET /{id}`

Sample request:

```bash
curl -X GET "http://localhost:8081/api/v1/customers/1"
```

Sample response:

```json
{
  "status": 200,
  "message": "Customer retrieved successfully",
  "data": {
    "id": 1,
    "firstName": "Jane",
    "lastName": "Doe",
    "nationalId": "33221144",
    "email": "jane.doe@example.com",
    "phoneNumber": "+254700123456",
    "kycStatus": "UNDER_REVIEW",
    "status": "KYC_UNDER_REVIEW",
    "preferredChannel": "EMAIL",
    "registeredAt": "2026-04-27T10:00:00Z",
    "creditProfile": {
      "id": 1,
      "creditLimit": 0.0,
      "outstandingBalance": 0.0,
      "creditScore": 0,
      "risk": "HIGH",
      "tier": "STANDARD",
      "lastEvaluatedAt": "2026-04-27T10:00:00Z"
    }
  }
}
```

### 3) Get Credit Profile

`GET /{id}/credit-profile`

Sample request:

```bash
curl -X GET "http://localhost:8081/api/v1/customers/1/credit-profile"
```

Sample response:

```json
{
  "status": 200,
  "message": "Credit profile retrieved successfully",
  "data": {
    "id": 1,
    "creditLimit": 5000.0,
    "outstandingBalance": 0.0,
    "creditScore": 620,
    "risk": "MEDIUM",
    "tier": "STANDARD",
    "lastEvaluatedAt": "2026-04-27T10:10:00Z"
  }
}
```

### 4) Upload KYC Document

`POST /{id}/kyc/documents` (multipart/form-data)

Sample request:

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/documents" \
  -H "Content-Type: multipart/form-data" \
  -F 'documentType=NATIONAL_ID' \
  -F "file=@/absolute/path/to/national-id.png"
```

Sample response:

```json
{
  "status": 201,
  "message": "KYC document uploaded successfully",
  "data": {
    "id": 10,
    "documentType": "NATIONAL_ID",
    "fileReference": "uploads/1/national-id.png",
    "verificationStatus": "PENDING",
    "rejectionReason": null,
    "submittedAt": "2026-04-27T10:05:00Z",
    "verifiedAt": null
  }
}
```

### 5) List KYC Documents

`GET /{custmerid}/list/kyc/documents`

Sample request:

```bash
curl -X GET "http://localhost:8081/api/v1/customers/1/list/kyc/documents"
```

Sample response:

```json
{
  "status": 200,
  "message": "KYC documents retrieved successfully",
  "data": [
    {
      "id": 10,
      "documentType": "NATIONAL_ID",
      "fileReference": "uploads/1/national-id.png",
      "verificationStatus": "PENDING",
      "rejectionReason": null,
      "submittedAt": "2026-04-27T10:05:00Z",
      "verifiedAt": null
    }
  ]
}
```

### 6) Submit KYC For Review

`POST /{customerid}/kyc/submit`

Sample request:

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/submit"
```

Sample response:

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

### 7) Approve KYC

`POST /{customerid}/kyc/approve`

Sample request:

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/approve" \
  -H "X-Reviewer: compliance.officer"
```

Sample response:

```json
{
  "status": 200,
  "message": "KYC approved successfully",
  "data": {
    "id": 1,
    "status": "LOAN_READY",
    "kycStatus": "VERIFIED",
    "creditProfile": {
      "creditLimit": 5000.0,
      "creditScore": 620
    }
  }
}
```

### 8) Reject KYC

`POST /{id}/kyc/reject`

Sample request:

```bash
curl -X POST "http://localhost:8081/api/v1/customers/1/kyc/reject" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Image is unclear. Please upload a clear national ID."
  }'
```

Sample response:

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

### Valid enum values

- `preferredChannel`: `EMAIL`, `SMS`, `PUSH`
- `documentType`: `NATIONAL_ID`, `PASSPORT`, `OTHER`

### Error Response Examples

All errors follow this envelope:

```json
{
  "status": 400,
  "message": "Validation failed",
  "data": {
    "code": "VALIDATION_FAILED",
    "timestamp": "2026-04-27T10:20:00Z",
    "path": "/api/v1/customers/register",
    "fieldViolations": [
      {
        "field": "email",
        "message": "must be a well-formed email address"
      }
    ]
  }
}
```

#### 1) Validation failure (`400 Bad Request`)

Example trigger:

- Missing `firstName` or invalid `email` in `POST /register`

Sample response:

```json
{
  "status": 400,
  "message": "Validation failed",
  "data": {
    "code": "VALIDATION_FAILED",
    "timestamp": "2026-04-27T10:20:00Z",
    "path": "/api/v1/customers/register",
    "fieldViolations": [
      {
        "field": "firstName",
        "message": "must not be blank"
      },
      {
        "field": "email",
        "message": "must be a well-formed email address"
      }
    ]
  }
}
```

#### 2) Customer not found (`404 Not Found`)

Example trigger:

- Calling `GET /api/v1/customers/999999` when the ID does not exist

Sample response:

```json
{
  "status": 404,
  "message": "Customer not found",
  "data": {
    "code": "CUSTOMER_NOT_FOUND",
    "timestamp": "2026-04-27T10:22:00Z",
    "path": "/api/v1/customers/999999"
  }
}
```

#### 3) Invalid KYC state transition (`409 Conflict`)

Example triggers:

- Submitting KYC when customer is not in `PENDING`
- Approving/rejecting KYC when status is not `KYC_UNDER_REVIEW`

Sample response:

```json
{
  "status": 409,
  "message": "KYC is not under review.",
  "data": {
    "code": "CONFLICT",
    "timestamp": "2026-04-27T10:25:00Z",
    "path": "/api/v1/customers/1/kyc/approve"
  }
}
```

#### 4) Duplicate customer details (`400 Bad Request`)

Example triggers:

- Registering with existing `nationalId`, `email`, or `phoneNumber`

Sample response:

```json
{
  "status": 400,
  "message": "Email already registered.",
  "data": {
    "code": "BAD_REQUEST",
    "timestamp": "2026-04-27T10:27:00Z",
    "path": "/api/v1/customers/register"
  }
}
```

#### 5) Unsupported media type (`415 Unsupported Media Type`)

Example trigger:

- Sending JSON instead of multipart data to `POST /{id}/kyc/documents`

Sample response:

```json
{
  "status": 415,
  "message": "Content type 'application/json' not supported",
  "data": {
    "code": "UNSUPPORTED_MEDIA_TYPE",
    "timestamp": "2026-04-27T10:30:00Z",
    "path": "/api/v1/customers/1/kyc/documents"
  }
}
```

## Run Tests

```bash
./mvnw test
```

## Packaging

Create executable jar:

```bash
./mvnw clean package
```

Run jar:

```bash
java -jar target/Customer-Backend-0.0.1-SNAPSHOT.jar
```


