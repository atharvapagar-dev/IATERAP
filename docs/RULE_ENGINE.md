# Rule Engine

The coverage rule engine evaluates a `TestProcedure` with its `Lru` context and ordered `TestStep` list. Each `EngineeringRule` is an independent Strategy and returns an immutable `RuleResult` containing pass/fail status, explanation, rule metadata, and optional details.

Enabled rule implementations are registered by Spring using their stable rule identifier. `rules_configuration` supplies the enabled state, category, priority, and description. The engine sorts structural rules before later categories, then by priority and rule identifier for deterministic execution. A failed structural result stops evaluation. The submit-for-review gate invokes the engine before its state transition and persistence work, so an enabled structural rule rejects a malformed procedure before later rule categories can run.

Administrators can inspect and update configuration through authenticated admin endpoints:

- `GET /api/admin/rules`
- `PATCH /api/admin/rules/{ruleId}/enabled` with `{ "enabled": true }`
- `PATCH /api/admin/rules/{ruleId}/priority` with `{ "priority": 100 }`

These endpoints require the `ADMIN` role. The initial migration seeds `STRUCT-TEST-STEP-SIGNAL`, which rejects missing or cross-LRU Signal references.
