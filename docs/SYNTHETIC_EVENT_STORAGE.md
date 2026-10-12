# Synthetic LRU Instance and Event Storage

AI-248 adds the storage boundary for simulated fleet reliability evidence. It
does not represent aircraft telemetry and must not be used to store production
or customer data.

## Event generation and traceability (AI-245 / AI-246)

`POST /api/reliability/synthetic-events/generate?aircraftId={id}` creates
simulated fleet events from persisted validation runs. It is restricted to
`ADMIN`, `SYSTEM_ENGINEER`, and `RELIABILITY_ENGINEER` roles.

For every validation run with an available procedure-history LRU, the generator
creates or reuses one clearly labelled `SyntheticLruInstance` for the selected
aircraft and LRU. It writes:

- `SORTIE_COMPLETED` for every source validation run;
- `FAILURE_DETECTED` when the run contains a mismatch, outlier, or missing
  reading; and
- `REPAIR_COMPLETED` after a generated failure event.

Every event stores its `source_validation_run_id` and immutable JSON payload.
The `(synthetic_lru_instance_id, source_validation_run_id, event_type)` unique
index makes repeated requests idempotent: existing synthetic evidence is
reported, never duplicated. These records are demonstration data only and are
not live aircraft telemetry or maintenance records.

## `synthetic_lru_instance`

Each row represents a clearly labelled simulated physical instance of an
existing engineering `lru`. `instance_code` is unique, while `generator_seed`
makes future deterministic event generation reproducible. An instance can be
`ACTIVE` or `RETIRED`.

## `event_log`

Each append-only event references both its synthetic instance and its LRU.
The composite foreign key `(synthetic_lru_instance_id, lru_id)` prevents an
event from being associated with an LRU other than the one the instance belongs
to. Event types are intentionally open vocabulary so the later generator can
add credible domain scenarios without a database migration.

The primary reliability-query index is:

```text
(lru_id, event_type, timestamp DESC)
```

This supports chronological event counts, recurring-failure trends, and
event-type analysis per LRU. `payload` is optional evidence context; it must
remain synthetic and non-sensitive.
