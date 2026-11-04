# Seeded database backup and restore verification

| Document information | Details |
| --- | --- |
| Jira | AI-284 / T18-SH |
| Script | `scripts/verify-seeded-backup-restore.ps1` |
| Scope | Local Docker Compose PostgreSQL only |

This verification proves that a backup from the configured seeded PostgreSQL database can be restored into a separate disposable database. It never restores over the source database.

## Preconditions

1. Docker Desktop is running.
2. `docker/.env` exists and has been populated from `docker/.env.example`.
3. The Compose PostgreSQL service contains the seeded dataset to test. Start the backend with the `synthetic-data` profile before the verification when a fresh seeded copy is required.

## Run

From the repository root in PowerShell:

```powershell
.\scripts\verify-seeded-backup-restore.ps1
```

The script performs these guarded steps:

1. Starts and checks the Compose PostgreSQL service.
2. Captures row counts for core seeded-data tables.
3. Creates a PostgreSQL custom-format dump under the ignored `backup/` directory.
4. Creates a timestamped verification database using `template0`.
5. Restores the dump only into that verification database.
6. Compares `lru`, `test_procedure`, `test_step`, `procedure_history`, `coverage_report`, and `audit_log` row counts between source and restored databases.
7. Writes an ignored JSON evidence report under `backup/` and drops the verification database.

Use `-KeepRestoredDatabase` only for troubleshooting:

```powershell
.\scripts\verify-seeded-backup-restore.ps1 -KeepRestoredDatabase
```

## Safety properties

- The source database is never dropped, overwritten, or restored into.
- The verification target has a generated timestamped name and is dropped after a successful or failed run unless explicitly kept.
- The script rejects unsafe database identifiers from the local environment file.
- Backup dumps and reports are ignored by Git so database contents are not committed.

## Evidence to record

Attach or retain the generated `backup/*.report.json` file with the week’s verification record. A `PASS` report records the source/verification names, timestamp, dump location, and matched table counts.
