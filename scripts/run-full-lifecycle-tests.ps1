[CmdletBinding()]
param(
    [switch]$SkipBackend,
    [switch]$SkipAiService,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$artifactDirectory = Join-Path $repositoryRoot "artifacts/full-lifecycle"
$reportPath = Join-Path $artifactDirectory "$(Get-Date -Format 'yyyyMMddHHmmss').report.json"
$gates = [System.Collections.Generic.List[object]]::new()
$startedAt = (Get-Date).ToUniversalTime().ToString("o")
$result = "PASS"
$failureMessage = $null
$previousProfile = $env:SPRING_PROFILES_ACTIVE
$previousJwtSecret = $env:JWT_SECRET

function Invoke-Gate {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [string]$WorkingDirectory,
        [Parameter(Mandatory)] [scriptblock]$Operation
    )

    $gateStartedAt = Get-Date
    Write-Host "Running $Name..."
    Push-Location $WorkingDirectory
    try {
        & $Operation
        if ($LASTEXITCODE -ne 0) {
            throw "$Name exited with code $LASTEXITCODE."
        }
        $gates.Add([ordered]@{
                name = $Name
                result = "PASS"
                durationSeconds = [math]::Round(((Get-Date) - $gateStartedAt).TotalSeconds, 2)
            })
    } catch {
        $gates.Add([ordered]@{
                name = $Name
                result = "FAIL"
                durationSeconds = [math]::Round(((Get-Date) - $gateStartedAt).TotalSeconds, 2)
                failure = $_.Exception.Message
            })
        throw
    } finally {
        Pop-Location
    }
}

try {
    # Keep the backend suite in its deterministic test profile without storing a
    # real credential in the repository. A caller can still provide its own secret.
    $env:SPRING_PROFILES_ACTIVE = "test"
    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
        $env:JWT_SECRET = "full-lifecycle-test-signing-secret-at-least-32-bytes-long"
    }

    if (-not $SkipBackend) {
        Invoke-Gate -Name "backend Maven test suite" -WorkingDirectory (Join-Path $repositoryRoot "backend") -Operation {
            & .\mvnw.cmd clean test
        }
    }

    if (-not $SkipAiService) {
        Invoke-Gate -Name "AI service pytest suite" -WorkingDirectory (Join-Path $repositoryRoot "ai-service") -Operation {
            & py -3 -m pytest -q
        }
    }

    if (-not $SkipFrontend) {
        Invoke-Gate -Name "frontend lint" -WorkingDirectory (Join-Path $repositoryRoot "frontend") -Operation {
            & npm run lint
        }
        Invoke-Gate -Name "frontend production build" -WorkingDirectory (Join-Path $repositoryRoot "frontend") -Operation {
            & npm run build
        }
    }

    Write-Host "PASS: all selected full-lifecycle gates completed."
} catch {
    $result = "FAIL"
    $failureMessage = $_.Exception.Message
    Write-Error $failureMessage
    exit 1
} finally {
    $env:SPRING_PROFILES_ACTIVE = $previousProfile
    $env:JWT_SECRET = $previousJwtSecret

    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    [ordered]@{
        startedAt = $startedAt
        completedAt = (Get-Date).ToUniversalTime().ToString("o")
        result = $result
        gates = $gates
        failure = $failureMessage
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $reportPath -Encoding utf8
    Write-Host "Full-lifecycle report: $reportPath"
}
