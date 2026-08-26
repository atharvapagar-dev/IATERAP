# Developer Guide: Authoring a Procedure

| Document Information | Details |
|---|---|
| Jira | AI-178 [T6-DOC] Developer Guide: Authoring a Procedure; AI-187 / T7-B3 / T7-DOC (Rollback + Version API docs) |
| Status | Documents shipped behaviour (Sprint 2 / Week 6; extended Week 7) |
| Last Updated | 26 August 2026 |

This guide documents the **actually implemented** Procedure Builder,
Version Management, Version Comparison, Rollback, and Reviewer Comments
workflow. It supersedes the
earlier draft in `API_PROCEDURES_TEST_STEPS.md` (AI-169) where the two
disagree -- that document was written before the controllers existed and
proposed a `/api/v1/...` path with a `{data, meta, errors}` envelope and
cursor pagination. The implementation below instead follows the
convention every other controller in this codebase already uses:
unversioned `/api/...` paths returning plain JSON bodies (see
`ConnectorController`, `FaultTemplateController`, etc.), no envelope, and
no pagination (list endpoints return everything, matching e.g.
`GET /api/connectors`).

## 1. Opening the Procedure Builder

Log in, then click **Procedures** in the top navigation. This opens
`ProcedureListPage` (`/procedures`), which lists every existing
`TestProcedure` with its current approval status, version number, and
step count.

## 2. Creating or opening a Procedure

- **New procedure**: click **New Procedure**. This routes to
  `/procedures/new`, which renders `ProcedureBuilderPage` in create mode
  (no procedure loaded yet).
- **Existing procedure**: click **Open in Builder** on any row. This
  routes to `/procedures/{id}`, which loads the procedure and its steps
  via `GET /api/procedures/{id}`.

## 3. Selecting the LRU

Every Procedure belongs to one LRU. Pick it from the **LRU** dropdown
first -- this scopes every typed reference dropdown in each Test Step
Card to that LRU:

- **Signal**, **Protocol**, and **Fault Category** are fetched from the
  existing `by-lru` backend endpoints
  (`/signal-libraries/by-lru/{lruId}`, `/protocol-tags/by-lru/{lruId}`,
  `/fault-templates/by-lru/{lruId}`).
