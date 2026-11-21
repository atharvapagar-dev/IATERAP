# IATERAP Architecture and API Reference

## System Architecture

IATERAP is a container-ready aerospace test-engineering and reliability platform with five runtime services:

```text
Browser -> Nginx / React frontend -> Spring Boot API -> PostgreSQL
                                      |
                                      +-> FastAPI AI advisory service
```

The frontend presents the Engineering Workspace and module pages. Nginx serves the built frontend and proxies `/api` requests to the Spring Boot backend. The backend owns authentication, authorisation, workflow state, audit history, coverage evaluation, validation reporting, and reliability calculations. PostgreSQL is the system of record. The FastAPI service is an advisory boundary: it supplies AI context and suggestions but does not directly mutate engineering workflow data.

For local and container setup, see [Installation Guide](INSTALLATION_GUIDE.md), [Docker Compose Deployment](DOCKER_COMPOSE_DEPLOYMENT.md), and [Environment Configuration](ENVIRONMENT_CONFIGURATION.md).

## Core Modules

| Module | Responsibility | Main boundary |
| --- | --- | --- |
| Identity and security | Login, JWT validation, RBAC, access-denied responses, audit permissions | Spring Security and API error envelope |
| Engineering hierarchy | Projects, aircraft, subsystems, LRUs, interfaces/connectors | REST API and PostgreSQL |
| Procedure lifecycle | Authoring, typed steps, reusable blocks, revisions, submission, review, approval, rollback | Procedure and review endpoints |
| Coverage and rules | Coverage aggregation, configurable rules, threshold checks | Coverage engine and rule configuration endpoints |
| Validation and reliability | Validation reports, synthetic events, MTBF/MTTR/failure-rate, Pareto/HFRA | Reporting and reliability endpoints |
| AI advisory | Read-only context, suggestion creation/acceptance, isolation from deterministic workflow writes | Backend AI boundary and FastAPI service |

## API Conventions

- Base path: `/api`.
- Authentication: send `Authorization: Bearer <JWT>` for protected endpoints. Authentication, health, and documentation routes are public as configured by the backend.
- Successful responses use the module DTO documented by the endpoint; mutation responses carry the created or updated resource where appropriate.
- Error responses use the common API error shape and stable machine-readable `code` values, for example `ACCESS_DENIED` and coverage-threshold errors.
- Collection endpoints may expose pagination and filtering parameters; callers must treat response fields as the contract rather than relying on a database schema.
- The interactive OpenAPI UI is exposed by the backend when enabled at `/swagger-ui/index.html`.

## Endpoint Groups

The OpenAPI UI is the authoritative field-level contract. This table is the consolidated navigation reference for client developers.

| Group | Representative paths | Purpose |
| --- | --- | --- |
| Public and identity | `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/health` | Session establishment and health checks |
| Engineering hierarchy | `/api/projects`, `/api/aircraft`, `/api/subsystems`, `/api/lrus` | Create and manage the project hierarchy |
| Design catalogues | `/api/interfaces`, `/api/connectors`, `/api/signal-libraries`, `/api/protocol-tags`, `/api/fault-templates`, `/api/fmea-branch-mappings` | Model interfaces, signals, faults, and FMEA mapping |
| Procedures and review | `/api/procedures`, `/api/procedures/{id}/builder`, `/api/procedures/review-queue` | Author, version, submit, review, approve, reject, compare, and roll back procedures |
| Coverage and rules | `/api/coverage/heatmap`, `/api/procedures/{id}/coverage`, `/api/admin/rules` | Retrieve coverage results and administer authorised rule configuration |
| Validation and reporting | `/api/validation/runs`, `/api/validation/reports`, `/api/reliability/*` | Validation output, reliability metrics, Pareto/HFRA, and trend analysis |
| AI advisory | `/api/ai/context/test-procedure/{id}`, `/api/ai/suggestions` | Retrieve scoped context and manage accepted suggestions without bypassing workflow controls |
| Audit | `/api/audit-logs` | Search audit history subject to RBAC |

## Security and Data Boundaries

- Keep JWT signing keys, database passwords, and service URLs in environment files excluded from source control. Start with the committed `*.example` templates.
- The backend is the only application writer to workflow data. The AI service must remain advisory and use the backend boundary for accepted changes.
- Audit history is append-only for application roles. See [Security Architecture and Permission Matrix](SECURITY_ARCHITECTURE_AND_PERMISSION_MATRIX.md).
- Validate all user-supplied filter, sort, and paging values at the API boundary. Never pass them directly to persistence queries.

## Operational References

- [API standardisation notes](API_STANDARDIZATION.md)
- [Procedure and coverage API guide](API_PROCEDURES_TEST_STEPS.md)
- [Reliability metrics API guide](API_RELIABILITY_METRICS.md)
- [AI context API guide](API_AI_CONTEXT.md)
- [Full lifecycle test report](FULL_LIFECYCLE_TEST_REPORT.md)
- [Backup and restore verification](BACKUP_RESTORE_VERIFICATION.md)

