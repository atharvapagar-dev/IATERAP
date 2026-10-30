# Security architecture and permission matrix

AI-272 completes the Week 17 security hardening baseline.

## Token controls

- Access JWTs authenticate normal API calls only.
- Refresh JWTs are purpose-bound: they can rotate a session through `/api/auth/refresh`, but cannot authenticate an application request.
- Refresh rotation is transactional: the used session is revoked and linked to its successor in one database transaction.
- A revoked, expired, unknown, malformed, or access-type token is rejected during refresh.
- Changing a user's role revokes all of that user's active refresh sessions.

## Permission matrix

| Capability | VIEWER | TEST_ENGINEER | SYSTEM_ENGINEER | RELIABILITY_ENGINEER | ADMIN |
| --- | --- | --- | --- | --- | --- |
| Read authenticated resources | Yes | Yes | Yes | Yes | Yes |
| Create/update/delete normal engineering resources | No | Yes | Yes | Yes | Yes |
| Submit validation | No | Yes | Yes | Yes | Yes |
| Procedure approval/rejection and reviewer comments | No | No | Yes | Yes | Yes |
| Rule/scoring configuration | No | No | No | No | Yes |
| User role changes | No | No | No | No | Yes |
| Synthetic events / reliability analytics / audit-log reads | No | No | Yes | Yes | Yes |

The security filter enforces the read-only VIEWER baseline for every mutating `/api/**` request. Controller-level `@PreAuthorize` checks enforce the narrower workflow and administrator actions above. Authentication and authorization errors expose `AUTHENTICATION_REQUIRED`, `AUTHENTICATION_FAILED`, and `ACCESS_DENIED` codes.

## Append-only audit storage

`V39__audit_log_append_only_application_grants.sql` grants the dedicated `iaterap_app` runtime database role only `SELECT` and `INSERT` on `audit_log`, explicitly revoking `UPDATE`, `DELETE`, and `TRUNCATE`. The Flyway migration role must remain separate from the runtime role. Production deployment must provision the `iaterap_app` login and use it for `SPRING_DATASOURCE_USERNAME`.