- **Interface** and **Connector** are fetched in full and filtered
  client-side to the selected LRU (there is no `by-lru` endpoint for
  these two yet; adding one was out of scope -- see Part M/Part L: "do
  not create a giant new API surface").

This is a UI convenience only. The backend's existing FK constraints
remain the authority -- the UI filtering does not replace or weaken them.

## 4. Adding a Test Step

Click **Add Test Step**. A new `TestStepCard` appears with empty typed
reference selects and text fields:

- Interface, Connector, Signal, Protocol, Fault Category (all required)
- Expected Behaviour, Acceptance Criteria, Pass/Fail Criteria (required)
- Expected Range (optional)

## 5. Editing a Test Step

Edit any field directly in its card. Changes are held in local component
state until you click **Save Procedure** -- nothing is persisted
per-keystroke.

## 6. Reordering steps

Use the **↑ / ↓** buttons in a card's header. This reorders the local
step array; the resulting array index (1-based) becomes each step's
`executionOrder` on save. This reuses the existing `execution_order`
column on `test_steps` (added in V10) -- no new ordering column was
introduced.

## 7. Removing a step

Click **Remove** on a card. On save, any existing step id that is no
longer present in the payload is deleted server-side (see "Save
semantics" below).

## 8. Saving

Click **Save Procedure**. This sends the full procedure (name,
description, LRU, and the complete ordered step list) to:

- `POST /api/procedures` for a new procedure, or
- `PUT /api/procedures/{id}` for an existing one.

**Save semantics** (`TestProcedureService`): the backend diffs the
incoming step list against the current `test_steps` rows for that
procedure --

- a step in the payload **without** an id is **created**
- a step in the payload **with** an id is **updated in place**
- an existing step **not present** in the payload is **deleted**
- every step's position in the payload array becomes its
  `execution_order`

The procedure fields and all step changes are written in one
`@Transactional` method, together with the revision write described
next -- so a failure never leaves a half-saved procedure or an orphaned
revision.

## 9. Submitting a procedure for review

After saving an existing procedure in `DRAFT`, click **Submit for Review**.
The Builder calls `POST /api/procedures/{id}/submit-for-review`; on success
it refreshes the displayed status and version to `IN_REVIEW`.

The backend remains authoritative for the transition. If the procedure has
no saved Test Steps, has a structurally invalid step, or is no longer in
`DRAFT`, the Builder displays the returned validation or conflict message.
Save any local edits before submitting: the action transitions the persisted
procedure and does not implicitly save unsaved form changes.

## 10. How revision creation works

Every `save`/`update` call recomputes a JSON snapshot of the procedure's
current fields plus its full ordered step list, and compares it against
the most recent `procedure_history` row for that procedure (by
`test_procedure_id`). If anything differs, the backend:

1. Increments `TestProcedure.versionNumber`.
2. Inserts a new, **never-updated** `procedure_history` row with that
   version number and the snapshot.

If the recomputed snapshot is identical to the latest revision (a
no-op resave), no new revision is written -- this keeps history
deterministic per PART Q ("do not create unnecessary history rows").

Two consecutive content-changing saves therefore always produce two
distinct, independently retrievable `procedure_history` rows, and the
first one is never mutated by the second (Part R).

## 11. Retrieving revision history

- `GET /api/procedures/{id}/history` -- every revision for a procedure,
  ordered oldest to newest.
- `GET /api/procedures/{id}/history/{versionNumber}` -- one specific
  revision.

The Procedure Builder shows the full history table under **Revision
History** once you've saved at least once.

### Verifying directly with SQL

```sql
SELECT id, version_number, name, approval_status, created_by, created_at
FROM procedure_history
WHERE test_procedure_id = :procedureId
ORDER BY version_number;
```

Two saves against the same procedure should show two rows here, with
`steps_snapshot` (a JSON column, omitted above for readability)
differing between them, and the earlier row's `steps_snapshot`
unchanged after the later save.

## 10a. Comparing two revisions (AI-182 / T7-B1)

`GET /api/procedures/{id}/compare?fromVersion={n}&toVersion={m}`

Both query parameters are required and refer to `procedure_history`
version numbers for that procedure. Comparison is **structured and
field-level**, never a text diff: each Test Step in `fromVersion` is
matched against a Test Step in `toVersion` **by persisted `TestStep.id`**
(not by position), and every one of the fields listed below is compared
individually.

**Request**

```
GET /api/procedures/42/compare?fromVersion=1&toVersion=3
Authorization: Bearer <jwt>
```

**Response** (`VersionComparisonResponse`)

```json
{
  "procedureId": 42,
  "procedureName": "Altimeter Calibration",
  "fromVersion": 1,
  "toVersion": 3,
  "addedStepCount": 1,
  "removedStepCount": 0,
  "changedStepCount": 1,
  "unchangedStepCount": 1,
  "steps": [
    {
      "changeType": "CHANGED",
      "fromStep": { "id": 101, "expectedBehaviour": "...", "...": "..." },
      "toStep":   { "id": 101, "expectedBehaviour": "...", "...": "..." },
      "fieldChanges": [
        {
          "fieldName": "expectedBehaviour",
          "oldValue": "Reading stabilizes within 2s",
          "newValue": "Reading stabilizes within 1s",
          "changeType": "CHANGED"
        }
      ]
    },
    {
      "changeType": "ADDED",
      "fromStep": null,
      "toStep": { "id": 108, "expectedBehaviour": "...", "...": "..." },
      "fieldChanges": []
    },
    {
      "changeType": "UNCHANGED",
      "fromStep": { "id": 102, "...": "..." },
      "toStep":   { "id": 102, "...": "..." },
      "fieldChanges": []
    }
  ]
}
```

- `changeType` per step is one of `ADDED`, `REMOVED`, `CHANGED`,
  `UNCHANGED`.
- `ADDED` / `REMOVED` steps carry no `fieldChanges` entries (there is
  nothing to diff field-by-field against).
- The compared fields are every engineering field on `TestStepResponse`:
  `expectedBehaviour`, `expectedRange`, `calibrationRequirement`,
  `safetyNotes`, `acceptanceCriteria`, `preconditions`, `dependencies`,
  `postConditions`, `passFailCriteria`, `expectedObservation`,
  `engineerRemarks`, plus the typed reference names (Interface,
  Connector, Signal, Protocol, Fault Category).

**Errors**

| Status | Cause |
|---|---|
| 400 | `fromVersion` or `toVersion` missing/non-numeric, or otherwise invalid |
| 404 | procedure does not exist, or either version number has no matching revision for that procedure |

**Auth**: same JWT bearer requirement as every other `/api/procedures/**`
endpoint; no extra role.

## 10b. Rolling back to a previous revision (AI-187 / T7-B3)

`POST /api/procedures/{id}/history/{versionNumber}/rollback`

Rolls the procedure back to the content recorded in `versionNumber`.
**Non-destructive**: rollback never edits, removes, or renumbers
`versionNumber` or any other existing revision. It replays that
revision's name/description/LRU/Test Steps onto the live procedure and
appends a brand new revision with the next version number -- the exact
same "write a new immutable snapshot" mechanism `PUT /api/procedures/{id}`
already uses for every other content-changing save (Section 9 above),
just fed from a historical snapshot instead of the request body.

```text
v1 -> v2 -> v3

rollback(procedureId, 1)

Result:
v1  unchanged
v2  unchanged
v3  unchanged
v4  created, content matches v1
```

**Request**

```
POST /api/procedures/42/history/1/rollback
Authorization: Bearer <jwt>
```

No request body.

**Response** (`TestProcedureResponse` -- the live procedure after
rollback, same shape `PUT /api/procedures/{id}` returns)

```json
{
  "id": 42,
  "lruId": 7,
  "name": "Altimeter Calibration",
  "description": "...",
  "approvalStatus": "DRAFT",
  "versionNumber": 4,
  "steps": [ { "id": 101, "expectedBehaviour": "...", "...": "..." } ]
}
```

`versionNumber` in the response is the **newly created** revision
number. The full persisted revision is retrievable afterwards via
`GET /api/procedures/{id}/history/{versionNumber}` (Section 10 above).

**Revision numbering**: always the next number after the procedure's
current version (`current + 1`), matching the numbering scheme already
used by every save/update -- the target revision's own number is never
reused, and rollback always writes a new revision even in the edge case
where the target's content happens to already match the current live
content (rollback is an explicit action, unlike a plain resave, so the
existing no-op-resave suppression described in Section 9 does not apply
here).

**Test Step identity across rollback**: where a source revision's step
id is still a live `TestStep` row, rollback reuses that same id (only
its field values are restored) so Version Comparison (Section 10a)
continues to match it by id and correctly reports it `UNCHANGED` when
compared against the source revision. Where the source revision
describes a step whose id was since deleted by a later edit, rollback
recreates it as a new row (necessarily a new id, since the old one no
longer exists) -- comparing the source revision against the rollback
result will show that specific step as a removed/added pair rather than
`UNCHANGED` in that edge case, even though its content is identical.

**Approval status**: left untouched, exactly like every other
content-changing save -- rollback does not auto-approve the new revision
or invent a new approval transition. The new revision goes through the
normal approval lifecycle from whatever state the procedure was
already in.

**Audit**: every rollback writes an `audit_log` row with
`action = 'ROLLBACK'` (a value added to the `audit_log.action` check
constraint by `V17__audit_log_rollback_action.sql`, alongside the
existing `CREATE`/`UPDATE`/`DELETE` values `AuditAspect` already writes
for every other save/update/delete). `entityName` is `TestProcedure`,
`entityId` is the procedure id, `beforeState` is the source revision
(`ProcedureHistoryResponse`), and `afterState` is the resulting live
procedure (`TestProcedureResponse`). Unlike the generic aspect-driven
audit entries, this one is written explicitly inside
`TestProcedureService#rollbackToRevision` (its method name doesn't match
`AuditAspect`'s `save`/`update`/`delete` pointcut), so the action is
recorded precisely as `ROLLBACK` rather than a generic `UPDATE`. An
audit-write failure is logged and swallowed -- it can never fail or roll
back an already-successful rollback.

**Errors**

| Status | Cause |
|---|---|
| 400 | `versionNumber` is not a positive integer |
| 404 | procedure does not exist |
| 404 | `versionNumber` has no matching revision for that procedure (including a version number that only exists for a *different* procedure) |

A failed rollback (any of the above, or an unexpected error) leaves the
database completely unchanged -- the whole operation runs in one
`@Transactional` service method, same as `save`/`update`.

**Auth**: same JWT bearer requirement as every other
`/api/procedures/**` endpoint (`SecurityConfig` requires authentication
for all non-`/api/auth/**` routes); no separate role exists for rollback
specifically, matching the fact that no granular per-endpoint RBAC exists
anywhere else in this codebase yet.

**Frontend**: `ProcedureBuilderPage`'s existing **Version Comparison**
panel now also lists every revision with a **Rollback to this revision**
button (disabled for the current revision), calling
`versionComparisonService.rollbackToRevision(procedureId, versionNumber)`
and reloading the procedure/steps/history on success.

## 12. How Reviewer Comments attach to revisions

A comment always names a `procedureHistoryId` (required) and, in the
Procedure Builder, is posted against whatever revision is currently the
*latest* one for the procedure being viewed:

```
POST /api/reviewer-comments
{
  "procedureHistoryId": 42,
  "testStepId": null,
  "commentText": "..."
}
```

`GET /api/reviewer-comments/by-revision/{procedureHistoryId}` lists every
comment on that revision, shown under **Reviewer Comments (latest
revision)** in the Builder.

## 13. How comments can attach to Test Steps

Pass a `testStepId` alongside `procedureHistoryId` to anchor a comment to
one specific step within that revision. In the UI, pick a step from the
**Anchor to Test Step** dropdown (only steps that already have a backend
id -- i.e. have been saved at least once -- are selectable) before
submitting the comment.

## 14. Validation rules

**Backend-authoritative, always enforced:**

- All five typed references on a Test Step (Interface, Connector,
  Signal, Protocol, Fault Category) must resolve to existing rows, or
  the save fails with a 404-style not-found error.
- A comment's `procedureHistoryId` must reference an existing revision.
- A comment's optional `testStepId`, if present, must satisfy **both**:
  1. that step's *current* `TestProcedure` matches the revision's
     `TestProcedure`, and
  2. that step's id is actually present in that specific revision's
     frozen `steps_snapshot` --

  otherwise the request is rejected with `400 Bad Request`
  (`IllegalArgumentException`, via the existing `GlobalExceptionHandler`).
  This prevents anchoring a comment to revision 2 while pointing at a
  step that only existed in revision 1 (PART W).
- Illegal approval-state transitions (see `ApprovalStateMachine`) return
  `409 Conflict`.

**Frontend, convenience only:** required-field checks before submit, and
LRU-scoped dropdowns as described in Section 3. None of this replaces
the backend checks above.

## 15. API endpoint summary

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/procedures` | List all procedures |
| GET | `/api/procedures/{id}` | Get one procedure + its ordered steps |
| POST | `/api/procedures` | Create a procedure (steps included) -> revision 1 |
| PUT | `/api/procedures/{id}` | Update a procedure (steps included) -> new revision if content changed |
| POST | `/api/procedures/{id}/submit-for-review` | Transition a structurally valid DRAFT procedure to IN_REVIEW |
| GET | `/api/procedures/{id}/history` | List all revisions, oldest first |
| GET | `/api/procedures/{id}/history/{versionNumber}` | Get one revision |
| GET | `/api/procedures/{id}/compare?fromVersion=&toVersion=` | Field-level, step-by-step comparison of two revisions |
| POST | `/api/procedures/{id}/history/{versionNumber}/rollback` | Roll back to a revision -> new, non-destructive revision |
| POST | `/api/reviewer-comments` | Create a comment on a revision (+ optional step) |
| GET | `/api/reviewer-comments/by-revision/{procedureHistoryId}` | List comments on a revision |

All endpoints require the existing JWT authentication; none of them
introduce new roles or weaken `SecurityConfig`.
