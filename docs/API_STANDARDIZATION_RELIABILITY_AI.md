# Reliability and AI API reference

| Document information | Details |
| --- | --- |
| Jira | AI-274 / T17-B3 |
| Scope | `/api/reliability/**`, `/api/ai/**`, `/api/ai/context/**` |
| Contract | Standard response envelope, version 1 |

## Shared contract

All Reliability and AI endpoints documented here return the standard success envelope:

```json
{
  "data": {},
  "meta": {},
  "timestamp": "2026-11-02T12:00:00"
}
```

`data` is the endpoint payload. `meta` is empty for non-paginated resources; consumers must not infer pagination from its absence. Error responses retain `status`, `error`, `message`, `timestamp`, and a stable machine-readable `code`. Relevant codes are `AI_ADVISORY_UNAVAILABLE`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `MALFORMED_REQUEST`, `METHOD_NOT_ALLOWED`, `AUTHENTICATION_REQUIRED`, `AUTHENTICATION_FAILED`, `ACCESS_DENIED`, and `INTERNAL_ERROR`.

All endpoints require an access JWT unless stated otherwise. A refresh token cannot authenticate an ordinary API request.

## Reliability API

Every Reliability route requires `ADMIN`, `SYSTEM_ENGINEER`, or `RELIABILITY_ENGINEER`. Data is calculated only from persisted synthetic-event/validation evidence; it must not be represented as live aircraft telemetry.

| Method and path | `data` payload | Purpose |
| --- | --- | --- |
| `GET /api/reliability/dashboard` | dashboard totals and per-LRU evidence summary | Dashboard read model |
| `GET /api/reliability/metrics` | operating-hour method and metric summaries at LRU, subsystem, and aircraft scope | Combined formula view |
| `GET /api/reliability/mtbf` | `MTBF_HOURS` metric series | Total operating hours / failures |
| `GET /api/reliability/mttr` | `MTTR_HOURS` metric series | Failure-to-repair duration / repairs |
| `GET /api/reliability/failure-rate` | `FAILURE_RATE_PER_HOUR` metric series | Failures / operating hours |
| `GET /api/reliability/reports/current` | current reporting-table rows | Nightly reporting projection |
| `GET /api/reliability/pareto-hfra` | failure ranking and HFRA flags | Pareto/trend analysis |

### Formula semantics

The metrics response contains `generatedAt`, `operatingHoursMethod`, and LRU, subsystem, and aircraft arrays. A metric item contains `id`, `name`, `operatingHours`, `failures`, `repairs`, `mtbfHours`, `mttrHours`, and `failureRatePerHour`.

- One `SORTIE_COMPLETED` synthetic event is one simulated operating hour.
- MTBF and failure rate are `null` when operating hours are unavailable; MTBF is also `null` when there are no failures.
- MTTR is `null` when there are no completed repairs.
- Formula-series endpoints return `generatedAt`, `metric`, and `lrus`, `subsystems`, and `aircraft` value arrays.

`/reports/current` exposes the persisted projection keyed by `(lru_id, period_start)`. Its rows include the period, inputs, calculated formula values, calculation time, and source. The scheduled reporting job refreshes this projection; a read request does not recalculate it.

## AI Advisory API

AI Advisory is decision support. The Spring application owns authentication, authorization, persistence, approval, and all engineering writes; the FastAPI service receives structured input over HTTP and has no database access.

| Method and path | Request / result | Purpose |
| --- | --- | --- |
| `POST /api/ai/suggestions` | `test_procedure_id`, `test_step_id`, optional `prompt` → suggestion, confidence, source, generated time | Request a structured advisory |
| `POST /api/ai/suggestions/queue` | Same request → queued suggestion | Queue a suggestion for the authenticated engineer |
| `GET /api/ai/suggestions/queue` | queued suggestions for caller | List pending decisions |
| `GET /api/ai/suggestions/accepted?procedureId={id}` | accepted suggestions for caller/procedure | Read accepted items |
| `POST /api/ai/suggestions/{id}/accept` | queue item | Record explicit acceptance |
| `POST /api/ai/suggestions/{id}/dismiss` | queue item | Record explicit dismissal |
| `GET /api/ai/context/test-procedure/{procedureId}` | read-only procedure and coverage-context snapshot | AI isolation boundary |

Only the accept/dismiss action records a queue decision; no suggestion is automatically applied to a procedure. Suggestions are attributed through `requestedBy` and `decidedBy`. The context endpoint maps only `GET`; unsupported verbs receive the MVC `405` contract and cannot invoke a write service.

For the detailed, versioned AI context schema, see [API_AI_CONTEXT.md](API_AI_CONTEXT.md).

## Compatibility and client guidance

- Read the payload from `data`; do not bind directly to the outer response object.
- Treat numeric metric values as nullable where documented.
- Branch on the stable error `code`, not the human-readable `message`.
- Send only access JWTs in `Authorization: Bearer <token>` for protected calls.
