# Security checklist run (AI-276)

Run date: 30 October 2026. Scope: JWT expiry and purpose handling, RBAC bypass attempts, audit-log append-only grants, and query-parameter injection exposure.

| Check | Evidence | Result |
| --- | --- | --- |
| Expired JWT handling | `SecurityChecklistTest.expiredJwtIsRejected` creates an already-expired access JWT and verifies validation fails. | Pass |
| Access/refresh JWT separation | `SecurityChecklistTest.accessAndRefreshTokensHaveDistinctPurposes` and `JwtFilterTest.refreshTokenCannotAuthenticateAnApiRequest`. | Pass |
| Refresh rotation/reuse | `RefreshTokenServiceTest` covers valid rotation, revoked-token rejection, invalid-token rejection, and access-token rejection at the refresh boundary. | Pass |
| RBAC bypass | `ReviewAuthorizationControllerTest` denies unauthenticated and non-reviewer approval actions; `SecurityPermissionMatrixTest` verifies admin-only role mutation and restricted audit-log reads. | Pass |
| Audit append-only database privileges | `SecurityChecklistTest.auditLogMigrationRevokesEveryNonInsertRuntimeOperation` verifies V39 grants only `SELECT`/`INSERT` and revokes `UPDATE`/`DELETE`/`TRUNCATE` for runtime and public roles. | Pass |
| SQL injection through filter/sort | The covered Procedure/Rule endpoints use typed cursor and bounded `limit` values only; no user-supplied SQL fragments or dynamic sort fields are accepted. Repository access remains Spring Data parameter binding. | Pass |

The suite is deliberately automated where possible; production deployment must still run Flyway with a separate migration owner and configure the application datasource with the least-privileged `iaterap_app` role.
