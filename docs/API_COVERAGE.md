# Procedure and Coverage API

| Status | Finalized for the current backend contract |
| --- | --- |
| Base path | `/api/procedures` |
| Authentication | Required |
| Response convention | `{ "data": ..., "meta": {...}, "timestamp": "..." }` |

## Procedure collection

`GET /api/procedures?cursor={lastId}&limit={1..100}` returns procedures after the numeric cursor in ascending procedure-id order. `limit` defaults to 50. `GET /api/procedures/by-lru/{lruId}` and `GET /api/procedures/review-queue` support the same pagination contract.

```json
{
  "data": [{ "id": 42, "name": "FCU validation", "versionNumber": 3, "approvalStatus": "DRAFT" }],
  "meta": { "count": 1, "nextCursor": "42" },
  "timestamp": "2026-10-30T12:00:00"
}
```

The procedure API also provides read/create/update, review submission, approval/rejection, revision history, rollback, immutable snapshots, and frozen coverage reports. All successful responses use the same envelope; reviewer actions require `ADMIN`, `SYSTEM_ENGINEER`, or `RELIABILITY_ENGINEER`.

## Current coverage

`GET /api/procedures/{procedureId}/coverage` evaluates the current persisted procedure and its ordered steps without creating a revision or audit record.

```json
{
  "data": {
    "procedureId": 42,
    "versionNumber": 3,
    "categories": { "COVERAGE-INTERFACE": 80.0, "COVERAGE-FAULT": 100.0 },
    "weights": { "COVERAGE-INTERFACE": 0.15, "COVERAGE-FAULT": 0.25 },
    "compositeScore": 85.0,
    "thresholdResults": { "COVERAGE-INTERFACE": true, "COVERAGE-FAULT": true },
    "unavailableReasons": {},
    "gaps": []
  },
  "meta": {},
  "timestamp": "2026-10-30T12:00:00"
}
```

Scores are percentages (`0.00`–`100.00`). The composite applies the enabled persisted coverage-rule weights. If any enabled category cannot produce a percentage, its score and threshold result are `null`, a reason is returned in `unavailableReasons`, and the composite is `null`; the API never reports a misleading partial composite.

## Rule configuration

`GET /api/admin/rules?cursor={lastRuleId}&limit={1..100}` is administrator-only and uses lexical rule-id cursors. `PATCH /api/admin/rules/{ruleId}/enabled` and `PATCH /api/admin/rules/{ruleId}/priority` return the updated configuration in the standard envelope. Coverage scoring configuration is managed under `/api/admin/coverage/scoring-config` and is also administrator-only.

## Errors

All errors retain HTTP status, `error`, human-readable `message`, `timestamp`, and a stable `code`. Important codes are `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `MALFORMED_REQUEST`, `METHOD_NOT_ALLOWED`, `ACCESS_DENIED`, `INVALID_APPROVAL_TRANSITION`, and `COVERAGE_THRESHOLD_NOT_MET`.

`COVERAGE_THRESHOLD_NOT_MET` is a `409 Conflict` returned by approval when the frozen submission-time coverage report has a failed required threshold. Clients should branch on `code`, not parse the message.
