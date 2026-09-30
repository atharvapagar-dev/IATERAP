# Week 12: Manual Review Approval Workflow

## Acceptance criteria discovered locally

The prior review-queue implementation explicitly deferred the following
workflow items: approve/reject actions, persisted reviewer comments, approval
decision audit history, and reviewer-role access control. This release
completes those manual-review items; it does not implement AI approval.

## Workflow

1. A procedure is submitted from `DRAFT` to `IN_REVIEW` using the existing
   structural-validation and frozen-coverage-report flow.
2. An `ADMIN`, `SYSTEM_ENGINEER`, or `RELIABILITY_ENGINEER` can open the
   review queue, load the immutable submitted revision's comments, add a
   comment, then approve or reject the procedure.
3. Approval retains the existing frozen coverage threshold gate. Rejection
   transitions directly from `IN_REVIEW` to `REJECTED` and is not coverage
   gated.
4. Each successful decision stores an audit record with the authenticated
   reviewer, decision action, procedure id, prior state, serialized result,
   and reviewed version metadata.

`TEST_ENGINEER`, `FIELD_ENGINEER`, and `VIEWER` cannot invoke reviewer routes.
The authorization boundary is enforced by Spring method security; the React
page reports a server-side forbidden or validation response rather than
assuming a decision succeeded.

## API

- `GET /api/procedures/review-queue`
- `POST /api/procedures/{id}/approve`
- `POST /api/procedures/{id}/reject`
- `GET /api/reviewer-comments/by-revision/{procedureHistoryId}`
- `POST /api/reviewer-comments`

The first three decision/review routes and reviewer-comment routes require one
of the reviewer roles above. State violations continue to return the existing
`409 Conflict` response. No schema migration is needed: `approval_status`,
the immutable `procedure_history` revision id, and `audit_log` already provide
the required persistence model.

## Out of scope

Approval remains a human decision. No trained AI model, prediction endpoint,
or automated approval decision is included in Week 12.
