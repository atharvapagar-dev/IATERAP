[CmdletBinding()]
param(
    [string]$EnvironmentFile = "docker/.env",
    [string]$BackupDirectory = "backup/postgres",
    [ValidateRange(1, 3650)]
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot "docker/docker-compose.yml"
$environmentPath = Join-Path $repositoryRoot $EnvironmentFile
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$containerBackupFile = "/tmp/iaterap-$timestamp.dump"
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

if (-not (Test-Path -LiteralPath $composeFile)) { throw "Docker Compose file not found: $composeFile" }
if (-not (Test-Path -LiteralPath $environmentPath)) { throw "Environment file not found: $environmentPath" }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "Docker CLI is required." }

$environment = Get-ComposeEnvironment
foreach ($key in "POSTGRES_DB", "POSTGRES_USER") {
    if (-not $environment[$key]) { throw "$key must be set in $EnvironmentFile." }
}
$database = $environment.POSTGRES_DB
$databaseUser = $environment.POSTGRES_USER
if ($database -notmatch "^[A-Za-z_][A-Za-z0-9_]*$" -or $databaseUser -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
    throw "POSTGRES_DB and POSTGRES_USER must be PostgreSQL identifiers."
}

$resolvedBackupDirectory = Join-Path $repositoryRoot $BackupDirectory
New-Item -ItemType Directory -Path $resolvedBackupDirectory -Force | Out-Null
$backupFile = Join-Path $resolvedBackupDirectory "$database-$timestamp.dump"
$manifestFile = Join-Path $resolvedBackupDirectory "$database-$timestamp.json"

try {
    Invoke-Compose @("up", "-d", "postgres")
    Invoke-Compose @("exec", "-T", "postgres", "pg_isready", "-U", $databaseUser, "-d", $database)
    $postgresContainer = (& docker compose --env-file $environmentPath -f $composeFile ps -q postgres).Trim()
    if ([string]::IsNullOrWhiteSpace($postgresContainer)) { throw "Could not resolve PostgreSQL container ID." }

    Write-Host "Creating PostgreSQL snapshot: $backupFile"
    Invoke-Compose @("exec", "-T", "postgres", "pg_dump", "-U", $databaseUser, "-d", $database, "-Fc", "-Z", "9", "-f", $containerBackupFile)
    & docker cp "${postgresContainer}:$containerBackupFile" $backupFile
    if ($LASTEXITCODE -ne 0) { throw "Could not copy database snapshot from PostgreSQL container." }

    $hash = (Get-FileHash -LiteralPath $backupFile -Algorithm SHA256).Hash
    [ordered]@{
        createdAt = (Get-Date).ToUniversalTime().ToString("o")
        database = $database
        format = "pg_dump custom"
        backupFile = $backupFile
        sha256 = $hash
        retentionDays = $RetentionDays
        result = "PASS"
    } | ConvertTo-Json | Set-Content -LiteralPath $manifestFile -Encoding utf8

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -LiteralPath $resolvedBackupDirectory -File |
        Where-Object { $_.LastWriteTime -lt $cutoff -and $_.Extension -in ".dump", ".json" } |
        Remove-Item -Force
    Write-Host "PASS: snapshot created. Manifest: $manifestFile"
} finally {
    if ($postgresContainer) { & docker exec $postgresContainer rm -f $containerBackupFile 2>$null }
}
