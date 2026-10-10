# Spring-to-FastAPI Advisory Integration

The Spring Boot backend accesses the independent FastAPI advisory service only
through `AiAdvisoryClient`. The backend never receives AI-service database
credentials and the client has no write path into engineering tables.

## Runtime boundary

- Spring endpoint: `POST /api/ai/suggestions`
- FastAPI endpoint: `POST /suggest`
- Configuration: `AI_ADVISORY_BASE_URL` (default:
  `http://127.0.0.1:8000`)
- Docker Compose value: `http://ai-service:8000`

The endpoint returns the FastAPI service's structured `suggestion`,
`confidence`, `source`, and `generated_at` values. Upstream service errors are
exposed as `502 Bad Gateway`; raw downstream exceptions are not returned to the
caller.

## AI-249 verification

`SpringFastApiSuggestionIntegrationTest` starts Uvicorn against the checked-in
FastAPI application, waits for `/health`, calls `/suggest` through the real
Spring HTTP client, and validates the structured response. CI installs the
`ai-service` requirements before running the backend suite.
