# Deployment regression suite

| Document information | Details |
| --- | --- |
| Jira | AI-290 / T19-B1 |
| Local runner | `scripts/test-deployment-routes.ps1` |
| CI workflow | `.github/workflows/deployment-regression.yml` |

The suite validates the containerised platform through its public Nginx endpoint. It detects broken Compose health checks, frontend routing, reverse-proxy configuration, backend authentication, and module read-route availability.

The frontend container exposes a dedicated internal `GET /healthz` endpoint. Compose probes that endpoint with `curl`, so frontend health is independent of the single-page-app fallback route and the base image's optional utilities.

## Coverage

- Public backend health endpoint.
- Seeded admin login through `/api/auth/login`.
- All 29 current top-level frontend screens through Nginx, including Workspace, Procedure, Coverage, Review, Reliability and AI screens.
- 15 protected API read endpoints spanning Projects, aircraft hierarchy, LRUs, procedure authoring, audit, rules, coverage, reliability and AI queue modules.

The screen list intentionally covers every top-level route. Dynamic entity-detail routes require domain-specific fixture assertions and remain covered by their module tests.

## Local run

Start the Compose stack with a populated `docker/.env`, then run:

```powershell
$env:DEPLOYMENT_REGRESSION_USERNAME = "admin"
$env:DEPLOYMENT_REGRESSION_PASSWORD = "admin123"
.\scripts\test-deployment-routes.ps1 -BaseUrl http://localhost:<APP_PORT>
```

The script writes an ignored JSON report under `artifacts/deployment-regression/` and does not log the supplied password or access token.

## CI behaviour

The Deployment Regression workflow creates a disposable Compose environment, builds all five services, waits for health checks, runs the deployed route/API suite, uploads logs and the report, then removes the generated Compose volume.
