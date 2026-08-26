# Week 7 Implementation Report: Rollback Endpoint + Version API Documentation

| Document Information | Details |
|---|---|
| Jira | T7-B3 / AI-187 (Rollback Endpoint), T7-DOC (API Documentation) |
| Calendar | 2026-08-17 to 2026-08-21 |
| Branch | `feature/rollback-endpoint` |

## Implementation Summary

Added a non-destructive rollback endpoint to the existing Procedure
Builder / Version Management backend, integrated it with the existing
field-level Version Comparison engine and the existing generic audit
infrastructure, added a small rollback UI to the existing Version
Comparison panel in `ProcedureBuilderPage`, and documented both the new
endpoint and the two pre-existing but previously undocumented endpoints
(`/history`, `/compare`) that T7-DOC calls for.

No existing Test Block, Procedure Builder, Version Comparison, or
approval-workflow functionality was rewritten, replaced, or had its
existing API contract changed.

## Existing Code Reused

The project already had a complete, working versioning foundation (Week
6 / T6-B1, AI-173) and comparison engine (Week 7 / T7-B1, AI-182). This
work builds directly on top of it rather than introducing anything
parallel:

- **`TestProcedure` + `ProcedureHistory`**: the existing live/immutable-
  snapshot model. Rollback writes a `ProcedureHistory` row exactly the
  same way `TestProcedureService.save()`/`update()` already do (JSON
  `stepsSnapshot` via `TestStepResponse`/`ObjectMapper`, not a new
  storage scheme).
- **`TestProcedureService`**: rollback lives here as
  `rollbackToRevision(Long, Integer)`, a third content-changing write
  alongside the existing `save()`/`update()`, sharing the same
  `@Transactional` boundary convention, the same `requireProcedure`
  helper, the same `writeRevision`/`orderedSteps` helpers, and the same
  FK-resolution-by-id pattern already used by `replaceSteps()`.
- **`VersionComparisonService`**: untouched. Rollback was designed
  specifically so its output plugs into the existing step-matched-by-id,
  field-level comparison without any change to that service.
- **`AuditService`**: untouched (`record(...)` reused as-is). Rollback
  calls it directly with `action = "ROLLBACK"` rather than going through
  `AuditAspect`'s generic pointcut, since `rollbackToRevision` doesn't
  match `AuditAspect`'s `save`/`update`/`delete` method-name pointcut.
- **`GlobalExceptionHandler`**: untouched. Rollback reuses the existing
  `NoSuchElementException` -> 404 and `IllegalArgumentException` -> 400
  mappings; no new exception types were introduced.
- **`SecurityConfig`**: untouched. The new endpoint sits under the
  already-authenticated `/api/procedures/**` path; no new role was
  invented (the codebase has no granular per-endpoint RBAC anywhere yet,
  so none was added here either).
