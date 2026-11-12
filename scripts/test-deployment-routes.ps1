[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:18080",
    [string]$Username = $env:DEPLOYMENT_REGRESSION_USERNAME,
    [string]$Password = $env:DEPLOYMENT_REGRESSION_PASSWORD,
    [string]$OutputDirectory = "artifacts/deployment-regression"
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$resolvedOutputDirectory = Join-Path $repositoryRoot $OutputDirectory
$reportPath = Join-Path $resolvedOutputDirectory "deployment-regression-$(Get-Date -Format 'yyyyMMddHHmmss').json"
$base = $BaseUrl.TrimEnd("/")
$result = "FAIL"
$failureMessage = $null

$screenRoutes = @(
    "/", "/health", "/login", "/workspace", "/projects", "/projects/create",
    "/aircraft", "/aircraft/create", "/subsystems", "/subsystems/create",
    "/lrus", "/lrus/create", "/signal-libraries", "/signal-libraries/create",
    "/protocol-tags", "/protocol-tags/create", "/fault-templates", "/fault-templates/create",
    "/fmea-branch-mappings", "/fmea-branch-mappings/create", "/procedures",
    "/procedures/create", "/procedures/review-queue", "/audit-logs", "/admin/rules",
    "/coverage-dashboard", "/validation-reports", "/reliability-dashboard", "/ai-suggestions"
)

$protectedApiRoutes = @(
    "/api/projects", "/api/aircraft", "/api/subsystems", "/api/lrus",
    "/api/signal-libraries", "/api/protocol-tags", "/api/fault-templates",
    "/api/fmea-branch-mappings", "/api/procedures", "/api/audit-logs",
    "/api/admin/rules", "/api/coverage/heatmap", "/api/reliability/dashboard",
    "/api/reliability/reports/current", "/api/ai/suggestions/queue"
)

function Assert-Status([object]$Response, [string]$Target) {
    if ($Response.StatusCode -ne 200) { throw "$Target returned HTTP $($Response.StatusCode)." }
}

if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
    throw "Provide deployment regression credentials through -Username/-Password or DEPLOYMENT_REGRESSION_USERNAME/DEPLOYMENT_REGRESSION_PASSWORD."
}

try {
    $health = Invoke-WebRequest -Uri "$base/api/health" -UseBasicParsing
    Assert-Status $health "$base/api/health"

    $loginPayload = @{ username = $Username; password = $Password } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" -Body $loginPayload
    if ([string]::IsNullOrWhiteSpace($login.accessToken)) { throw "Login response did not contain an access token." }
    $headers = @{ Authorization = "Bearer $($login.accessToken)" }

    foreach ($route in $screenRoutes) {
        $response = Invoke-WebRequest -Uri "$base$route" -UseBasicParsing
        Assert-Status $response "$base$route"
        if ($response.Content -notmatch '<div id="root">') { throw "$base$route did not return the frontend application shell." }
    }

    foreach ($route in $protectedApiRoutes) {
        $response = Invoke-WebRequest -Uri "$base$route" -Headers $headers -UseBasicParsing
        Assert-Status $response "$base$route"
    }

    $result = "PASS"
    Write-Host "PASS: $($screenRoutes.Count) deployed screens and $($protectedApiRoutes.Count) protected API reads passed through Nginx."
} catch {
    $failureMessage = $_.Exception.Message
    Write-Error $failureMessage
    throw
} finally {
    New-Item -ItemType Directory -Path $resolvedOutputDirectory -Force | Out-Null
    [ordered]@{
        verifiedAt = (Get-Date).ToUniversalTime().ToString("o")
        baseUrl = $base
        result = $result
        screenRouteCount = $screenRoutes.Count
        protectedApiRouteCount = $protectedApiRoutes.Count
        failure = $failureMessage
    } | ConvertTo-Json | Set-Content -LiteralPath $reportPath -Encoding utf8
    Write-Host "Deployment regression report: $reportPath"
}
