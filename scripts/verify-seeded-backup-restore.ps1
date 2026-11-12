[CmdletBinding()]
param(
    [string]$EnvironmentFile = "docker/.env",
    [switch]$KeepRestoredDatabase
)

$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot "docker/docker-compose.yml"
$environmentPath = Join-Path $repositoryRoot $EnvironmentFile

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Docker Compose file not found: $composeFile"
}
if (-not (Test-Path -LiteralPath $environmentPath)) {
    throw "Environment file not found: $environmentPath. Copy docker/.env.example to docker/.env and set its values."
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is required. Start Docker Desktop and retry."
}

$environment = @{}
Get-Content -LiteralPath $environmentPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2
        if ($parts.Length -eq 2) { $environment[$parts[0].Trim()] = $parts[1].Trim() }
    }
}

foreach ($key in "POSTGRES_DB", "POSTGRES_USER") {
    if (-not $environment.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($environment[$key])) {
        throw "$key must be set in $EnvironmentFile."
    }
}

$sourceDatabase = $environment.POSTGRES_DB
$databaseUser = $environment.POSTGRES_USER
if ($sourceDatabase -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
    throw "POSTGRES_DB must be a PostgreSQL identifier (letters, digits, underscores)."
}
if ($databaseUser -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
    throw "POSTGRES_USER must be a PostgreSQL identifier (letters, digits, underscores)."
}

$verificationDatabase = "${sourceDatabase}_restore_verify_$(Get-Date -Format 'yyyyMMddHHmmss')"
$backupDirectory = Join-Path $repositoryRoot "backup"
$backupFile = Join-Path $backupDirectory "$verificationDatabase.dump"
$containerBackupFile = "/tmp/$verificationDatabase.dump"
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $environmentPath -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose command failed: $($Arguments -join ' ')" }
}

function Invoke-Psql([string]$Database, [string]$Sql) {
    Invoke-Compose @("exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1", "-U", $databaseUser, "-d", $Database, "-At", "-c", $Sql)
}

function Get-RowCounts([string]$Database) {
    $tables = @("lru", "test_procedure", "test_step", "procedure_history", "coverage_report", "audit_log")
    $result = [ordered]@{}
    foreach ($table in $tables) {
        $output = Invoke-Psql $Database "SELECT count(*) FROM public.$table;"
        $result[$table] = [long](($output | Select-Object -Last 1).Trim())
    }
    return $result
}

try {
    Invoke-Compose @("up", "-d", "postgres")
    Invoke-Compose @("exec", "-T", "postgres", "pg_isready", "-U", $databaseUser, "-d", $sourceDatabase)
    $postgresContainer = (& docker compose --env-file $environmentPath -f $composeFile ps -q postgres).Trim()
    if ([string]::IsNullOrWhiteSpace($postgresContainer)) { throw "Could not resolve the PostgreSQL container ID." }

    Write-Host "Capturing source row counts from '$sourceDatabase'..."
    $sourceCounts = Get-RowCounts $sourceDatabase

    Write-Host "Creating backup at '$backupFile'..."
    Invoke-Compose @("exec", "-T", "postgres", "sh", "-c", "pg_dump -U $databaseUser -d $sourceDatabase -Fc -f $containerBackupFile")
    & docker cp "${postgresContainer}:$containerBackupFile" $backupFile
    if ($LASTEXITCODE -ne 0) { throw "Could not copy the PostgreSQL backup to '$backupFile'." }

    Write-Host "Creating isolated verification database '$verificationDatabase'..."
    Invoke-Psql "postgres" "CREATE DATABASE $verificationDatabase TEMPLATE template0;"

    Write-Host "Restoring backup into isolated verification database..."
    Invoke-Compose @("exec", "-T", "postgres", "pg_restore", "-U", $databaseUser, "-d", $verificationDatabase, "--exit-on-error", $containerBackupFile)

    Write-Host "Comparing restored row counts..."
    $restoredCounts = Get-RowCounts $verificationDatabase
    $mismatches = @($sourceCounts.Keys | Where-Object { $sourceCounts[$_] -ne $restoredCounts[$_] })
    if ($mismatches.Count -gt 0) {
        $details = $mismatches | ForEach-Object { "$_ (source=$($sourceCounts[$_]), restored=$($restoredCounts[$_]))" }
        throw "Backup restore verification failed: $($details -join '; ')"
    }

    $report = [ordered]@{
        verifiedAt = (Get-Date).ToUniversalTime().ToString("o")
        sourceDatabase = $sourceDatabase
        restoredDatabase = $verificationDatabase
        backupFile = $backupFile
        rowCounts = $restoredCounts
        result = "PASS"
    } | ConvertTo-Json -Depth 4
    $reportPath = Join-Path $backupDirectory "$verificationDatabase.report.json"
    Set-Content -LiteralPath $reportPath -Value $report -Encoding utf8
    Write-Host "PASS: backup restored and verified. Report: $reportPath"
}
finally {
    if ($postgresContainer) {
        & docker exec $postgresContainer rm -f $containerBackupFile 2>$null
    }
    if (-not $KeepRestoredDatabase -and $verificationDatabase) {
        try {
            Write-Host "Removing isolated verification database '$verificationDatabase'..."
            Invoke-Psql "postgres" "DROP DATABASE IF EXISTS $verificationDatabase WITH (FORCE);"
        } catch {
            Write-Warning "Could not remove verification database '$verificationDatabase': $($_.Exception.Message)"
        }
    }
}
