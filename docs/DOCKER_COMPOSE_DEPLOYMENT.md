# Five-service Docker Compose deployment

| Document information | Details |
| --- | --- |
| Jira | AI-281 / T18-B1 |
| Compose file | `docker/docker-compose.yml` |
| Public endpoint | `http://localhost` by default |

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

For a non-default local setup, create `docker/.env` (not committed):

```dotenv
APP_PORT=8088
POSTGRES_PORT=5433
POSTGRES_PASSWORD=replace-for-non-demo-use
JWT_SECRET=replace-with-a-long-random-secret
```

## Verification and shutdown

```bash
docker compose config
docker compose exec postgres pg_isready -U iaterap -d iaterap
curl http://localhost/api/health
docker compose down
```

Use `docker compose down -v` only when intentionally deleting the local PostgreSQL volume and all of its data.