- **`TestProcedureController`**: the existing controller gained one new
  method; the existing `/history` and `/history/{versionNumber}` routes
  were left exactly as-is, and the rollback route was nested under them
  (`/history/{versionNumber}/rollback`) following that same convention,
  rather than the `/versions/{id}/rollback` path the ticket's *example*
  guessed (the ticket text explicitly allows this: "do not blindly
  create this exact endpoint if the existing project architecture uses
  another consistent route").

## Rollback Design

`TestProcedureService.rollbackToRevision(procedureId, sourceVersionNumber)`:

1. Validates `sourceVersionNumber` is a positive integer (else `400`).
2. Loads the `TestProcedure` (else `404`).
3. Loads the `ProcedureHistory` row for `(procedureId, sourceVersionNumber)`
   (else `404` -- this lookup is already scoped by `procedureId`, so a
   revision number belonging only to a *different* procedure is
   correctly treated as not found for this one).
4. Treats that `ProcedureHistory` row as **read-only input** -- no setter
   is ever called on it, and it is never passed to `save()`.
5. Replays its `name`/`description`/`lruId` onto the live `TestProcedure`,
   and its `stepsSnapshot` onto the live `TestStep` rows via a new
   `restoreSteps()` helper (see below).
6. Saves the procedure, increments `versionNumber` to `current + 1`
   (never reusing the source revision's own number), and calls the
   existing `writeRevision()` helper to append a brand new
   `ProcedureHistory` row.
7. Explicitly writes a `ROLLBACK` audit entry (see Audit behavior below).
8. Returns the live procedure (`TestProcedureResponse`), same shape
   `update()` already returns, with `versionNumber` = the new revision.

The whole method is `@Transactional`; any failure (missing referenced
master data, a bad snapshot, etc.) leaves no partial revision and no
partial `TestStep` changes behind, matching `save()`/`update()`'s
existing atomicity guarantee.

**Why `restoreSteps()` is a separate method from the existing
`replaceSteps()`, not a reuse of it:** `replaceSteps()` treats a step id
in the request that no longer exists live as a client error and throws
-- correct for an ordinary Procedure Builder edit, where the client
should never reference a stale id. Rollback is different: the historical
snapshot is *allowed* to describe a step that a later edit has since
deleted. `restoreSteps()` therefore:

- reuses a snapshot step's original id **in place** when that id is
  still a live `TestStep` row (so it's just an in-place field restore),
  which is what makes `compare(sourceRevision, rollbackResult)` report
  that step as `UNCHANGED` rather than a spurious removed+added pair;
- creates a **new** row when the original id is no longer live (the step
  was deleted since the source revision), recovering its content but
  necessarily under a new id -- a documented, inherent limitation given
  the existing comparison engine matches steps by id, not content (see
  Known Limitations);
- deletes any currently-live step not present in the source snapshot,
  mirroring `replaceSteps()`'s existing delete-if-absent behavior.

**Why approval status is left untouched:** exactly like `update()`
already leaves it untouched on every other content-changing save, the
new revision carries forward whatever approval state the procedure
already had. Rollback does not invent a new approval transition or
auto-approve its result -- there was no existing rule requiring that,
and inventing one would have violated "do not invent a new approval
vocabulary" (Sec 9 of the ticket).

## API

### `GET /api/procedures/{id}/history` (pre-existing, now documented)

Lists every revision for a procedure, oldest first. Unchanged.

### `GET /api/procedures/{id}/compare?fromVersion=&toVersion=` (pre-existing, now documented)

Field-level, step-by-step comparison of two revisions (steps matched by
persisted `TestStep.id`). Unchanged -- this report only adds the
documentation T7-DOC asked for; see
`docs/DEVELOPER_GUIDE_PROCEDURE_AUTHORING.md` Section 10a for the full
request/response contract.

### `POST /api/procedures/{id}/history/{versionNumber}/rollback` (new -- AI-187 / T7-B3)

Full contract (request, response, numbering, Test Step identity
behavior, approval status behavior, audit behavior, error responses,
security) documented in
`docs/DEVELOPER_GUIDE_PROCEDURE_AUTHORING.md` Section 10b.

## Database Changes

One new migration, genuinely required (not optional):

- **`V17__audit_log_rollback_action.sql`** -- widens the existing
  `audit_log.action` `CHECK` constraint (added in `V6__create_audit_log.sql`,
  originally `CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))`) to also
  allow `'ROLLBACK'`. Without this, the `ROLLBACK` audit write in Sec 7
  of the requirements would fail a database constraint check on every
  rollback. Backward compatible: existing rows and the existing three
  action values are unaffected; this only adds a fourth permitted value.
  No other schema change was required -- `ProcedureHistory` already
  stores everything rollback needs.

## Tests

### `TestProcedureServiceTest.java` (existing file, extended)

The service constructor gained an `AuditService` parameter (rollback's
own dependency), so the existing test's constructor call was updated to
pass a new `@Mock AuditService auditService` -- the only change to any
pre-existing test in this file. All pre-existing tests are otherwise
untouched. New unit tests added (Mockito-based, no DB):

- `rollbackCreatesNewRevisionFromSourceContentAndLeavesHistoryUntouched`
  -- new revision number, source revision object never mutated, live
  step id reused when still present.
- `rollbackRecreatesAStepWhoseOriginalIdWasDeletedByALaterEdit` -- the
  documented recreate-on-missing-id edge case.
- `rollbackAlwaysCreatesANewRevisionEvenIfContentMatchesCurrent` -- no
  no-op-resave suppression for rollback.
- `rollbackRecordsARollbackAuditEntry` -- `auditService.record(...)`
  called with `action = "ROLLBACK"`.
- `rollbackAuditFailureDoesNotFailTheRollback` -- an audit-write
  exception is swallowed; the rollback itself still succeeds.
- `rollbackThrowsWhenProcedureDoesNotExist` / `...RevisionDoesNotExist`
  / `...RevisionBelongsToADifferentProcedure` -- `NoSuchElementException`,
  and no `ProcedureHistory` write occurs.
- `rollbackRejectsAnInvalidVersionNumber` -- `IllegalArgumentException`
  for `0` and `null`, no repository interaction at all.

### `ProcedureRollbackIntegrationTest.java` (new file)

`@SpringBootTest @ActiveProfiles("synthetic-data")`, following the exact
same convention as the existing `VersionComparisonServiceTest.java` and
`ProcedureHistoryWriteThroughVerificationTest.java` -- real repositories
against the project's Postgres synthetic-data database, driving
revisions through the real `TestProcedureService` save/update/rollback
flow (no hand-inserted `ProcedureHistory` rows):

- `rollbackCreatesANewRevisionAndLeavesAllPriorRevisionsUntouched` --
  full v1->v2->v3->rollback(v1)->v4 flow; asserts v1/v2/v3 snapshots are
  byte-for-byte unchanged and v4 exists as its own revision.
- `comparingV1AndTheRollbackRevisionShowsNoFieldLevelDifferences` -- the
  Sec 11 acceptance criterion: `compare(v1, v4)` reports `UNCHANGED`
  with zero field changes.
- `comparingTheSourceRevisionAndTheRollbackRevisionShowsTheExpectedDifferences`
  -- `compare(v3, v4)` reports `CHANGED` with the field reverting to
  v1's value.
- `rollbackWritesARollbackAuditLogEntry` -- a real `audit_log` row with
  `action = 'ROLLBACK'` is queryable afterwards (this is also what
  exercises the `V17` migration end-to-end).
- `rollbackToAMissingRevisionReturns404Equivalent` /
  `rollbackOnAMissingProcedureReturns404Equivalent` /
  `rollbackUsingARevisionThatBelongsToAnotherProcedureFails` /
  `rollbackRejectsAnInvalidRevisionNumber`.
- `rollingBackToTheCurrentRevisionStillCreatesANewRevision` -- Sec 6/9:
  rollback is never suppressed as a no-op.
- `rollbackDoesNotInventANewApprovalStatus`.

## Validation

**Build/test execution: NOT run.** This environment has no outbound
network access (Maven could not download its own wrapper distribution,
let alone the project's dependencies) and no local Maven dependency
cache, and the project's `@SpringBootTest` integration tests
additionally require a live PostgreSQL instance seeded via the
`synthetic-data` Spring profile, per the project's own existing test
setup (`ProcedureHistoryWriteThroughVerificationTest`,
`VersionComparisonServiceTest`, `SyntheticDataConstraintTest`, etc. all
already require this and were not runnable here either, before any
change in this patch). This is a limitation of the tool environment,
not something specific to this change.

Every new/changed method, field, and getter/setter referenced by the new
code was cross-checked directly against the actual entity/DTO/repository
source in the ZIP (not assumed) to catch compile errors without a
build -- see "Files Changed" for exactly what was touched. No test
result is claimed as PASS; this report states plainly that build and
test execution could not be verified in this environment, and that
running `mvn test` (with a reachable Maven repository and a running
Postgres instance on the `synthetic-data` profile) is the necessary next
step before merge.

## Files Changed

**Backend**

- `backend/src/main/java/com/adtech/iaterap/backend/service/TestProcedureService.java`
  -- added `AuditService` dependency; added `rollbackToRevision()`,
  `restoreSteps()`, `deserializeSnapshotSteps()`,
  `recordRollbackAudit()`, `safeSerialize()`.
- `backend/src/main/java/com/adtech/iaterap/backend/controller/TestProcedureController.java`
  -- added `POST /{id}/history/{versionNumber}/rollback`.
- `backend/src/main/resources/db/migration/V17__audit_log_rollback_action.sql`
  -- new migration (widen `audit_log.action` check constraint).
- `backend/src/test/java/com/adtech/iaterap/backend/service/TestProcedureServiceTest.java`
  -- constructor call updated for the new `AuditService` dependency;
  8 new rollback unit tests added.
- `backend/src/test/java/com/adtech/iaterap/backend/service/ProcedureRollbackIntegrationTest.java`
  -- new file, 10 end-to-end tests.

**Frontend**

- `frontend/src/services/versionComparisonService.js` -- added
  `rollbackToRevision(procedureId, versionNumber)`.
- `frontend/src/pages/ProcedureBuilderPage.jsx` -- added a revision list
  with a "Rollback to this revision" button to the existing Version
  Comparison panel, and the `handleRollback` handler wiring it to
  `loadAll()`/history refresh on success.

**Documentation**

- `docs/DEVELOPER_GUIDE_PROCEDURE_AUTHORING.md` -- added Section 10a
  (`/compare`, previously undocumented despite already being
  implemented) and Section 10b (rollback); updated the Section 14
  endpoint summary table and the document header.
- `docs/WEEK_7_ROLLBACK_IMPLEMENTATION.md` -- this report.

**Deliberately not touched:** `VersionComparisonService`,
`VersionComparisonController`, any Test Block class, `ApprovalStateMachine`,
`SecurityConfig`, `GlobalExceptionHandler`, `AuditAspect`, `AuditService`,
any migration prior to V17, `API_PROCEDURES_TEST_STEPS.md` (a stale
pre-implementation draft already superseded by the developer guide, per
that guide's own header note).

## Known Limitations

- **Step identity after a delete-then-rollback.** If a Test Step present
  in the source revision was deleted by a later edit before the
  rollback, rollback recreates it with a **new** id (its old id no
  longer exists to reuse). Content is fully preserved, but
  `compare(sourceRevision, rollbackResult)` will report that specific
  step as a removed/added pair rather than `UNCHANGED`, because the
  existing `VersionComparisonService` matches steps by id, not content.
  This only affects steps that were deleted and is inherent to the
  existing id-based comparison design; steps that were merely edited (not
  deleted) in between are unaffected and compare as `UNCHANGED` as
  required by Sec 11 of the ticket.
- **No optimistic locking.** The existing `TestProcedure` entity has no
  `@Version` column and `update()` has none either; rollback follows the
  same existing convention rather than introducing new concurrency
  control unilaterally. A true concurrent-rollback race is no more (and
  no less) possible than a concurrent `update()` race already is today.
- **No per-endpoint RBAC.** The codebase has no granular
  authorization beyond "authenticated or not" anywhere yet, so rollback
  is gated the same way every other `/api/procedures/**` write already
  is. Adding finer-grained roles was out of scope per Sec 13 ("if a new
  permission is genuinely required... document it" -- none was found to
  be genuinely required here, since no existing endpoint has one either).
- **Build/tests unverified in this environment**, as detailed under
  Validation above.

## Git

- Branch: `feature/rollback-endpoint`
- Commit message: `feat(procedure): implement non-destructive procedure rollback`
  (documentation changes are small enough to include in the same commit,
  following the existing project convention of doc updates riding along
  with the feature that needed them)
- PR title: `feat(procedure): rollback endpoint and version API documentation`
- Jira references: T7-B3, AI-187, T7-DOC
