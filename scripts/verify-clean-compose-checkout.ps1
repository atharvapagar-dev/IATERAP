[CmdletBinding()]
param(
    [string]$RepositoryUrl = "https://github.com/atharvapagar-dev/IATERAP.git",
    [string]$Branch = "develop",
    [int]$TimeoutSeconds = 300,
    [switch]$KeepCheckout
)

$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$verificationId = Get-Date -Format "yyyyMMddHHmmss"
$projectName = "iaterap-clean-$verificationId"
$temporaryCheckout = Join-Path ([System.IO.Path]::GetTempPath()) $projectName
$artifactDirectory = Join-Path $repositoryRoot "artifacts/clean-compose-verification"
$reportPath = Join-Path $artifactDirectory "$verificationId.report.json"
$appPort = 18080
$postgresPort = 15432
$result = "FAIL"
$failureMessage = $null
$cleanupResult = "NOT_STARTED"

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name is required to run clean-checkout verification."
    }
}

function Get-FreePort([int]$StartingPort) {
    for ($port = $StartingPort; $port -lt ($StartingPort + 100); $port++) {
        $listener = $null
        try {
            $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
            $listener.Start()
            return $port
        } catch {
            continue
        } finally {
            if ($listener) { $listener.Stop() }
        }
    }
    throw "Could not find a free TCP port starting at $StartingPort."
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --project-name $projectName --env-file "docker/.env" -f "docker/docker-compose.yml" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose command failed: $($Arguments -join ' ')"
    }
}

Require-Command git
Require-Command docker

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop must be running before clean-checkout verification can start."
}

try {
    $appPort = Get-FreePort 18080
    $postgresPort = Get-FreePort 15432

    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    Write-Host "Cloning $Branch into an isolated temporary checkout..."
    & git clone --depth 1 --branch $Branch $RepositoryUrl $temporaryCheckout
    if ($LASTEXITCODE -ne 0) { throw "Git clone failed for $RepositoryUrl ($Branch)." }

    $environmentFile = Join-Path $temporaryCheckout "docker/.env"
    $environment = @(
        "POSTGRES_DB=iaterap_verify",
        "POSTGRES_USER=iaterap_verify",
        "POSTGRES_PASSWORD=local-verification-password-not-for-production",
        "POSTGRES_PORT=$postgresPort",
        "APP_PORT=$appPort",
        "JWT_SECRET=clean-checkout-verification-secret-at-least-32-bytes-long",
        "AI_ADVISORY_BASE_URL=http://ai-service:8000"
    )
    Set-Content -LiteralPath $environmentFile -Value $environment -Encoding utf8

    Push-Location $temporaryCheckout
    try {
        Write-Host "Validating the cloned Compose configuration..."
        Invoke-Compose @("config", "-q")

        Write-Host "Building and starting all Compose services..."
        Invoke-Compose @("up", "--build", "--detach", "--wait", "--wait-timeout", "$TimeoutSeconds")

        $services = & docker compose --project-name $projectName --env-file "docker/.env" -f "docker/docker-compose.yml" ps --format json | ConvertFrom-Json
        $unhealthy = @($services | Where-Object { $_.Health -and $_.Health -ne "healthy" })
        if ($unhealthy.Count -gt 0) {
            throw "One or more Compose services are not healthy: $($unhealthy.Name -join ', ')."
        }

        $frontendResponse = Invoke-WebRequest -Uri "http://localhost:$appPort/" -UseBasicParsing
        if ($frontendResponse.StatusCode -ne 200) { throw "Frontend returned HTTP $($frontendResponse.StatusCode)." }

        $healthResponse = Invoke-WebRequest -Uri "http://localhost:$appPort/api/health" -UseBasicParsing
        if ($healthResponse.StatusCode -ne 200) { throw "Backend health endpoint returned HTTP $($healthResponse.StatusCode)." }

        $result = "PASS"
        Write-Host "PASS: clean checkout Compose stack is healthy at http://localhost:$appPort/."
    } finally {
        Pop-Location
    }
} catch {
    $failureMessage = $_.Exception.Message
    Write-Error $failureMessage
    throw
} finally {
    if (Test-Path -LiteralPath $temporaryCheckout) {
        try {
            Push-Location $temporaryCheckout
            try {
                & docker compose --project-name $projectName --env-file "docker/.env" -f "docker/docker-compose.yml" down --volumes --remove-orphans
                if ($LASTEXITCODE -ne 0) { throw "Docker Compose cleanup failed." }
                $cleanupResult = "COMPLETED"
            } finally {
                Pop-Location
            }
        } catch {
            $cleanupResult = "FAILED: $($_.Exception.Message)"
            Write-Warning $cleanupResult
        }
    }

    $report = [ordered]@{
        verifiedAt = (Get-Date).ToUniversalTime().ToString("o")
        result = $result
        repositoryUrl = $RepositoryUrl
        branch = $Branch
        composeProject = $projectName
        appPort = $appPort
        postgresPort = $postgresPort
        cleanup = $cleanupResult
        failure = $failureMessage
    } | ConvertTo-Json
    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    Set-Content -LiteralPath $reportPath -Value $report -Encoding utf8
    Write-Host "Verification report: $reportPath"

    if ((Test-Path -LiteralPath $temporaryCheckout) -and -not $KeepCheckout) {
        Remove-Item -LiteralPath $temporaryCheckout -Recurse -Force
    } elseif (Test-Path -LiteralPath $temporaryCheckout) {
        Write-Host "Temporary checkout retained: $temporaryCheckout"
    }
}
