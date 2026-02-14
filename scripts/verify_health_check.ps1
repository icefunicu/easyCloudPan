param(
    [string]$BaseUrl = "http://localhost:7090/api"
)

$ErrorActionPreference = "Stop"

Write-Output "Checking backend health at $BaseUrl/actuator/health..."

try {
    $resp = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method GET

    if ($null -eq $resp) {
        throw "FAIL: /actuator/health returned empty response body."
    }

    $status = [string]$resp.status
    if ($status -ne "UP") {
        throw "FAIL: /actuator/health status=$status (expected UP)"
    }
    Write-Output "  Overall status: UP"

    $components = $resp.components
    if ($null -eq $components) {
        throw "FAIL: /actuator/health missing components (expected db/redis)"
    }

    $dbStatus = [string]$components.db.status
    if ($dbStatus -ne "UP") {
        throw "FAIL: db component status=$dbStatus (expected UP)"
    }
    Write-Output "  db component: UP"

    $redisStatus = [string]$components.redis.status
    if ($redisStatus -ne "UP") {
        throw "FAIL: redis component status=$redisStatus (expected UP)"
    }
    Write-Output "  redis component: UP"

    Write-Output "PASS: Backend health check passed (status=UP, db=UP, redis=UP)."
} catch {
    throw "FAIL: Health check failed - $($_.Exception.Message)"
}
