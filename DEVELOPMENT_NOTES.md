# Development Notes

## Overall approach

I built the service top-down from the brief, treating each numbered section as
a single responsibility. The pipeline is composed in a thin orchestrator
(`DefaultLoanEvaluationService`) that delegates each step to a focused
collaborator:

```
request → validation → eligibility (pre-EMI) → risk band → rate
       → EMI → eligibility (EMI affordability) → decision → audit
```

Each collaborator implements a small interface (`EmiCalculator`,
`RiskBandClassifier`, `InterestRateCalculator`, `EligibilityEvaluator`). The
orchestrator owns no business rules of its own — it only sequences the steps
and persists the audit record.

The HTTP layer is intentionally thin: validation is declarative
(Bean Validation on the request records), failures map to `400` via a
`@RestControllerAdvice`, and the controller never throws domain exceptions.

## Key design decisions

1. **Externalised configuration via `@ConfigurationProperties`.** Every rate,
   premium and threshold lives in `application.yml` under `lending.*`. The
   business logic reads from a typed `LendingProperties` record, so changing a
   premium is a config edit, not a code change. Tests reuse a
   `TestLendingProperties.defaults()` factory so production and test paths
   exercise identical numbers.

2. **Java records for DTOs and immutable domain models.** Records give
   compact, immutable carriers with built-in `equals`/`hashCode`/`toString`.
   The `Decision` and `Offer` types are pure value objects — JPA stays inside
   `LoanApplicationRecord`, the only mutable persistence-aware class.

3. **Interfaces + impl packages for each rule.** This keeps the strategy
   pattern explicit: a future "machine-learning risk band" classifier or a
   different rate model can drop in without touching the orchestrator.

4. **EMI math in `MathContext.DECIMAL64` then rounded once.** The brief asks
   for `scale=2, HALF_UP`. Doing every intermediate step at scale 2 would
   accumulate rounding error. Using `DECIMAL64` (16-digit) and rounding only
   the final EMI gives results that match standard EMI calculators.

5. **Order of rule evaluation.** Pre-EMI rules (credit-score floor and
   age+tenure limit) run first. If `LOW_CREDIT_SCORE` fires we short-circuit
   — no risk band exists, so a rate cannot be computed. Otherwise we still
   compute the EMI (even if `AGE_TENURE_LIMIT_EXCEEDED` already fired) and
   accumulate any EMI-affordability reasons, matching the example response in
   the brief that lists multiple reasons together.

6. **EMI ratio rules collapsed to a single reason.** EMI > 60% strictly
   implies EMI > 50%, so reporting both would be redundant. The evaluator
   returns the strictest matching reason
   (`EMI_EXCEEDS_60_PERCENT` else `EMI_EXCEEDS_50_PERCENT`).

7. **`age*12 + tenureMonths > 65*12` instead of floating-point years.**
   Avoids rounding ambiguity at the boundary; e.g. age 60 with a 60-month
   tenure is exactly at the limit (passes), while 84 months exceeds (fails).

8. **Audit storage in a single denormalised table.** The brief asks for
   "Stores decisions for audit". One row per decision, capturing both inputs
   and outputs, is the simplest auditable form. Re-evaluating later does not
   rely on joining tables. The table is created via Hibernate `ddl-auto: update`
   for the H2 in-memory database; in production this would move to Flyway/
   Liquibase migrations.

9. **`@JsonInclude(NON_NULL)` on the response.** The rejected example in the
   brief omits the `offer` field entirely (and shows `riskBand: null`). The
   response record carries `offer`/`riskBand`/`rejectionReasons` as nullable
   fields and Jackson hides the nulls — the approved response has no
   `rejectionReasons` key, the rejected response has no `offer` key, and so on.

10. **Swagger UI bundled.** springdoc-openapi exposes a live, interactive UI
    at `/swagger-ui.html` plus the raw OpenAPI document at `/v3/api-docs`. The
    controller is annotated with example payloads for both approved and
    rejected outcomes so reviewers can click "Try it out" and see realistic
    responses.

