# Loan Evaluation Service

A Spring Boot REST service that evaluates loan applications, generates a single
offer based on the requested tenure and stores every decision for audit.

The implementation follows the brief in `Java_Backend_TakeHome_Assignment (1).pdf`.

## Tech stack

| Layer       | Choice                                |
|-------------|---------------------------------------|
| Language    | Java 21 (compatible with Java 25 JDK) |
| Framework   | Spring Boot 3.4                       |
| Persistence | Spring Data JPA + H2 (in-memory)      |
| Validation  | Jakarta Bean Validation               |
| Docs        | springdoc-openapi (Swagger UI)        |
| Tests       | JUnit 5, AssertJ, Mockito, MockMvc    |
| Build       | Maven 3.9                             |

## Quick start

```bash
mvn test            # run all unit + web tests
mvn spring-boot:run # boot the service on http://localhost:8080
```

When the service is running:

| Resource              | URL                                              |
|-----------------------|--------------------------------------------------|
| Swagger UI            | http://localhost:8080/swagger-ui.html            |
| OpenAPI JSON          | http://localhost:8080/v3/api-docs                |
| H2 Console            | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:loandb`, user `sa`, no password) |
| Actuator (health)     | http://localhost:8080/actuator/health            |
| Actuator (liveness)   | http://localhost:8080/actuator/health/liveness   |
| Actuator (readiness)  | http://localhost:8080/actuator/health/readiness  |
| Actuator (info)       | http://localhost:8080/actuator/info              |
| Actuator (metrics)    | http://localhost:8080/actuator/metrics           |
| Prometheus scrape     | http://localhost:8080/actuator/prometheus        |

## API

### `POST /applications`

Submit a loan application; receive the decision and (if approved) an offer.
Every decision is persisted to the `loan_application_record` table for audit.

**Request**

```json
{
  "applicant": {
    "name": "Asha",
    "age": 30,
    "monthlyIncome": 75000,
    "employmentType": "SALARIED",
    "creditScore": 780
  },
  "loan": {
    "amount": 500000,
    "tenureMonths": 36,
    "purpose": "PERSONAL"
  }
}
```

**Approved response — `201 Created`**

```json
{
  "applicationId": "6267b814-5542-482c-93dd-fe310a0fc1ab",
  "status": "APPROVED",
  "riskBand": "LOW",
  "offer": {
    "interestRate": 12.00,
    "tenureMonths": 36,
    "emi": 16607.15,
    "totalPayable": 597857.40
  }
}
```

**Rejected response — `201 Created`**

```json
{
  "applicationId": "e8f947d5-2032-4c98-93f7-8b3f829b49ed",
  "status": "REJECTED",
  "rejectionReasons": ["LOW_CREDIT_SCORE"]
}
```

**Validation error — `400 Bad Request`**

```json
{
  "timestamp": "2026-05-08T07:12:16.393526Z",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_FAILED",
  "message": "Validation failed for one or more fields",
  "path": "/applications",
  "method": "POST",
  "traceId": "8e34a2c1-7b9d-4f0a-9c1e-2f5a6b7c8d9e",
  "fieldErrors": [
    {"field": "applicant.age", "rejectedValue": 18, "message": "applicant.age must be between 21 and 60"}
  ]
}
```

### Error model

Every error response follows the same `ErrorResponse` shape:

| Field          | Description                                            |
|----------------|--------------------------------------------------------|
| `timestamp`    | When the error was produced (UTC)                      |
| `status`       | HTTP status code                                       |
| `error`        | HTTP reason phrase                                     |
| `errorCode`    | Stable application code (branch on this, not `message`)|
| `message`      | Human-readable description                             |
| `path`         | Request path                                           |
| `method`       | HTTP method                                            |
| `traceId`      | UUID for log correlation                               |
| `fieldErrors`  | Per-field detail (only on validation failures)         |

| `errorCode`            | HTTP | Meaning                                            |
|------------------------|------|----------------------------------------------------|
| `VALIDATION_FAILED`    | 400  | One or more body fields failed Bean Validation     |
| `CONSTRAINT_VIOLATION` | 400  | Path/query parameter constraint violation          |
| `MALFORMED_REQUEST`    | 400  | Body is not valid JSON                             |
| `INVALID_ENUM_VALUE`   | 400  | Enum field received an unknown value               |
| `MISSING_PARAMETER`    | 400  | A required request parameter is absent             |
| `TYPE_MISMATCH`        | 400  | A parameter could not be converted to its type     |
| `RESOURCE_NOT_FOUND`   | 404  | Path is not mapped to any endpoint                 |
| `ILLEGAL_ARGUMENT`     | 400  | Domain rejected an argument                        |
| `INTERNAL_ERROR`       | 500  | Unexpected exception (the `traceId` is in the logs)|

### Validation rules

| Field                     | Rule                          |
|---------------------------|-------------------------------|
| `applicant.name`          | Non-blank                     |
| `applicant.age`           | 21–60 (inclusive)             |
| `applicant.monthlyIncome` | Positive                      |
| `applicant.employmentType`| `SALARIED` \| `SELF_EMPLOYED` |
| `applicant.creditScore`   | 300–900 (inclusive)           |
| `loan.amount`             | 10,000–50,00,000              |
| `loan.tenureMonths`       | 6–360                         |
| `loan.purpose`            | `PERSONAL` \| `HOME` \| `AUTO`|

### Business rules

| Rule                                               | Source                       |
|----------------------------------------------------|------------------------------|
| Reject if `creditScore < 600`                      | `LOW_CREDIT_SCORE`           |
| Reject if `age + tenureYears > 65`                 | `AGE_TENURE_LIMIT_EXCEEDED`  |
| Reject if `EMI > 60% × monthlyIncome`              | `EMI_EXCEEDS_60_PERCENT`     |
| Reject if `EMI > 50% × monthlyIncome` (offer rule) | `EMI_EXCEEDS_50_PERCENT`     |

### Risk bands & rate stack

| Score    | Band   | Risk premium | + Salaried | + Self-employed | + Loan > 10L |
|----------|--------|--------------|------------|-----------------|--------------|
| 750+     | LOW    | +0%          | +0%        | +1%             | +0.5%        |
| 650–749  | MEDIUM | +1.5%        | +0%        | +1%             | +0.5%        |
| 600–649  | HIGH   | +3%          | +0%        | +1%             | +0.5%        |

Final rate = `12% (base) + risk + employment + loan-size`.

### EMI

```
EMI = P × r × (1+r)^n / ((1+r)^n − 1)
```

where `r` is the monthly interest rate and `n` is `tenureMonths`. Computations
use `BigDecimal` with `MathContext.DECIMAL64`; the result is rounded to scale
`2`, `RoundingMode.HALF_UP`.

## Project layout

```
src/main/java/com/rbihub/loan
├── LoanEvaluationApplication.java
├── config/         # @ConfigurationProperties, OpenAPI metadata
├── controller/     # HTTP layer
├── dto/            # Request/response records (no business logic)
│   ├── request/
│   └── response/
├── domain/         # Pure domain (entities, immutable records, enums)
│   ├── enums/
│   └── model/
├── exception/      # @RestControllerAdvice
├── repository/     # Spring Data JPA
└── service/        # Business rules
    └── impl/       # Default implementations of each service interface
