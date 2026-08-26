# Developer Guide: Authoring a Procedure

| Document Information | Details |
|---|---|
| Jira | AI-178 [T6-DOC] Developer Guide: Authoring a Procedure |
| Status | Documents shipped behaviour (Sprint 2 / Week 6) |
| Last Updated | 25 August 2026 |

This guide documents the **actually implemented** Procedure Builder,
Version Management, and Reviewer Comments workflow. It supersedes the
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
| POST | `/api/reviewer-comments` | Create a comment on a revision (+ optional step) |
| GET | `/api/reviewer-comments/by-revision/{procedureHistoryId}` | List comments on a revision |

All endpoints require the existing JWT authentication; none of them
introduce new roles or weaken `SecurityConfig`.
