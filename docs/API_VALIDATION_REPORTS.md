# IATERAP API Documentation: Validation Reports & Synthetic Fault Injection

| Document Information | Details |
|---|---|
| Ticket Scope | AI-239 / T13-B1, T13-B2, T13-B3, T13-DOC |
| Module | `com.adtech.iaterap.backend.controller.ValidationController` |
| Security | Spring Security JWT + Method-Level Role-Based Access Control |
| API Context | `/api/validation` |
| Status | Approved & Implemented |

---

## 1. Overview & Architectural Principles

The Validation subsystem provides independent evidence comparison and discrepancy detection for aerospace test engineering. Test procedures undergo validation by comparing expected engineering requirements and behavioural states against observed telemetry or injected synthetic fixtures.

### Key Architectural Guarantees:
1. **Immutable Snapshot Anchoring:** Synthetic fault injection and validation reports evaluate against immutable procedure revisions (`ProcedureHistory`) rather than mutable live draft procedures.
2. **Strict Synthetic Evidence Isolation:** Synthetic test fixtures are explicitly prefixed (`synthetic-{scenario}-{category}-{uuid}.json`), flagged with `synthetic: true`, and tagged with `_injectionRunId`, `_syntheticScenario`, and `_faultCategory`. They are never represented as actual flight hardware telemetry.
3. **Role-Based Access Control:** Execution of synthetic fault injection is restricted to authorized engineering roles (`ADMIN`, `TEST_ENGINEER`, `SYSTEM_ENGINEER`, `RELIABILITY_ENGINEER`). Non-authorized roles (`VIEWER`, `FIELD_ENGINEER`) and unauthenticated clients are denied access.
4. **Approval Gate:** Synthetic fault injection requires a procedure revision in `APPROVED` status. Drafts or in-review procedures are rejected.

---

## 2. API Endpoints

### 2.1 Trigger Synthetic Fault Injection

Executes a deterministic synthetic fault or nominal simulation against an approved procedure revision snapshot. The generated evidence passes through the standard ingestion pipeline (`ValidationIngestionService`) for unbiased contract evaluation.

```http
POST /api/validation/runs/synthetic
```

#### Authorization
- **Permitted Roles:** `ADMIN`, `TEST_ENGINEER`, `SYSTEM_ENGINEER`, `RELIABILITY_ENGINEER`
- **Denied Roles:** `VIEWER`, `FIELD_ENGINEER` (HTTP 403 Forbidden)
- **Unauthenticated:** HTTP 401 Unauthorized

#### Request Parameters (Query / Form)

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `procedureHistoryId` | Long | Yes | - | Identifier of the immutable procedure revision snapshot |
| `scenario` | String | No | `NOMINAL` | Simulation scenario: `NOMINAL` or `FAULT_INJECTED` |
| `faultCategory` | String | No | `OUT_OF_RANGE` (if FAULT_INJECTED) | Supported category: `OUT_OF_RANGE`, `DISCRETE_MISMATCH`, `MISSING_SIGNAL` |
| `targetLruId` | Long | No | - | Optional LRU identifier for targeting validation; must match the procedure's bound LRU |
| `targetStepId` | Long | No | - | Optional specific test step identifier from the revision's snapshot |

#### Representative Success Response (`200 OK`)

```json
{
  "id": 142,
  "procedureHistoryId": 18,
  "sourceFilename": "synthetic-fault_injected-out_of_range-f61b7f94-7cb5-4521-9989-1065ea0cf3d2.json",
  "sourceSha256": "4b825dc642cb6eb9a060e54bf8d69288fbee4904ce6243260f40222f44c61d4a",
  "sourceFormat": "JSON",
  "recordedBy": "validator.engineer",
  "status": "VALIDATED",
  "uploadedAt": "2026-10-07T14:30:00",
  "totalReadings": 4,
  "matchedCount": 3,
  "mismatchedCount": 1,
  "outlierCount": 0,
  "missingCount": 0
}
```

#### Error Responses

| Status Code | Reason | Cause | Example Error Message |
|---|---|---|---|
| `400 Bad Request` | Validation Error | Missing procedure revision parameter | `"Procedure revision is required"` |
| `400 Bad Request` | Validation Error | Procedure revision does not exist | `"Procedure revision not found: 999"` |
| `400 Bad Request` | Lifecycle Gate | Revision status is not `APPROVED` | `"Synthetic fault injection requires an APPROVED procedure revision"` |
| `400 Bad Request` | Target Mismatch | `targetLruId` differs from revision LRU | `"Procedure revision 18 does not belong to LRU 5"` |
| `400 Bad Request` | Target Step Missing | `targetStepId` not present in revision | `"testStepId 9999 is not part of procedure revision 18"` |
| `400 Bad Request` | Unsupported Category | Unknown category name | `"Unsupported synthetic fault category: CORRUPTED_FRAME"` |
| `401 Unauthorized` | Authentication | Missing or expired JWT token | `"Full authentication is required to access this resource"` |
| `403 Forbidden` | Authorization | User role lacks permission (e.g. VIEWER) | `"You do not have permission to access this resource."` |

