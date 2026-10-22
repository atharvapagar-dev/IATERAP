# Reliability Metrics API

All routes require `ADMIN`, `SYSTEM_ENGINEER`, or `RELIABILITY_ENGINEER`. They use only persisted, explicitly synthetic event evidence; no endpoint represents live aircraft telemetry.

## Formula endpoints

`GET /api/reliability/mtbf` returns MTBF in simulated operating hours. For every scope, `value = total operating hours / number of failures`. A value is `null` when no operating hours or no failures make MTBF unavailable.

`GET /api/reliability/mttr` returns MTTR in simulated hours. For every scope, `value = total time from a failure event to its repair event / number of repairs`.

`GET /api/reliability/failure-rate` returns failure rate per simulated operating hour. For every scope, `value = failures / operating hours`; it is `null` when no operating hours exist.

Each response has `generatedAt`, `metric`, plus `lrus`, `subsystems`, and `aircraft` arrays. Every array item has `id`, `name`, and `value`.

```json
{
  "generatedAt": "2026-10-20T10:00:00",
  "metric": "MTBF_HOURS",
  "lrus": [{ "id": 7, "name": "Flight Computer", "value": 4.0 }],
  "subsystems": [],
  "aircraft": []
}
```

## Supporting routes

`GET /api/reliability/metrics` returns all three formulas and their source counts together.

`GET /api/reliability/reports/current` returns the persisted current-month LRU reporting projection. It is refreshed nightly by the reporting job and is keyed by `(lru_id, period_start)`.

## Operating-hour convention

One `SORTIE_COMPLETED` synthetic event represents one simulated operating hour. This convention is returned by the combined metrics endpoint and is intentionally visible to prevent synthetic evidence being misrepresented as real operating data.
