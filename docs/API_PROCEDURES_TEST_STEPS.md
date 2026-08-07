# IATERAP API Documentation: Test Procedures & Test Steps

| Document Information | Details |
|---|---|
| Jira | AI-169 [T5-DOC] API documentation: /procedures, /procedures/{id}/steps |
| Document Type | API Documentation (Draft) |
| Status | Draft - endpoints not yet implemented |
| Last Updated | 7 August 2026 |
| API Version | v1 |
| Conventions | See Proposal Section 22 (API Design) |

---

## 1. Purpose

This document drafts the REST API contract for the Test Procedure Engineering module's `/procedures` and `/procedures/{id}/steps` endpoints, ahead of controller implementation.

`ProcedureController` and any Test Step-related controller do not exist yet in the codebase as of this document's writing (only the `TestProcedure` and `TestStep` JPA entities, repositories, and Flyway schema exist - see `DATABASE_DESIGN.md`). This document is a design artifact: the contract described here is intended to guide implementation, not to document already-shipped behaviour.

## 2. Scope

Covered in this document:
- `GET /api/v1/procedures` - list Test Procedures
- `POST /api/v1/procedures` - create a Test Procedure
- `GET /api/v1/procedures/{id}` - fetch a single Test Procedure
- `GET /api/v1/procedures/{id}/steps` - list Test Steps for a Procedure
- `POST /api/v1/procedures/{id}/steps` - add a Test Step to a Procedure

Not covered (out of scope for this ticket):
- Procedure/Step update (`PUT`) and version-revision endpoints - depend on the Version Management module (Section 19), not yet built.
- Coverage Engine endpoints (coverage score, gap analysis) - depend on the Rule Engine, not yet built.
- Approval workflow transition endpoints - Intern B's scope (Version Management & Approval Workflow).

## 3. Conventions Followed

Per Proposal Section 22:
- Resource paths are plural and noun-based; no verbs in the URL.
- Every endpoint has a distinct request DTO and response DTO, decoupled from the JPA entities.
- Responses use a consistent envelope: `{ "data": ..., "meta": {...}, "errors": [...] }`.
- Status codes: `200`/`201` success, `400` validation errors, `404` missing resource, `409` state-conflict (e.g. editing a locked Approved revision).
- Pagination on list endpoints is cursor-based (`GET /api/v1/procedures?cursor=...&limit=...`).

## 4. Endpoint: `GET /api/v1/procedures`

Lists Test Procedures.

**Query parameters**

| Param | Type | Description |
|---|---|---|
| `lruId` | long, optional | Filter by owning LRU |
| `approvalStatus` | string, optional | Filter by `DRAFT` / `SUBMITTED` / `APPROVED` / `REJECTED` |
| `cursor` | string, optional | Pagination cursor |
| `limit` | int, optional | Page size, default 20 |

**Response `200`**

```json
{
  "data": [
    {
      "id": 1,
      "lruId": 4,
      "lruName": "Flight Control Unit",
      "name": "Flight Control Unit - Interface Validation",
      "description": "Validates CAN interface signals against nominal range.",
      "approvalStatus": "DRAFT",
      "versionNumber": 1,
      "createdAt": "2026-08-01T10:00:00",
      "updatedAt": "2026-08-01T10:00:00"
    }
  ],
  "meta": { "nextCursor": null },
  "errors": []
}
```

## 5. Endpoint: `POST /api/v1/procedures`

Creates a new Test Procedure.

**Request body**

```json
{
  "lruId": 4,
  "name": "Flight Control Unit - Interface Validation",
  "description": "Validates CAN interface signals against nominal range."
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `lruId` | long | Yes | Must reference an existing LRU |
| `name` | string | Yes | Non-blank |
| `description` | string | No | |

`approvalStatus` defaults to `DRAFT` and `versionNumber` to `1`; neither is client-settable on create.

**Response `201`** - same shape as the list item above.

**Errors**
| Status | Cause |
|---|---|
| `400` | `name` blank, or `lruId` missing |
| `404` | `lruId` does not reference an existing LRU |

## 6. Endpoint: `GET /api/v1/procedures/{id}`

Fetches a single Test Procedure by ID.

**Response `200`** - same shape as a list item.
**Response `404`** - no Test Procedure with that ID.

## 7. Endpoint: `GET /api/v1/procedures/{id}/steps`

Lists Test Steps belonging to a Test Procedure, ordered by `executionOrder`.

**Response `200`**

```json
{
  "data": [
    {
      "id": 10,
      "testProcedureId": 1,
      "interfaceId": 7,
      "connectorId": null,
      "signalLibraryId": null,
      "protocolTagId": null,
      "faultTemplateId": null,
      "expectedBehaviour": "Signal present on interface",
      "expectedRange": "0-100",
      "calibrationRequirement": null,
      "safetyNotes": null,
      "acceptanceCriteria": "Signal within nominal range",
      "preconditions": null,
      "dependencies": null,
      "executionOrder": 1,
      "postConditions": null,
      "passFailCriteria": "Pass if within range, fail otherwise",
      "expectedObservation": null,
      "engineerRemarks": null,
      "approvalStatus": "DRAFT",
      "versionNumber": 1,
      "createdAt": "2026-08-01T10:05:00",
      "updatedAt": "2026-08-01T10:05:00"
    }
  ],
  "meta": { "nextCursor": null },
  "errors": []
}
```

`lruId` (the denormalized column added in AI-167 for composite FK enforcement) is intentionally omitted from the response - it's a persistence-layer implementation detail, not part of the engineering contract.

**Response `404`** - no Test Procedure with that ID.

## 8. Endpoint: `POST /api/v1/procedures/{id}/steps`

Adds a Test Step to a Test Procedure.

**Request body**

```json
{
  "interfaceId": 7,
  "connectorId": null,
  "signalLibraryId": null,
  "protocolTagId": null,
  "faultTemplateId": null,
  "expectedBehaviour": "Signal present on interface",
  "acceptanceCriteria": "Signal within nominal range",
  "passFailCriteria": "Pass if within range, fail otherwise",
  "executionOrder": 1
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `interfaceId` / `connectorId` / `signalLibraryId` / `protocolTagId` / `faultTemplateId` | long | No, but at least one recommended | Must belong to the same LRU as the parent Test Procedure (enforced at the DB level by AI-167's composite FKs) |
| `expectedBehaviour` | string | Yes | Non-blank |
| `acceptanceCriteria` | string | Yes | Non-blank |
| `passFailCriteria` | string | Yes | Non-blank |
| `executionOrder` | int | Yes | |

**Response `201`** - same shape as a step list item.

**Errors**
| Status | Cause |
|---|---|
| `400` | A required field is blank/missing |
| `404` | `{id}` does not reference an existing Test Procedure, or a referenced Interface/Connector/etc. does not exist |
| `409` | A referenced Interface/Connector/etc. belongs to a different LRU than the parent Test Procedure (composite FK violation, AI-167) |

## 9. Open Questions for Implementation

- Should `POST /procedures/{id}/steps` accept a client-supplied `executionOrder`, or should the server auto-assign the next sequence number? This document assumes client-supplied for now.
- The `409` case for a cross-LRU reference (Section 8) needs the controller to catch `DataIntegrityViolationException` and translate it to a structured `409` with an error code (e.g. `LRU_MISMATCH`) per Section 22's error-handling convention - not yet implemented anywhere in the codebase.

## 10. Ref

AI-169