---

### 2.2 Retrieve Consolidated Validation Report

Retrieves comprehensive validation outcomes, procedure snapshot metadata, telemetry/synthetic flags, reading pass rates, and individual test step evaluations for a run.

```http
GET /api/validation/runs/{runId}/report
GET /api/validation/reports/{runId}
```

*Note: `/api/validation/reports/{runId}` is provided as a route alias to support RESTful report access.*

#### Authorization
- Authenticated JWT user (any authenticated role: `ADMIN`, `TEST_ENGINEER`, `SYSTEM_ENGINEER`, `RELIABILITY_ENGINEER`, `VIEWER`).

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `runId` | Long | Yes | Validation run identifier |

#### Representative Success Response (`200 OK`)

```json
{
  "runId": 142,
  "procedureHistoryId": 18,
  "procedureName": "Primary Flight Computer Actuator Control Verification",
  "procedureVersionNumber": 2,
  "lruId": 104,
  "lruName": "PFC-ACT-104",
  "sourceFilename": "synthetic-fault_injected-out_of_range-f61b7f94-7cb5-4521-9989-1065ea0cf3d2.json",
  "sourceFormat": "JSON",
  "recordedBy": "validator.engineer",
  "status": "VALIDATED",
  "uploadedAt": "2026-10-07T14:30:00",
  "synthetic": true,
  "totalReadings": 4,
  "matchedCount": 3,
  "mismatchedCount": 1,
  "outlierCount": 0,
  "missingCount": 0,
  "passRatePercentage": 75.00,
  "readings": [
    {
      "id": 501,
      "validationRunId": 142,
      "testStepId": 1001,
      "sourceRowNumber": 1,
      "observedValue": 28.0000000000,
      "observedUnit": "VOLT",
      "normalizedValue": 28.0000000000,
      "observedBehaviour": null,
      "result": "MATCH",
      "deviation": 0.0000000000,
      "errorMessage": null,
      "recordedAt": "2026-10-07T14:30:01"
    },
    {
      "id": 502,
      "validationRunId": 142,
      "testStepId": 1002,
      "sourceRowNumber": 2,
      "observedValue": 38.5000000000,
      "observedUnit": "VOLT",
      "normalizedValue": 38.5000000000,
      "observedBehaviour": null,
      "result": "MISMATCH",
      "deviation": 10.5000000000,
      "errorMessage": "Reading is outside the expected range",
      "recordedAt": "2026-10-07T14:30:01"
    }
  ]
}
```

#### Error Responses

| Status Code | Reason | Cause | Example Error Message |
|---|---|---|---|
| `400 Bad Request` | Resource Not Found | `runId` does not exist | `"Validation run not found: 999"` |
| `401 Unauthorized` | Authentication | Missing or expired JWT token | `"Full authentication is required to access this resource"` |

---

### 2.3 List Consolidated Validation Reports

Retrieves all validation reports associated with a specific procedure revision.

```http
GET /api/validation/reports?procedureHistoryId={procedureHistoryId}
```

#### Request Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `procedureHistoryId` | Long | Yes | Procedure history revision ID |

#### Representative Success Response (`200 OK`)

```json
[
  {
    "runId": 142,
    "procedureHistoryId": 18,
    "procedureName": "Primary Flight Computer Actuator Control Verification",
    "procedureVersionNumber": 2,
    "lruId": 104,
    "lruName": "PFC-ACT-104",
    "sourceFilename": "synthetic-fault_injected-out_of_range-f61b7f94.json",
    "sourceFormat": "JSON",
    "recordedBy": "validator.engineer",
    "status": "VALIDATED",
    "uploadedAt": "2026-10-07T14:30:00",
    "synthetic": true,
    "totalReadings": 4,
    "matchedCount": 3,
    "mismatchedCount": 1,
    "outlierCount": 0,
    "missingCount": 0,
    "passRatePercentage": 75.00,
    "readings": [ ... ]
  }
]
```

---

### 2.4 Ingest Hardware Telemetry Fixture

Ingests an external CSV or JSON telemetry dataset uploaded by an engineer and compares against the procedure contract.

```http
POST /api/validation/runs
Content-Type: multipart/form-data
```

#### Form Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `procedureHistoryId` | Long | Yes | Procedure history snapshot ID |
| `format` | String | Yes | `CSV` or `JSON` |
| `file` | Binary | Yes | File payload (.csv or .json) |

#### Response (`200 OK`)
Returns `ValidationRun` object with calculated matched, mismatched, outlier, and missing counts.

---

### 2.5 List Validation Runs

Lists runs associated with an approved procedure revision.

```http
GET /api/validation/runs?procedureHistoryId={procedureHistoryId}
```

#### Response (`200 OK`)
Returns `List<ValidationRun>`.

---

### 2.6 Get Run Readings

Lists raw reading outcomes for a specific validation run.

```http
GET /api/validation/runs/{runId}/readings
```

#### Response (`200 OK`)
Returns `List<ValidationReading>`.
