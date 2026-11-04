# External environment configuration

| Document information | Details |
| --- | --- |
| Jira | AI-282 / T18-B2 |
| Scope | Spring backend, Docker Compose, local developer configuration |

IATERAP does not store runtime database credentials or JWT signing keys in version-controlled application configuration. Supply them through environment variables or an ignored local environment file instead.

## Required backend variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Yes | Restricted application database role |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database role password |
| `JWT_SECRET` | Yes | JWT HMAC signing secret; use at least 32 random bytes |
| `AI_ADVISORY_BASE_URL` | No | AI service URL; defaults to local loopback only |
| `JWT_ACCESS_TOKEN_TTL_MINUTES` | No | Access-token lifetime; default `60` |
| `JWT_REFRESH_TOKEN_TTL_DAYS` | No | Refresh-token lifetime; default `7` |

The backend fails at startup if any required variable is missing. This is intentional: a missing secret is safer than silently starting with a known default.

## Local development

Copy `backend/.env.example` to an ignored local file, replace placeholders, then load its values into the shell or your IDE run configuration. The JVM does not automatically read `.env` files; use your IDE environment-file integration or explicitly export variables before starting Spring Boot.

PowerShell example:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:5432/iaterap"
$env:SPRING_DATASOURCE_USERNAME = "iaterap_app"
$env:SPRING_DATASOURCE_PASSWORD = "use-a-local-secret"
$env:JWT_SECRET = "use-a-long-random-secret-at-least-32-bytes"
cd backend
.\mvnw.cmd spring-boot:run
```

## Docker Compose

Copy `docker/.env.example` to `docker/.env`, replace all placeholders, then run Compose from the `docker` directory:

```bash
cd docker
docker compose --env-file .env config
docker compose --env-file .env up --build -d
```

Compose requires `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `APP_PORT`, and `JWT_SECRET`. It refuses to render a configuration without them. The backend receives the same database/JWT variables through its container environment.

## Secret handling rules

- Never commit `.env`, private keys, passwords, JWTs, or production connection strings.
- Commit only `*.env.example` templates containing placeholders.
- Use a unique `JWT_SECRET` per environment; rotating it invalidates issued tokens.
- Keep the Flyway migration owner separate from the restricted runtime database role.
- Use a managed secret store in production and inject values at deployment time.
