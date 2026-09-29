# Developer Guide: Procedure Snapshots and Frozen Coverage Reports

| Document information | Details |
|---|---|
| Jira | AI-220 / T11-DOC; AI-124 / AI-223 |
| Status | Documents shipped Week 11 behaviour |
| Last updated | 18 September 2026 |

## Purpose

`ProcedureSnapshot` and `CoverageReport` preserve the exact evidence used
when a procedure version is submitted for review. They avoid a historical
approval decision changing merely because a shared Interface, Connector,
Signal, Fault Template, or FMEA mapping is edited later.

This is separate from `procedure_history`: history records content revisions;
the snapshot/report pair records the submission-time coverage evidence for the
specific reviewed version.

## Lifecycle and version relationship

```text
TestProcedure version N (DRAFT)
  POST /api/procedures/{id}/submit-for-review
      -> TestProcedure version N+1 (IN_REVIEW)
      -> ProcedureSnapshot(procedure, N+1)
      -> CoverageReport(snapshot, procedure, N+1, reportVersion 1)
  POST /api/procedures/{id}/approve
      -> reads CoverageReport for N+1, then IN_REVIEW -> APPROVED when eligible
```

`TestProcedureService.submitForReview` increments the procedure version and,
in the same transaction, delegates to
`ProcedureSnapshotService.createSubmissionArtifacts`. That service writes one
snapshot for `(test_procedure_id, procedure_version_number)` and one initial
coverage report for that snapshot. Repeated attempts cannot create a duplicate:
the database has unique constraints for the snapshot version and for report
versioning.

Snapshots and reports can be retrieved by the reviewed procedure version:

- `GET /api/procedures/{id}/snapshots/{versionNumber}`
- `GET /api/procedures/{id}/coverage-reports/{versionNumber}`
- `POST /api/procedures/{id}/approve`

All routes remain subject to the application-wide JWT authentication policy.

## Frozen data and calculation

The snapshot payload stores the procedure identity, version, status, name,
description, LRU identity/name, ordered Test Step response data, and the
relevant embedded shared-asset values. It also stores a coverage context of
the LRU's Interface, Signal, Fault and FMEA mapping IDs at submission time.

`ProcedureSnapshotService` calculates the report by reading that serialized
payload, not the live shared-asset repositories. The report payload contains
the category scores, composite score where all active categories are available,
per-category `thresholdResults`, unavailable-category reasons, snapshot id,
and procedure version. The coverage calculation uses the existing
`CoverageEngine` and its enabled coverage-rule configuration.

Therefore later edits to a shared asset cannot mutate either persisted payload
or the approval decision for the already submitted version. New submissions
produce evidence for their new version instead.

## Threshold gating and approval

The existing threshold source is `rules_configuration.threshold` for the
enabled coverage rules, administered through
`GET`/`PUT /api/admin/coverage/scoring-config`. There is no separate global
approval-composite threshold in the current model. At report creation,
`CoverageEngine` compares each measured category score with its configured
threshold and persists the result in `thresholdResults`.

`POST /api/procedures/{id}/approve` first applies the existing
`ApprovalStateMachine` legality check (`IN_REVIEW -> APPROVED`). It then reads
the frozen report for the procedure's current reviewed version; it does not
recalculate coverage. Approval is rejected with the standard `ApiError`
`409 Conflict` response if any persisted, measured threshold result is
`false`. `null` is retained as the engine's established unavailable/not-
applicable result, rather than being reinterpreted as a new approval rule.
On rejection, no approval state or approval-history transition is written.

If all measured thresholds are met, the procedure transitions to `APPROVED`
using the same state machine. Existing illegal transitions remain `409
Conflict`, and endpoint authentication is unchanged.

## Database model and immutability

Flyway migration `V29__procedure_snapshot_and_coverage_report.sql` defines:

- `procedure_snapshot`, keyed by `id`, with unique
  `(test_procedure_id, procedure_version_number)` and a foreign key to
  `test_procedures` with `ON DELETE CASCADE`.
- `coverage_report`, keyed by `id`, with its snapshot foreign key and unique
  report-version constraints. `report_version` currently starts at `1`.

Both JPA entities are annotated `@Immutable`; their persisted columns are
also `updatable = false`. They are append-only submission artifacts, not live
views. Deleting a procedure cascades its snapshot and report rows.

## Future-change considerations

- Do not replace frozen payload calculation with a live coverage evaluation
  at approval time.
- Any new report version must preserve the existing unique constraints and
  explicitly define which report version the approval boundary consumes.
- If a future global composite approval threshold is introduced, add it to
  the configuration model and freeze its evaluated decision with the report;
  do not hard-code it in the controller or frontend.
- Keep state transitions in `ApprovalStateMachine`; coverage gating is a
  precondition at the approval boundary, not a second workflow.
The frozen payload also stores the submitted LRU name and subsystem identity,
plus a value copy of every measurable Interface, Signal, Fault, and FMEA
element. The report derives its `gaps` and `coverageClassifications` from those
copies, never from the live asset tables. Therefore a later rename or edit of
an Interface cannot change either the historical gap description or the
subsystem context shown for the submitted revision.
