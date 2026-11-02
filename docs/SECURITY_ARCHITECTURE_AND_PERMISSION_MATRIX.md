# Security architecture and permission matrix

| Document information | Details |
| --- | --- |
| Jira | AI-277 / T17-DOC |
| Scope | Section 20 security architecture |
| Enforcement | Spring Security, JWT services, controller method security, Flyway grants |

## Security boundaries

IATERAP is a stateless JWT API. The security filter establishes the authenticated principal from a valid **access** token, then route rules and controller method rules authorise the operation. The FastAPI advisory service is isolated behind the Spring API: it receives an explicit DTO over HTTP and has no IATERAP database credentials or direct write capability.

Unauthenticated access returns `401` with `AUTHENTICATION_REQUIRED` (or `AUTHENTICATION_FAILED` for invalid credentials). Authenticated users without authority receive `403` with `ACCESS_DENIED`. API clients should use the `code`, not a human-readable message, for control flow.

## Token lifecycle

- Login issues an access token and a refresh token.
- Access tokens are the only tokens accepted by `JwtFilter` for protected application requests.
- Refresh tokens carry a distinct token type and a unique `jti`; they are stored server-side by identifier, never as a raw-token lookup value.
- `POST /api/auth/refresh` validates the presented refresh token, revokes its session, and atomically issues a new access/refresh pair linked to its predecessor.
- Expired, revoked, malformed, unknown, or access-type tokens are rejected at refresh.
- `POST /api/auth/logout` revokes the presented refresh-token session.
- Changing a user role revokes that user's active refresh-token sessions so old privilege context cannot be refreshed.

`/api/auth/**`, health, and API-documentation routes are public. All other application routes require authentication. CORS preflight (`OPTIONS`) is permitted; CSRF is disabled because the API is bearer-token based and stateless.

## Permission matrix

| Capability | VIEWER | FIELD_ENGINEER | TEST_ENGINEER | SYSTEM_ENGINEER | RELIABILITY_ENGINEER | ADMIN |
| --- | --- | --- | --- | --- | --- | --- |
| Read authenticated resources | Yes | Yes | Yes | Yes | Yes | Yes |
| Create/update/delete engineering resources | No | No | Yes | Yes | Yes | Yes |
| Submit validation | No | No | Yes | Yes | Yes | Yes |
| Procedure approval/rejection and reviewer comments | No | No | No | Yes | Yes | Yes |
| Rule and scoring configuration | No | No | No | No | No | Yes |
| User role changes | No | No | No | No | No | Yes |
| Synthetic events, reliability analytics, and audit-log reads | No | No | No | Yes | Yes | Yes |

The filter enforces the read-only viewer baseline by requiring an engineering role for mutating `/api/**` requests. Controller-level `@PreAuthorize` checks add narrower workflow rules. This layered design means a route must pass both authentication/HTTP-route checks and any domain-specific method guard.

## Audit and database integrity

Audit logging is append-only for the application runtime role. Flyway migration `V39__audit_log_append_only_application_grants.sql` grants `iaterap_app` only `SELECT` and `INSERT` on `audit_log` (plus required sequence usage) and explicitly revokes `UPDATE`, `DELETE`, and `TRUNCATE` from both `iaterap_app` and `PUBLIC`.

Deployment must use separate identities:

- The Flyway migration owner applies schema changes and grants.
- The runtime datasource uses the restricted `iaterap_app` login through `SPRING_DATASOURCE_USERNAME`.

Using a migration-owner/superuser account as the running application datasource defeats the append-only guarantee and is not supported.

## Operational verification checklist

Before release, verify the following against the deployed environment:

1. An expired access JWT returns `401`; a refresh JWT cannot authenticate a protected resource.
2. A refresh token rotates once and the replaced token cannot be reused.
3. VIEWER cannot send mutating requests; only ADMIN can change configuration or users.
4. Reliability, synthetic-event, and audit-log endpoints reject non-authorised roles.
5. The runtime database role cannot `UPDATE`, `DELETE`, or `TRUNCATE audit_log`.
6. No public dynamic filter/sort parameter is concatenated into SQL. Any future filter/sort feature must whitelist its fields and directions before reaching a query.
7. The AI context endpoint is authenticated and read-only; it accepts no write method.

Automated security checks cover expired-token rejection, access/refresh separation, and the audit-log grant/revoke contract. See [SECURITY_CHECKLIST_AI276.md](SECURITY_CHECKLIST_AI276.md) for the recorded test checklist.
