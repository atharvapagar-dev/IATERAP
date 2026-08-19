# Developer Guide: Test Block Reuse Patterns

| Document Information | Details |
|---|---|
| Jira | AI-181 [T7-A3] Developer Guide: Test Block reuse patterns |
| Status | Documents shipped behaviour (Sprint 2 / Week 7) |
| Last Updated | 19 August 2026 |

This guide documents the **actually implemented** Test Block library --
promoting a Test Step into an organisation-wide reusable block, and
inserting a block back into any procedure. It assumes familiarity with
`DEVELOPER_GUIDE_PROCEDURE_AUTHORING.md` (AI-178); this document only
covers what Test Blocks add on top of that workflow.

## 1. What a Test Block is

A `TestBlock` holds the same reusable content fields as a `TestStep`
(Expected Behaviour, Expected Range, Calibration Requirement, Safety
Notes, Acceptance Criteria, Preconditions, Dependencies, Post
Conditions, Pass/Fail Criteria, Expected Observation, Engineer Remarks)
-- but deliberately **no** Interface/Connector/Signal/Protocol/Fault
Category references of its own. Those are LRU-scoped, so a block
cannot carry them across LRUs the way organisation-wide reuse requires.
The target procedure's own bindings are always supplied at insert time
instead (Section 4).

## 2. Promoting a Test Step to a Test Block

Open a procedure already saved at least once (`Save as reusable Test
Block` only appears on an existing step -- i.e. one that already has a
backend id). In that step's `TestStepCard`, click **Save as reusable
Test Block**, fill in a name (required) and description (optional),
then **Save Block**.

This calls:

    POST /api/test-blocks/from-step/{testStepId}
    { "name": "...", "description": "..." }

`TestBlockService.createFromStep` copies the step's eleven reusable
content fields into a brand-new `TestBlock` row and returns it.
**This is a one-time copy, not a link** -- editing the original
`TestStep` afterward does not change the block, and there is currently
no endpoint to edit a `TestBlock` directly once created (see Section 6).
A duplicate block name is rejected with an `IllegalStateException`
(`409 Conflict`, via `GlobalExceptionHandler`).

## 3. Browsing the shared library

`ProcedureBuilderPage` loads every block via `GET /api/test-blocks` and
filters to `b.expectedBehaviour` truthy client-side -- this hides
legacy AI-153-era seeded shell rows (name/description only, no content)
from the **Insert Test Block** panel, since those cannot be inserted
(Section 6). The full unfiltered list, including shell rows, remains
retrievable via the same endpoint for any future block-management page.

## 4. Inserting a Test Block into a procedure

On an already-saved procedure, the **Insert Test Block** panel appears
below the step list (it renders nothing if the filtered block list is
empty). Pick a block, then the same five typed references every
`TestStepCard` requires (Interface, Connector, Signal, Protocol Tag,
Fault Category) -- all required, all scoped to the procedure's LRU the
same way Section 3 of the Procedure Authoring guide describes. Click
**Insert Test Block**:

    POST /api/test-blocks/{blockId}/insert
    {
      "testProcedureId": 42,
      "interfaceId": 7,
      "connectorId": 3,
      "signalLibraryId": 5,
      "protocolTagId": 2,
      "faultCategoryId": 9
    }

**Insert semantics** (`TestBlockService.insertIntoProcedure`): the
block's content is copied into a new `TestStepRequest`, appended to the
target procedure's *current* step list, and the whole procedure is sent
through the existing `TestProcedureService.update` path -- the same
save-diff logic Section 8 of the Procedure Authoring guide describes.
This is deliberate: an insert must go through the normal add/edit/
reorder/remove diffing so it produces a new `procedure_history`
revision exactly like every other content change (Section 5). The
block's `usageCount` is then incremented by one and saved separately.
On success, the panel calls `onInserted` (`loadAll`), so the newly
inserted step and the updated procedure state come straight from the
server rather than being assembled client-side.

## 5. How this interacts with revision history

Because an insert is routed through `TestProcedureService.update`
rather than writing the `TestStep` directly, it participates in the
same snapshot-and-compare revisioning described in
`DEVELOPER_GUIDE_PROCEDURE_AUTHORING.md` Section 9: inserting a block
always changes the procedure's step list, so it always produces a new,
independently retrievable `procedure_history` row. There is no separate
"block insert" event type in the history -- it appears as an ordinary
content-changing revision, indistinguishable in `procedure_history`
from a manually added step with the same content.

### Verifying directly with SQL

    SELECT id, name, usage_count FROM test_blocks WHERE id = :blockId;

    SELECT version_number, created_at
    FROM procedure_history
    WHERE test_procedure_id = :procedureId
    ORDER BY version_number DESC
    LIMIT 1;

`usage_count` should read one higher after each insert, and the latest
`procedure_history` row's `version_number` should be one higher than
before the insert.

## 6. Legacy content-less blocks

`TestBlock` rows seeded before AI-179 (AI-153's original shell) have a
name and description but no reusable content -- `expectedBehaviour` is
`null`. `insertIntoProcedure` explicitly rejects these:

    IllegalStateException: Test Block {id} has no reusable content and
    cannot be inserted (likely a legacy AI-153 seeded shell row)

returned as `409 Conflict`. The frontend never surfaces these as
selectable in the first place (Section 3), so this check exists purely
as backend-authoritative protection against a stale block id being
posted directly (e.g. via Postman or a future admin tool), matching the
"frontend filtering is convenience only" principle from the Procedure
Authoring guide.

## 7. Reuse isolation guarantee (AI-180)

Both directions in this workflow are **snapshots, not live references**:

- A `TestBlock` created via `createFromStep` does not change if the
  source `TestStep` is edited afterward.
- A `TestStep` created via `insertIntoProcedure` does not change if the
  source `TestBlock` is edited afterward.

There is currently no "edit a Test Block" endpoint, so this is verified
in `TestBlockReuseIsolationTest` by mutating the relevant entity
directly via its repository and confirming the other side is
unaffected -- proving the underlying data-model invariant independent
of whatever edit UI/endpoint gets built later.

## 8. Validation rules

**Backend-authoritative, always enforced:**

- `createFromStep`: the source `testStepId` must exist, or the request
  fails with a not-found error; the block `name` must be unique
  (`409 Conflict` otherwise).
- `insertIntoProcedure`: all six ids (`testProcedureId` plus the five
  typed references) are `@NotNull`; the target block must exist and
  must have non-null `expectedBehaviour` (Section 6); the inserted
  step's references are still validated exactly as any other step's
  would be on save (Section 13 of the Procedure Authoring guide).

**Frontend, convenience only:** the block list filtered to
content-bearing rows (Section 3), required-field checks before submit,
and LRU-scoped reference dropdowns. None of this replaces the backend
checks above.

## 9. API endpoint summary

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/test-blocks` | List all Test Blocks (including legacy content-less rows) |
| GET | `/api/test-blocks/{id}` | Get one Test Block |
| POST | `/api/test-blocks/from-step/{testStepId}` | Promote an existing Test Step into a new, reusable Test Block |
| POST | `/api/test-blocks/{id}/insert` | Insert a Test Block's content as a new Test Step in a target procedure -> new revision |

All endpoints require the existing JWT authentication; none of them
introduce new roles or weaken `SecurityConfig`.
