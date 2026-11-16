# Clean-checkout Docker Compose verification

| Document information | Details |
| --- | --- |
| Jira | AI-285 / T18-QA |
| Script | `scripts/verify-clean-compose-checkout.ps1` |
| Scope | A fresh clone of the selected branch using generated local-only credentials |

This run verifies that a contributor who did not create the Compose file can clone the repository and start the complete five-service stack without relying on untracked local files or existing Docker volumes.

## Preconditions

1. Docker Desktop is running and has access to image registries.
2. Git is available in PowerShell.
3. Ports `18080-18179` and `15432-15531` are available on the local machine.

## Run

From the repository root:

```powershell
.\scripts\verify-clean-compose-checkout.ps1
```

The script performs the following checks:

1. Clones `develop` into a timestamped temporary directory.
2. Creates a generated, ignored `docker/.env` in that clone with disposable credentials and available local ports.
3. Validates the Compose configuration before starting services.
4. Builds and starts PostgreSQL, FastAPI, Spring Boot, React, and Nginx, waiting for their Compose health checks.
5. Verifies the public frontend endpoint and proxied `/api/health` endpoint return HTTP 200.
6. Writes a JSON evidence report under ignored `artifacts/clean-compose-verification/`.
7. Removes the temporary Compose project, its volume, and the cloned checkout unless `-KeepCheckout` is supplied.

To verify another branch or retain the clone for diagnosis:

```powershell
.\scripts\verify-clean-compose-checkout.ps1 -Branch feature/example -KeepCheckout
```

## Safety

- The verification never uses the caller's `docker/.env` or existing database volume.
- It uses a unique Compose project name and disposable database credentials.
- Cleanup uses `down --volumes` only for the generated verification project.
- Reports are ignored by Git and contain no passwords or JWT secret values.

## Evidence

A successful run produces a `PASS` JSON file under `artifacts/clean-compose-verification/`. Retain or attach that report to AI-285.
