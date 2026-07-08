# Scoring Service (demo)

A small Spring Boot REST service that scores a credit application using a
**rule-based** scoring model and returns a score, a decision, the model
version, and the list of factors that drove the decision.

> **This is a demo/educational project.** It does not use real banking or
> credit data, does not connect to any real systems, and its model is a
> hand-written heuristic, not a trained or validated statistical/ML model.
> It must **not** be used to make real credit decisions. See
> [Limitations](#limitations) below.

## Contents

- [Running the service](#running-the-service)
- [API](#api)
- [Input features](#input-features)
- [Scoring rules (model `rule-based-v1`)](#scoring-rules-model-rule-based-v1)
- [Sample requests](#sample-requests)
- [Architecture](#architecture)
- [Testing](#testing)
- [Limitations](#limitations)
- [Migration path to ML / ONNX / an external model service](#migration-path-to-ml--onnx--an-external-model-service)

## Running the service

Prerequisites: JDK 17+ and Maven (or use the wrapper if you add one).

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

> **JDK note:** the test suite uses Mockito's inline mock maker, which
> depends on ByteBuddy for bytecode instrumentation. As of this writing
> ByteBuddy does not yet support very new JDKs (e.g. JDK 26). If `mvn test`
> fails with a ByteBuddy/`IllegalArgumentException: Java NN ... is not
> supported` error, point `JAVA_HOME` at a JDK 17-21 before running Maven,
> e.g.:
> ```bash
> export JAVA_HOME=$(/usr/libexec/java_home -v 21)
> mvn test
> ```

Build a runnable jar:

```bash
mvn clean package
java -jar target/scoring-service-0.1.0.jar
```

### Example request

```bash
curl -s -X POST http://localhost:8080/api/v1/scoring \
  -H "Content-Type: application/json" \
  -d @samples/approved-request.json | python3 -m json.tool
```

## API

### `POST /api/v1/scoring`

**Request body**

| Field                | Type    | Format constraints              |
|----------------------|---------|----------------------------------|
| `requestId`          | string  | not blank                        |
| `clientAge`          | integer | 18-100                           |
| `monthlyIncome`      | decimal | > 0                               |
| `requestedAmount`    | decimal | > 0                               |
| `employmentMonths`   | integer | >= 0                              |
| `hasOverduePayments` | boolean | required                          |
| `activeLoansCount`   | integer | 0-50                              |

**Response body (200 OK)**

```json
{
  "requestId": "req-approve-1",
  "score": 100,
  "decision": "APPROVED",
  "modelVersion": "rule-based-v1",
  "factors": [
    {
      "code": "AGE_IN_OPTIMAL_RANGE",
      "description": "Client age 30 is within the 25-55 range considered lower risk",
      "impact": 10
    }
  ]
}
```

`decision` is one of `APPROVED`, `MANUAL_REVIEW`, `REJECTED`.

**Validation error responses (400 Bad Request)**

Both format errors (bad types/ranges on the request DTO, checked with Bean
Validation) and business-logic errors (implausible combinations of
otherwise well-formed fields, checked by `FeatureBusinessValidator`) return
the same shape, so the two failure classes are always distinguishable by
the `error` field:

```json
{
  "timestamp": "2026-07-08T13:51:44.368257Z",
  "status": 400,
  "error": "Request format validation failed",
  "details": [
    "clientAge: clientAge must be at least 18",
    "requestId: requestId must not be blank"
  ]
}
```

```json
{
  "timestamp": "2026-07-08T13:51:44.404212Z",
  "status": 400,
  "error": "Business validation failed",
  "details": [
    "employmentMonths (500) exceeds what is plausible for clientAge (20), assuming a minimum working age of 16",
    "requestedAmount (5000000) is implausibly high relative to monthlyIncome (1000)"
  ]
}
```

## Input features

| Feature              | Meaning                                                          |
|-----------------------|-------------------------------------------------------------------|
| `requestId`          | Caller-supplied identifier, echoed back in the response           |
| `clientAge`          | Applicant's age in years                                          |
| `monthlyIncome`      | Applicant's declared monthly income                               |
| `requestedAmount`    | Amount of credit requested                                        |
| `employmentMonths`   | Months in current employment                                      |
| `hasOverduePayments` | Whether the applicant currently has overdue payments on record    |
| `activeLoansCount`   | Number of active loans the applicant currently holds              |

## Scoring rules (model `rule-based-v1`)

The model starts from a base score of **50** and adds/subtracts fixed
points per rule, then clamps the result to **[0, 100]**:

| Rule                          | Condition                                    | Impact |
|--------------------------------|-----------------------------------------------|--------|
| Age                            | outside 21-65                                 | -10    |
|                                 | 21-24 or 56-65                                | 0      |
|                                 | 25-55                                         | +10    |
| Requested amount / income      | ratio <= 3                                    | +20    |
|                                 | ratio <= 6                                    | +5     |
|                                 | ratio <= 10                                   | -10    |
|                                 | ratio > 10                                    | -30    |
| Employment duration            | < 6 months                                    | -15    |
|                                 | 6-24 months                                   | 0      |
|                                 | > 24 months                                   | +15    |
| Overdue payments               | none                                          | +5     |
|                                 | present                                       | -35    |
| Active loans                   | 0                                             | +10    |
|                                 | 1-2                                           | 0      |
|                                 | 3-4                                           | -15    |
|                                 | 5+                                            | -30    |

**Decision thresholds** (on the final, clamped score):

- `score >= 70` -> `APPROVED`
- `40 <= score < 70` -> `MANUAL_REVIEW`
- `score < 40` -> `REJECTED`

The `factors` array in the response lists every rule that fired, with its
code, a human-readable description, and its signed point contribution -
this is what the score is built from, in full.

**Model version:** `rule-based-v1`, returned in every response as
`modelVersion` (also exposed as `RuleBasedScoringModelV1.VERSION` in code)
so that decisions remain traceable to the exact model logic that produced
them.

## Sample requests

The [`samples/`](samples) directory has ready-to-use request bodies:

| File                                                          | Expected result                          |
|-----------------------------------------------------------------|--------------------------------------------|
| [`approved-request.json`](samples/approved-request.json)                     | `score: 100`, `APPROVED`                  |
| [`manual-review-request.json`](samples/manual-review-request.json)           | `score: 40`, `MANUAL_REVIEW`               |
| [`rejected-request.json`](samples/rejected-request.json)                     | `score: 0`, `REJECTED`                     |
| [`invalid-format-request.json`](samples/invalid-format-request.json)         | `400`, format validation errors            |
| [`invalid-business-rules-request.json`](samples/invalid-business-rules-request.json) | `400`, business validation errors  |

Try any of them with:

```bash
curl -s -X POST http://localhost:8080/api/v1/scoring \
  -H "Content-Type: application/json" \
  -d @samples/manual-review-request.json | python3 -m json.tool
```

## Architecture

```
com.example.scoring
├── model        # ScoringModel interface, ScoringFeatures/ScoringResult/
│                 # ScoringFactor/Decision, RuleBasedScoringModelV1 - zero
│                 # dependency on the web layer
├── validation    # FeatureBusinessValidator - cross-field/business checks
│                 # on the domain feature vector
├── service       # ScoringService - orchestrates validate -> score -> map
├── api           # ScoringController, ScoringMapper, request/response DTOs
└── exception     # ScoringValidationException, GlobalExceptionHandler
```

The model package only knows about `ScoringFeatures` and `ScoringResult`
- it has no imports from `api` or Spring Web. This is what makes it
possible to add `RuleBasedScoringModelV2`, or an entirely different
implementation, behind the same `ScoringModel` interface without touching
the API layer (see [migration path](#migration-path-to-ml--onnx--an-external-model-service)).

## Testing

```bash
mvn test
```

- `RuleBasedScoringModelV1Test` - approve/manual-review/reject outcomes,
  decision-threshold boundaries, score clamping.
- `FeatureBusinessValidatorTest` - each business rule individually and
  combined.
- `ScoringServiceTest` - orchestration order (validate before scoring) and
  short-circuiting on validation failure, with mocked model/validator.
- `ScoringControllerTest` - HTTP-level behavior with the service mocked
  (`@WebMvcTest`).
- `ScoringApiIntegrationTest` - full Spring context, no mocks, exercising
  the same approve/manual/reject/business-error scenarios end to end.

## Limitations

This model is a teaching example, not a credit risk model. Concretely:

- **Not trained or statistically validated.** The rules, weights, and
  thresholds were chosen by hand for illustration; they were not fit or
  backtested against any historical outcome data.
- **No calibration.** The `score` is not a calibrated probability of
  default or repayment; it's an arbitrary point scale.
- **No fairness/bias analysis.** A model like this, if it were real, would
  need disparate-impact testing across protected classes before any
  production use - none has been done here.
- **Tiny feature set.** Seven fields is not enough signal for a real
  underwriting decision (no credit bureau data, no transaction history, no
  collateral, no macro factors, etc.).
- **Arbitrary thresholds.** 70/40 were picked to produce a readable
  three-way split in a demo, not derived from any cost/risk analysis.
- **No monitoring, drift detection, or audit trail.** A production scoring
  system needs decision logging, model performance monitoring, and a
  change-management process for rule/threshold updates; none of that
  exists here.
- **No authentication, authorization, or rate limiting** on the API.
- **No real data, no real system integrations.** This service does not
  call out to any credit bureau, bank, or production system, and none of
  the sample data represents real people or accounts.

## Migration path to ML / ONNX / an external model service

The `ScoringModel` interface is the seam designed for this:

```java
public interface ScoringModel {
    String getVersion();
    ScoringResult score(ScoringFeatures features);
}
```

Nothing outside the `model` package needs to change to swap the
implementation, because the API/service layers only ever depend on this
interface, never on `RuleBasedScoringModelV1` directly.

A realistic path forward:

1. **Collect labeled outcome data** the proper way: from real, consented,
   compliant historical decisions/outcomes (not from this demo's sample
   data), with a documented data governance and fairness review process.
2. **Train and validate a candidate model offline** (e.g. logistic
   regression or gradient boosting) against that data, including
   calibration and bias/fairness testing, and get sign-off from risk/legal
   before it goes anywhere near production traffic.
3. **Export the trained model to ONNX** and add an
   `OnnxScoringModelV2 implements ScoringModel` that loads the `.onnx` file
   (e.g. via ONNX Runtime's Java bindings) and maps `ScoringFeatures` to the
   model's input tensor and its output back to a `ScoringResult`.
   Alternatively, implement a `RemoteScoringModel implements ScoringModel`
   that calls out to a dedicated external model-serving endpoint (e.g. over
   HTTP/gRPC), keeping the same interface so the rest of the service is
   unaware of where scoring actually happens.
4. **Run champion/challenger evaluation**: keep `rule-based-v1` as the
   live decision-maker while logging the new model's output side by side,
   before switching which implementation is wired into `ScoringService`.
5. **Version everything.** Bump `getVersion()` for every model change so
   every decision stays traceable to the exact model/version that produced
   it, and keep old versions available for reproducing past decisions.
6. **Add production concerns** that this demo intentionally skips:
   authentication/authorization on the endpoint, request/decision audit
   logging, model performance and drift monitoring, and a rollback plan.
