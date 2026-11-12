# Five-service Docker Compose deployment

| Document information | Details |
| --- | --- |
| Jira | AI-281 / T18-B1 |
| Compose file | `docker/docker-compose.yml` |
| Public endpoint | `http://localhost` by default |

For operations, backup/recovery, upgrade, rollback and security guidance, see the [Compose deployment runbook](../docker/DEPLOYMENT.md) stored beside the Compose file.

## Services

| Service | Responsibility | Health probe |
| --- | --- | --- |
| `postgres` | Persistent PostgreSQL data and Flyway schema target | `pg_isready` |
| `ai-service` | Independent FastAPI advisory service | `GET /health` |
| `backend` | Spring Boot API and Flyway migration runner | `GET /api/health` |
| `frontend` | Built React single-page application | `GET /` |
| `nginx` | Public reverse proxy and same-origin API routing | `GET /` |

`backend` starts only after PostgreSQL and FastAPI are healthy. `nginx` starts only after the frontend and backend are healthy. Nginx routes `/api/**` to Spring Boot and every other request to the React application. FastAPI stays on the private Compose network and is called by Spring Boot at `http://ai-service:8000`.

## Run

```bash
cd docker
docker compose up --build -d
docker compose ps
```

Open `http://localhost` (or `http://localhost:${APP_PORT}` when `APP_PORT` is set). Follow startup logs with `docker compose logs -f backend ai-service nginx`.

Before starting Compose, copy `docker/.env.example` to `docker/.env` (not committed) and replace all placeholders. Runtime database credentials and JWT signing keys are required; Compose intentionally fails fast if they are absent. See [Environment Configuration](ENVIRONMENT_CONFIGURATION.md).

```dotenv
POSTGRES_DB=iaterap
POSTGRES_USER=iaterap_app
POSTGRES_PASSWORD=replace-with-a-unique-local-password
POSTGRES_PORT=5433
APP_PORT=8088
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-bytes
```

## Verification and shutdown

```bash
docker compose --env-file .env config
docker compose exec postgres pg_isready -U iaterap -d iaterap
curl http://localhost/api/health
docker compose down
```

Use `docker compose down -v` only when intentionally deleting the local PostgreSQL volume and all of its data.
