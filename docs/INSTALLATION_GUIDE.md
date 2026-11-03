# IATERAP installation guide

| Document information | Details |
| --- | --- |
| Jira | AI-280 / T18-A3 |
| Audience | Developers setting up a local, non-container development environment |
| Deployment alternative | [Docker Compose deployment](DOCKER_COMPOSE_DEPLOYMENT.md) |

This guide installs IATERAP directly on a developer machine. It is intentionally separate from the Docker Compose guide: use this guide when running services individually for development and debugging; use Compose when you need the five-service stack behind Nginx.

## 1. Prerequisites

Install the following and confirm they are available on your `PATH`:

| Tool | Required version | Check |
| --- | --- | --- |
| Git | Current stable | `git --version` |
| Java | 21 | `java -version` |
| PostgreSQL | 16 or compatible | `psql --version` |
| Python | 3.12 | `python --version` |
| Node.js | 22 LTS | `node --version` |
| npm | Bundled with Node 22 | `npm --version` |

Maven is provided through `backend/mvnw` (`mvnw.cmd` on Windows), so a global Maven installation is not required.

## 2. Get the source

```bash
git clone https://github.com/atharvapagar-dev/IATERAP.git
cd IATERAP
```

If you are working from an existing clone, start from the latest `develop` branch.

```bash
git switch develop
git pull --ff-only origin develop
```

## 3. Create the local database

Start PostgreSQL, then create the development role and database. The commands below use the repository's local defaults. They are for local development only; use unique secrets and a restricted runtime role outside a local machine.

```sql
CREATE ROLE iaterap WITH LOGIN PASSWORD 'iaterap123';
CREATE DATABASE iaterap OWNER iaterap;
```

Run them with a PostgreSQL administrator account, for example:

```bash
psql -U postgres -d postgres
```

The backend runs Flyway migrations automatically on first startup. Do not manually apply files from `backend/src/main/resources/db/migration`.

## 4. Install the AI service

Use an isolated Python environment:

```bash
cd ai-service
python -m venv .venv
```

Activate it:

```powershell
# Windows PowerShell
.\.venv\Scripts\Activate.ps1
```

```bash
# macOS/Linux
source .venv/bin/activate
```

Install and start the service:

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verify `http://127.0.0.1:8000/health` returns an `UP` response. Keep this terminal running.

## 5. Start the backend

Open a second terminal at the repository root. The backend defaults already point to local PostgreSQL at `localhost:5432` and the AI service at `127.0.0.1:8000`.

```powershell
# Windows PowerShell
cd backend
.\mvnw.cmd spring-boot:run
```

```bash
# macOS/Linux
cd backend
./mvnw spring-boot:run
```

For a repeatable demo dataset, add the synthetic-data profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=synthetic-data"
```

The profile clears and reseeds the explicitly synthetic engineering reference data at startup. Do not use it with data you need to retain.

Verify the backend:

```bash
curl http://127.0.0.1:8080/api/health
```

Expected response: `Backend is Healthy!`

## 6. Start the frontend

Open a third terminal:

```bash
cd frontend
npm ci
npm run dev
```

Vite prints the local URL, normally `http://localhost:5173`. The development frontend calls the backend at `http://127.0.0.1:8080/api` by default.

## 7. Local sign-in and API checks

The baseline migration includes local seed accounts. The administrator development account is:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | `ADMIN` |

These credentials are public repository fixtures, not production credentials. Change/remove them for any environment outside local development.

You can verify authentication directly:

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Useful local endpoints:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Backend health | `http://127.0.0.1:8080/api/health` |
| Spring OpenAPI UI | `http://127.0.0.1:8080/swagger-ui/index.html` |
| FastAPI health | `http://127.0.0.1:8000/health` |
| FastAPI docs | `http://127.0.0.1:8000/docs` |

## 8. Run checks before contributing

```bash
# Backend (from backend/)
./mvnw test

# AI service (from ai-service/, with the virtual environment active)
pip install pytest
pytest -q

# Frontend (from frontend/)
npm run lint
npm run build
```

On Windows, use `mvnw.cmd` rather than `./mvnw`.

## 9. Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Backend cannot connect to PostgreSQL | Confirm PostgreSQL is running, the `iaterap` database exists, and port `5432` is free. Override `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` if needed. |
| Backend cannot call AI service | Start FastAPI first and confirm `http://127.0.0.1:8000/health`. Override `AI_ADVISORY_BASE_URL` only when using a different host/port. |
| Frontend requests fail | Confirm backend health, then check that the frontend API base URL is `http://127.0.0.1:8080/api` for direct local development. |
| Synthetic reference data is missing | Restart the backend using `synthetic-data`. This resets synthetic data, so never enable it against a database with retained work. |
| Port already in use | Stop the conflicting process or choose another port and update the related service URL configuration. |

## 10. Stop services

Use `Ctrl+C` in each service terminal. The direct local installation leaves PostgreSQL running, which is normal; stop it through your OS service manager only when you no longer need it.