```

## Configuration

All thresholds are externalised via `@ConfigurationProperties` (`lending.*`)
in `application.yml`. To experiment with different rates or floors, change the
config — no code edits are required.

## Production readiness

| Concern              | How it is addressed                                                                                             |
|----------------------|-----------------------------------------------------------------------------------------------------------------|
| Health checks        | `/actuator/health` (aggregate), `/actuator/health/liveness`, `/actuator/health/readiness` for K8s probes         |
| Build provenance     | `spring-boot-maven-plugin` `build-info` goal exposes artifact, version and build time at `/actuator/info`        |
| Metrics              | Micrometer with Prometheus scrape at `/actuator/prometheus`; HTTP latency SLO histograms (50/100/200/500/1000ms) |
| Domain metrics       | `loan.decisions{status,riskBand}`, `loan.rejections{reason}`, `loan.amount{status}`, `loan.emi.ratio{riskBand}`, `loan.risk.classified{band}` |
| Log correlation      | `CorrelationIdFilter` accepts/echoes `X-Correlation-Id` and stamps SLF4J MDC; logs include `[correlationId]`     |
| Graceful shutdown    | `server.shutdown=graceful` so in-flight requests complete on `SIGTERM`                                           |
| Structured errors    | `@RestControllerAdvice` returns the same `ErrorResponse` shape with stable `errorCode` for every failure         |
| Audit trail          | Every decision (approved or rejected) is persisted to `LOAN_APPLICATION_RECORD`                                  |
| Configuration safety | `LendingProperties` is `@Validated`; missing premium maps fail fast at startup                                   |
