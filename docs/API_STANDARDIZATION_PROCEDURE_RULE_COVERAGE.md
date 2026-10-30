# API standardization: Procedure, Rule, and Coverage

AI-269 standardizes the public Procedure Builder, rule-configuration, and procedure-coverage APIs.

## Success envelope

Every response from these endpoints has the form:

```json
{
  "data": {},
  "meta": {},
  "timestamp": "2026-10-28T10:30:00"
}
```

Procedure and rule collections accept `cursor` and `limit` (1–100). They return `meta.count` and `meta.nextCursor`; pass the latter as the next request's cursor. Procedure cursors are stable procedure IDs; rule cursors are stable rule IDs.

## Stable error codes

Error responses retain the established status/message fields and now add `code` for client automation. Key codes include:

- `RESOURCE_NOT_FOUND`
- `VALIDATION_FAILED`
- `INVALID_PARAMETER`
- `MISSING_PARAMETER`
- `MALFORMED_REQUEST`
- `INVALID_APPROVAL_TRANSITION`
- `COVERAGE_THRESHOLD_NOT_MET`

`COVERAGE_THRESHOLD_NOT_MET` is emitted when an approval attempt is blocked by frozen coverage requirements, allowing clients to distinguish it from other conflicts without parsing prose.

## Frontend compatibility

The frontend service adapters unwrap the envelope before components consume `response.data`, so existing Procedure Builder and rule-configuration workflows retain their current UI contracts.
