# IATERAP Docker Compose deployment

This guide is kept beside `docker-compose.yml` so the deployment instructions and Compose stack evolve together.

## Prerequisites

- Docker Desktop with Compose v2.
- Git and a checked-out repository revision.
- Ports selected for PostgreSQL and the public Nginx entry point.
- A unique JWT signing secret of at least 32 bytes.

## Configure and start

```powershell
Copy-Item docker/.env.example docker/.env
```

Set unique values in `docker/.env`; do not commit it. Then start the stack:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up --build -d
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
```

The public application is `http://localhost:${APP_PORT}`. Nginx serves the frontend and proxies `/api/**` to Spring Boot. PostgreSQL and the AI service are private Compose dependencies except for the optional PostgreSQL diagnostic port.

## Verify

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml config -q
Invoke-WebRequest http://localhost:<APP_PORT>/
Invoke-WebRequest http://localhost:<APP_PORT>/api/health
docker compose --env-file docker/.env -f docker/docker-compose.yml logs --tail 100 backend ai-service nginx
```

All services should report healthy in `docker compose ps` before declaring deployment ready.

## Operations

Follow logs:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml logs -f backend ai-service nginx
```

Stop without deleting data:

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml down
```

`down -v` deletes PostgreSQL data and must only be used for deliberate local reset or after a verified backup.

## Backup, restore and rollback

Create a compressed PostgreSQL snapshot:

```powershell
.\scripts\backup-compose-postgres.ps1
```

Schedule it daily in Windows Task Scheduler by creating a task that runs the following program and arguments under the account that runs Docker Desktop:

```text
Program: powershell.exe
Arguments: -NoProfile -ExecutionPolicy Bypass -File <repository>\scripts\backup-compose-postgres.ps1
```

Snapshots are retained for 14 days by default under `backup/postgres/`. Use `-RetentionDays` to change the local retention window.

Restore a snapshot only into a new isolated database:

```powershell
.\scripts\restore-compose-postgres.ps1 -BackupFile .\backup\postgres\<snapshot>.dump
```

The restore command rejects the configured source database as a target. Inspect the restored database before any approved DBA-led promotion. For a regular restore drill with row-count evidence, run `scripts/verify-seeded-backup-restore.ps1`.

Before an application upgrade, take a snapshot, update to the approved revision, and rebuild:

```powershell
git fetch origin
git checkout <approved-revision>
docker compose --env-file docker/.env -f docker/docker-compose.yml up --build -d
```

If application rollback is required, return to the last approved Git revision and rebuild. Database migration rollback is not automatic; restore from a verified snapshot only through the approved recovery process.

## Security

- Never commit `docker/.env`, dumps, backup manifests, or secrets.
- Use distinct production values for database credentials and `JWT_SECRET`.
- Restrict access to the Docker host and backup directory.
- Use TLS termination and a managed secret store for non-local deployment.
