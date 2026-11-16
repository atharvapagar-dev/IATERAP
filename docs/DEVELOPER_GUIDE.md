# IATERAP Developer Guide

| Document information | Details |
| --- | --- |
| Jira | AI-291 / T19-B2 |
| Audience | Contributors extending the platform |
| Related setup guide | [Installation Guide](INSTALLATION_GUIDE.md) |
| Related deployment guide | [Docker Compose Deployment](DOCKER_COMPOSE_DEPLOYMENT.md) |

## Development workflow

1. Branch from the current `develop` branch using the Jira key in the branch name.
2. Keep a change inside its module boundary: frontend UI, Spring backend, FastAPI advisory service, schema migration, or deployment automation.
3. Add focused tests at the same time as the implementation.
4. Run the checks in [Verification](#verification), update the relevant documentation, and open a PR against `develop`.
5. Use Conventional Commit messages and include the Jira key in the PR title and description.

Never commit `.env` files, tokens, database dumps, generated artifacts, or production credentials.

## Repository map

| Path | Contents | Normal change location |
| --- | --- | --- |
| `frontend/src` | React pages, components, routes, API clients, styles | UI changes and client request handling |
| `backend/src/main/java` | Spring controllers, services, entities, repositories, rules, security | REST APIs and deterministic domain logic |
| `backend/src/main/resources/db/migration` | Versioned Flyway migrations | Persistent-schema changes only |
| `backend/src/test` | JUnit unit, controller, and integration tests | Backend behaviour coverage |
| `ai-service/app` | FastAPI routes and advisory logic | AI-context and suggestion behaviour |
| `ai-service/tests` | Pytest coverage for the advisory service | AI service validation |
| `docker` | Compose stack, Nginx routing, deployment runbook | Container/runtime configuration |
| `scripts` | Repeatable verification and operational commands | Local/CI automation |
| `docs` | Implementation, API, deployment, and user-facing guides | Documentation updates |

## Running locally

Follow the [Installation Guide](INSTALLATION_GUIDE.md) for the direct-development setup. The normal order is:

1. Start PostgreSQL.
2. Start the AI service on port `8000`.
3. Start Spring Boot on port `8080`; Flyway applies migrations automatically.
4. Start the React development server, normally on port `5173`.

Use the seeded `admin` / `admin123` account only for local development. Use Docker Compose when validating the five-service deployment behind Nginx; do not mix local service URLs with a Compose frontend.

## Backend implementation rules

### Add or change an API

1. Define request/response DTOs and validation annotations.
2. Keep controller methods small: HTTP translation and authorisation only.
3. Put transactional domain behaviour in a service and persistence queries in a repository.
4. Reuse the global API error conventions; do not expose exception details or database entities directly.
5. Add controller/service tests for authorised, unauthorised, invalid, and successful cases.
6. Update the relevant API guide and, when the endpoint is public-facing, OpenAPI annotations.

Backend routes are currently rooted at `/api`. Treat the actual controller and OpenAPI contract as authoritative over older planning documents.

### Add a database migration

1. Create the next sequential `V<version>__descriptive_name.sql` file under `backend/src/main/resources/db/migration`.
2. Make the migration forward-only and compatible with a populated database.
3. Add indexes and foreign keys where the query and ownership model require them.
4. Never edit a migration that may already have run in another environment.
5. Prove the change with a test and a clean database startup.

## Add a Rule Engine rule

Rules are deterministic, side-effect-free strategies in `backend/.../coverage/rule`.

1. Implement `EngineeringRule`; return a stable `getRuleId()`, category, priority, description, and a `RuleResult` from `evaluate`.
2. Keep rule evaluation limited to the provided procedure, LRU, and ordered steps. A rule must not write data, call other rules, or make network requests.
3. Add a matching `rules_configuration` seed/migration entry. The ID must exactly match `getRuleId()`.
4. Use `STRUCTURAL` only for rules that must block submission. Structural failures short-circuit later categories.
5. Add unit tests for passing, failing, disabled, ordering, and configuration-mismatch behaviour.
6. Update [Rule Engine Architecture](RULE_ENGINE.md) and any affected approval/coverage documentation.

The engine discovers Spring rule beans and executes enabled persisted configuration in deterministic category, priority, and rule-ID order. A configured ID without an implementation is intentionally treated as a startup/validation defect rather than silently skipped.

## Frontend implementation rules

- Add protected pages in `frontend/src/routes/AppRoutes.jsx` and use the existing API service layer rather than issuing ad-hoc requests from components.
- Preserve the authenticated landing route (`/workspace`) and pass a requested protected route through `location.state` when redirecting to login.
- Handle loading, empty, success, and API-error states explicitly.
- Do not treat client-side visibility as authorisation; backend RBAC is the enforcement point.
- Run lint and a production build after every UI change.

## AI advisory boundary

The FastAPI service is advisory. It may generate context and recommendations but must not acquire direct database write access or bypass the Spring workflow. Accepted suggestions are applied only through backend-authorised operations. Add FastAPI tests under `ai-service/tests`; `ai-service/pytest.ini` keeps test discovery deterministic.

## Verification

Run the smallest relevant check while developing, then run the complete change gate before opening a PR:

```powershell
# Backend
cd backend
.\mvnw.cmd clean test

# AI advisory service
cd ..\ai-service
py -3 -m pytest -q

# Frontend
cd ..\frontend
npm run lint
npm run build
```

For an end-to-end deployment check, start the Compose stack and run:

```powershell
$env:DEPLOYMENT_REGRESSION_USERNAME = "admin"
$env:DEPLOYMENT_REGRESSION_PASSWORD = "admin123"
.\scripts\test-deployment-routes.ps1 -BaseUrl http://localhost:<APP_PORT>
```

The GitHub deployment-regression workflow performs the same five-service check in a disposable environment for pull requests. See [Deployment Regression Suite](DEPLOYMENT_REGRESSION.md) for its scope and generated evidence.

## PR checklist

- [ ] Branch is based on current `develop`.
- [ ] Change has focused automated coverage.
- [ ] Migrations are forward-only and tested where applicable.
- [ ] Secrets, dumps, generated artifacts, and local `.env` files are excluded.
- [ ] Backend, AI, frontend, or deployment checks relevant to the change pass.
- [ ] API, architecture, installation, or deployment docs were updated if the contract changed.
- [ ] PR names the Jira ticket, summary, verification evidence, and any deployment risk.