11. **Production readiness via Spring Boot Actuator + Micrometer.**
    - Health endpoints split into `liveness` and `readiness` so Kubernetes
      probes can be wired without overlap.
    - `/actuator/info` is populated by the Maven plugin's `build-info` goal so
      a deployed instance reports the exact artifact and build time.
    - Domain counters (`loan.decisions`, `loan.rejections`, `loan.amount`,
      `loan.emi.ratio`, `loan.risk.classified`) are recorded after every
      evaluation so dashboards and alerts can track approval-rate drift,
      rejection-reason breakdowns and EMI-affordability distribution without
      parsing logs. Metrics are tagged with `application` and emitted in
      Prometheus format.
    - HTTP latency SLO histograms (50/100/200/500/1000 ms buckets) are
      published on `http.server.requests` for percentiles.
    - `CorrelationIdFilter` accepts an `X-Correlation-Id` header (or generates
      a UUID when absent), echoes it on the response and stamps it into
      SLF4J's MDC so every log line during a request carries the same id —
      this turns a forest of logs back into a request-keyed timeline.

12. **Structured, branchable error responses.** The `ErrorResponse` shape
    carries a stable, machine-readable `errorCode` enum that clients should
    branch on, plus `traceId`, `method` and `rejectedValue` fields so
    operators can correlate a 4xx/5xx response with the matching log line.
    Field-level errors expose what was rejected, not just why.

## Trade-offs

- **In-memory H2 vs a real RDBMS.** H2 keeps the project self-contained and
  the smoke tests reproducible, but the audit log evaporates on restart. A
  production deployment would point the same JPA layer at PostgreSQL and add
  Flyway migrations.

- **Single-row audit vs separate `application` and `decision` tables.** A
  normalised schema would model the original application separately from each
  evaluation event. I chose the simpler one-row form because the brief's
  decision is final (no re-evaluation flow) and a single row is easier to
  inspect during review.

- **Synchronous evaluation in the controller thread.** Acceptable here because
  the math is bounded and the persistence is local. A real platform would
  publish an event after persisting and let downstream consumers (notifications,
  KYC, fraud screening) react asynchronously.

- **No authentication.** Out of scope for the brief; in production this would
  sit behind an API gateway with mTLS or JWT.

## Assumptions

- "Loan amount must be between 10,000 – 50,00,000" is read in the Indian
  numbering convention as **10,000–5,000,000 INR**.
- "Reject if EMI > 60%" uses a strict greater-than; EMI exactly equal to 60%
  is acceptable. Likewise for the 50% offer rule.
- "Age + tenure (in years) > 65" is enforced as `age*12 + tenureMonths > 780`,
  treating `tenureMonths/12` as a real number rather than rounding.
- A credit score below 600 short-circuits to rejection without computing the
  EMI; this avoids assigning an arbitrary band to a sub-prime applicant.
- `creditScore: 300–900` is a hard validation range; out-of-range scores fail
  validation rather than auto-rejecting.

## Improvements with more time

- **Migrations.** Flyway scripts under `src/main/resources/db/migration` so
  the schema is versioned independent of Hibernate's `ddl-auto`.
- **Idempotency.** Accept an `Idempotency-Key` header so retried requests
  reuse the same `applicationId` and decision row.
- **Property-based tests** for the EMI calculator (e.g. with jqwik) to verify
  invariants like "increasing tenure decreases EMI" across random valid inputs.
- **Audit-trail endpoint.** `GET /applications/{id}` returning the stored
  decision; useful for a real lending UX.
- **Observability.** Micrometer counters for approve/reject outcomes, EMI
  percentile histograms, and request tracing via Spring's `Observation` API.
- **Containerisation.** A multi-stage Dockerfile and a `docker-compose.yml`
  with PostgreSQL for local parity with production.
- **Contract tests** with Spring Cloud Contract so consumers can rely on the
  schema without coupling to the runtime.
