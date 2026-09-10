# IATERAP API Documentation: Procedure Coverage

| Document Information | Details |
|---|---|
| Jira | AI-214 [T10-DOC] API documentation: `/procedures/{id}/coverage` |
| Document Type | API Documentation (Draft) |
| Status | Draft - endpoint contract documented; controller implementation pending |
| API Version | v1 |
| Last Updated | 10 September 2026 |

---

## 1. Purpose

This document defines the REST contract for calculating the six-category coverage score for one test procedure. The endpoint is read-only: it evaluates the procedure's current test steps using the configured coverage rules and returns the category percentages together with the weighted composite score.

The contract is documented ahead of controller implementation so the backend and Procedure Builder frontend can integrate against the same response shape.

## 2. Endpoint

```http
GET /api/procedures/{id}/coverage
```

| Component | Description |
|---|---|
| `{id}` | Numeric test-procedure identifier |
| Request body | None |
| Authentication | Authenticated user; same security policy as other `/api/procedures` endpoints |
| Side effects | None; the evaluation does not modify the procedure, steps, or revision history |

## 3. Successful response

**Status:** `200 OK`

```json
{
  "procedureId": 42,
  "versionNumber": 3,
  "categories": {
    "INTERFACE": 80.0,
    "SIGNAL": 60.0,
    "REQUIREMENT": 40.0,
    "FUNCTIONAL": 20.0,
    "FAULT": 0.0,
    "FMEA": 100.0
  },
  "weights": {
    "INTERFACE": 0.15,
    "SIGNAL": 0.10,
    "REQUIREMENT": 0.20,
    "FUNCTIONAL": 0.10,
    "FAULT": 0.25,
    "FMEA": 0.20
  },
  "compositeScore": 53.0,
  "evaluatedAt": "2026-09-10T12:30:00Z"
}
```

`compositeScore` is calculated as:

```text
(FAULT × 0.25) + (FMEA × 0.20) + (REQUIREMENT × 0.20)
+ (INTERFACE × 0.15) + (SIGNAL × 0.10) + (FUNCTIONAL × 0.10)
```

All category percentages and the composite score are returned on a `0.00`–`100.00` scale and rounded to two decimal places. The six weights sum to `1.00`.

## 4. Category semantics

| Category | Meaning |
|---|---|
| `INTERFACE` | Test steps referencing an interface element |
| `SIGNAL` | Test steps referencing a signal element |
| `REQUIREMENT` | Test steps representing a requirement |
| `FUNCTIONAL` | Test steps representing a functional element |
| `FAULT` | Test steps referencing a fault element |
| `FMEA` | Test steps containing FMEA evidence |

For each category, the percentage is:

```text
(referenced elements ÷ total procedure test steps) × 100
```

When a procedure has no test steps, each category evaluates to `0.0`; the response remains valid and the composite score is `0.0`.

## 5. Error responses

### `400 Bad Request`

The `{id}` path value is not numeric.

### `401 Unauthorized`

No valid authentication credentials were supplied.

### `404 Not Found`

No test procedure exists with the requested identifier.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Test procedure not found: 42",
  "timestamp": "2026-09-10T12:30:00"
}
```

### `500 Internal Server Error`

The configured rule set is incomplete or a configured rule has no registered implementation. The service must not return a partial score in this case.

## 6. Consistency and revision behaviour

- The evaluation uses the procedure's current persisted test steps and current enabled rule configuration.
- The endpoint does not create a procedure revision or audit entry.
- Calling the endpoint repeatedly with unchanged procedure data produces the same score.
- A later revision may produce a different score; clients should retain `versionNumber` with the result when displaying or exporting it.

## 7. Implementation status

`CoverageEngine` and the six category rules are implemented and unit-tested. The controller/service endpoint described here is the next integration step; until that work is merged, this document is a draft contract and `/api/procedures/{id}/coverage` must not be presented as an available production route.

## 8. Jira reference

- AI-214 / T10-DOC
