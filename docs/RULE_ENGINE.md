# Rule Engine Architecture

## Purpose

The Rule Engine is the validation boundary between a structured `TestProcedure` and later engineering analysis. It evaluates a procedure, its `Lru` context, and its ordered `TestStep` list through independent engineering rules. Its first production responsibility is to reject malformed procedure structure before a procedure enters the approval workflow; it is also the extension point for future Coverage, Safety, and Advisory rules.

The Coverage Engine is not yet a separate endpoint or score calculator. This component provides the ordered, fail-fast validation foundation that coverage computation can call without duplicating structural checks.

## Strategy contract and result

`EngineeringRule` is the Strategy interface. A rule receives only the supplied procedure/LRU context (and, where needed, ordered steps) and returns a side-effect-free `RuleResult`. Rules do not invoke or coordinate other rules.

`RuleResult` is immutable and contains:

- pass/fail state;
- a human-readable explanation;
- optional structured details, such as the invalid step index and reason;
- the stable rule identifier and category attached by `RuleEngine` when it evaluates a configured rule.

The current `TestStepSignalRule` (`STRUCT-TEST-STEP-SIGNAL`) verifies that every test step has a Signal Library reference belonging to the same LRU as the procedure. A missing or cross-LRU signal produces a failed result with structured details.

## Ordering and fail-fast behavior

Spring discovers registered `EngineeringRule` implementations. `RuleEngine` loads only enabled entries from `rules_configuration`, resolves them by stable rule identifier, and orders them deterministically:

1. `STRUCTURAL` rules execute before all other categories.
2. Within that grouping, lower persisted priority executes first.
3. Rule identifier is the deterministic tie-breaker.

If a structural rule fails, the engine stops immediately and does not evaluate later rules. Configuration controls whether an implementation participates and its execution order; it does not allow arbitrary rule logic to be stored in the database.

## Submission and approval integration

`TestProcedureService.submitForReview` loads the procedure and steps, invokes `RuleEngine`, and rejects a failed structural result before the approval state machine transition or persistence write. The existing submit-time structural checks remain in place. Consequently, an enabled malformed Signal reference is rejected clearly before later rule categories or future coverage calculations can proceed.

High-level flow:

```text
Admin Console -> /api/admin/rules -> RuleConfigurationService -> rules_configuration

Submit for review -> TestProcedureService -> RuleEngine -> enabled configuration + rule beans
                                                    -> RuleResult(s)
                                                    -> structural failure: reject submission
                                                    -> structural pass: continue approval workflow
```

## Persisted rule metadata

`rules_configuration` stores one row per stable rule identifier with category, non-negative priority, description, enabled state, and timestamps. `V21__rules_configuration.sql` creates the table, indexes it for priority/execution lookup, and seeds the initial structural Signal rule. `V22__seed_default_rules_configuration.sql` uses an idempotent insert for that default seed, preserving the expected configuration on migrated installations.

The JPA `RuleConfiguration` entity, repository, and service are the single persistence path used by both the engine and admin API.

## Admin configuration

The backend exposes ADMIN-protected endpoints using stable identifiers rather than database row IDs:

- `GET /api/admin/rules`
- `PATCH /api/admin/rules/{ruleId}/enabled`
- `PATCH /api/admin/rules/{ruleId}/priority`

Requests validate null and negative values through the existing validation and `ApiError` conventions. The Rule Management frontend at `/admin/rules` uses these endpoints to list rules, toggle enabled state, and update priority. The browser route follows the existing authenticated-route pattern; the backend remains the authoritative ADMIN authorization boundary because the current frontend session stores a token but no role claim.

## Extensibility

Future rule beans can declare stable IDs and categories for Coverage, Safety, or Advisory evaluation. Adding a rule requires its implementation and a matching persisted configuration row; it does not require editing a hard-coded rule chain. Structural rules retain precedence and can continue to short-circuit later categories.
