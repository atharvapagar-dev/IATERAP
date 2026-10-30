# API standardization: Reliability and AI

AI-273 applies IATERAP's standard public API contract to Reliability and AI Advisory endpoints.

## Success responses

Every Reliability endpoint (`/api/reliability/**`) and AI Advisory/context endpoint (`/api/ai/**`) now returns:

```json
{
  "data": {},
  "meta": {},
  "timestamp": "2026-10-28T10:45:00"
}
```

Lists are present in `data` and retain an empty `meta` object until a cursor contract is required. The frontend adapters unpack `data` before page components consume it, preserving existing UI models.

## Stable errors

Errors retain status, error, message, and timestamp and add a machine-readable `code`. Relevant codes include `AI_ADVISORY_UNAVAILABLE`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `MALFORMED_REQUEST`, `METHOD_NOT_ALLOWED`, `AUTHENTICATION_FAILED`, `ACCESS_DENIED`, `INVALID_REQUEST`, `INVALID_APPROVAL_TRANSITION`, `COVERAGE_THRESHOLD_NOT_MET`, and `INTERNAL_ERROR`.
