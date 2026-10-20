# AI Service Isolation Verification

The FastAPI advisory service is optional decision-support infrastructure. Procedure authoring, coverage calculation, snapshotting, review, and approval execute entirely in the Spring Boot core.

`AiServiceIsolationVerificationTest` verifies this boundary by deliberately targeting an unreachable AI endpoint, then exercising deterministic coverage logic and checking that the authoring, coverage, and approval-core classes have no dependency on the AI package.

For a deployment check, stop only the advisory container:

```bash
docker compose -f docker/docker-compose.yml stop ai-service
```

The backend remains available because it depends only on PostgreSQL. Advisory requests return the existing controlled `502` response while core engineering operations remain available.
