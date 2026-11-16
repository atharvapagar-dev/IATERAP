# Full Lifecycle Test Report

## Purpose

This report defines the end-to-end release gate for the IATERAP workflow, from authenticated access through procedure authoring, review, validation, reliability analytics, AI advisory, and deployment readiness. It is deliberately split into deterministic automated suites and the Docker Compose verification so failures identify the affected boundary.

Run the local automated gates from the repository root:

```powershell
.\scripts\run-full-lifecycle-tests.ps1
```

The script writes a timestamped JSON result to `artifacts/full-lifecycle/` (ignored by Git). It uses the backend `test` profile and reads database connection settings from the environment or the test defaults. Run the clean-checkout Compose gate separately when Docker Desktop is available:

```powershell
.\scripts\verify-clean-compose-checkout.ps1 -Branch develop
```

## Automated Run Record

Recorded on 16 November 2026 in the development environment:

| Gate | Result | Evidence |
| --- | --- | --- |
| Backend regression suite | PASS | Maven Surefire: 322 tests, 0 failures, 0 errors, 0 skipped |
| AI advisory service suite | PASS | `py -3 -m pytest -q`: 19 passed |
| Frontend static checks | PASS | `npm run lint` and `npm run build` completed successfully |
| Clean-checkout Compose stack | Requires Docker Desktop | [`verify-clean-compose-checkout.ps1`](../scripts/verify-clean-compose-checkout.ps1) builds a fresh clone and verifies service health plus public routes |

`pytest.ini` constrains pytest to the maintained `tests/` directory. This prevents transient local pytest cache directories from being discovered as test sources and makes the AI-service gate reproducible.

## Workflow Stage Coverage

| # | Workflow stage | Primary automated evidence |
| --- | --- | --- |
| 1 | Health and configuration boundary | health/controller and deployment verification tests |
| 2 | Authentication and token handling | security, JWT, and authentication service tests |
| 3 | Project creation and administration | project controller and service tests |
| 4 | Aircraft setup | aircraft controller and service tests |
| 5 | Subsystem setup | subsystem controller and service tests |
| 6 | LRU setup | LRU controller and service tests |
| 7 | Interface and connector definition | interface/connector controller and validation tests |
| 8 | Signal, protocol, and fault cataloguing | signal, protocol-tag, and fault-template tests |
| 9 | FMEA branch mapping | FMEA mapping controller and validation tests |
| 10 | Procedure authoring | procedure creation and builder tests |
| 11 | Test-step typing and validation | procedure-step and input-validation tests |
| 12 | Reusable procedure blocks | procedure builder regression tests |
| 13 | Version-history creation | author/version/submit regression tests |
| 14 | Version comparison | procedure version comparison tests |
| 15 | Rollback | procedure rollback integration tests |
| 16 | Coverage evaluation | coverage engine and rule tests |
| 17 | Submission for review | author/version/submit regression tests |
| 18 | Approval or rejection | procedure approval coverage-gate tests |
| 19 | Audit history | audit-log and permission-matrix tests |
| 20 | Validation runs and seeded events | synthetic-event generation integration tests |
| 21 | Validation report retrieval | validation-report controller and service tests |
| 22 | Reliability metrics and reporting tables | reliability analytics and metrics tests |
| 23 | Pareto, HFRA, and trend analysis | Pareto/HFRA service tests |
| 24 | AI advisory suggestions and isolation | AI context/controller tests and `ai-service/tests` |
| 25 | Containerised deployment readiness | clean-checkout Compose verification script and health checks |

## Full-System Acceptance Checklist

- Run `run-full-lifecycle-tests.ps1` with a reachable test PostgreSQL database.
- Run the clean-checkout Compose verification on a machine with Docker Desktop running.
- Use a non-production JWT secret and database during all verification runs.
- Attach the generated JSON reports to the release evidence; do not commit them because they may include machine-specific paths and timestamps.
- Confirm the UI login lands at `/workspace` unless a protected route explicitly requested a different destination.

