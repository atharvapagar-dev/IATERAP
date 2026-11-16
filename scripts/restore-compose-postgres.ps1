[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupFile,
    [string]$EnvironmentFile = "docker/.env",
    [string]$TargetDatabase
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot "docker/docker-compose.yml"
$environmentPath = Join-Path $repositoryRoot $EnvironmentFile
$postgresContainer = $null

function Get-ComposeEnvironment {
    $values = @{}
    Get-Content -LiteralPath $environmentPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split "=", 2
            if ($parts.Length -eq 2) { $values[$parts[0].Trim()] = $parts[1].Trim() }
        }
    }
    return $values
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $environmentPath -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose command failed: $($Arguments -join ' ')" }
}

if (-not (Test-Path -LiteralPath $BackupFile)) { throw "Backup file not found: $BackupFile" }
if (-not (Test-Path -LiteralPath $composeFile)) { throw "Docker Compose file not found: $composeFile" }
if (-not (Test-Path -LiteralPath $environmentPath)) { throw "Environment file not found: $environmentPath" }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "Docker CLI is required." }

$environment = Get-ComposeEnvironment
$sourceDatabase = $environment.POSTGRES_DB
$databaseUser = $environment.POSTGRES_USER
if (-not $sourceDatabase -or -not $databaseUser) { throw "POSTGRES_DB and POSTGRES_USER must be set in $EnvironmentFile." }
if (-not $TargetDatabase) { $TargetDatabase = "${sourceDatabase}_restore_$(Get-Date -Format 'yyyyMMddHHmmss')" }
foreach ($identifier in @($sourceDatabase, $databaseUser, $TargetDatabase)) {
    if ($identifier -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") { throw "Database identifiers may contain only letters, digits and underscores." }
}
if ($TargetDatabase -eq $sourceDatabase) { throw "TargetDatabase must never be the configured source database." }

$containerBackupFile = "/tmp/$TargetDatabase.dump"
try {
    Invoke-Compose @("up", "-d", "postgres")
    Invoke-Compose @("exec", "-T", "postgres", "pg_isready", "-U", $databaseUser, "-d", $sourceDatabase)
    $postgresContainer = (& docker compose --env-file $environmentPath -f $composeFile ps -q postgres).Trim()
    if ([string]::IsNullOrWhiteSpace($postgresContainer)) { throw "Could not resolve PostgreSQL container ID." }

    $alreadyExists = Invoke-Compose @("exec", "-T", "postgres", "psql", "-U", $databaseUser, "-d", "postgres", "-At", "-c", "SELECT 1 FROM pg_database WHERE datname = '$TargetDatabase';")
    if ($alreadyExists -match "1") { throw "Target database '$TargetDatabase' already exists; choose a new target name." }

    & docker cp $BackupFile "${postgresContainer}:$containerBackupFile"
    if ($LASTEXITCODE -ne 0) { throw "Could not copy backup into PostgreSQL container." }
    Invoke-Compose @("exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1", "-U", $databaseUser, "-d", "postgres", "-c", "CREATE DATABASE $TargetDatabase TEMPLATE template0;")
    Invoke-Compose @("exec", "-T", "postgres", "pg_restore", "--exit-on-error", "-U", $databaseUser, "-d", $TargetDatabase, $containerBackupFile)
    Write-Host "PASS: restored '$BackupFile' into isolated database '$TargetDatabase'."
    Write-Host "The source database '$sourceDatabase' was not changed. Inspect or promote the restored database through the approved DBA process."
} finally {
    if ($postgresContainer) { & docker exec $postgresContainer rm -f $containerBackupFile 2>$null }
}
