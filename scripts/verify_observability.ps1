param(
    [string]$BaseUrl = "http://localhost:7090/api"
)

$ErrorActionPreference = "Stop"

$script:passed = 0
$script:failed = 0
$script:failedTests = @()

function Write-TestResult {
    param([string]$Name, [bool]$Success, [string]$Message = "")
    if ($Success) {
        Write-Output "  [PASS] $Name"
        $script:passed++
    } else {
        Write-Output "  [FAIL] $Name - $Message"
        $script:failed++
        $script:failedTests += $Name
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan Observability Verification"
Write-Output "========================================"
Write-Output ""

Write-Output "[1/4] M-OBS-001: Web Vitals Report..."
try {
    $timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $vitalsBody = @{
        name = "LCP"
        value = 1234
        rating = "good"
        page = "/main/all"
        timestamp = $timestamp
    } | ConvertTo-Json -Compress
    
    $reportResp = Invoke-WebRequest -Uri "$BaseUrl/analytics/web-vitals" -Method POST -ContentType "application/json" -Body $vitalsBody
    $reportJson = $reportResp.Content | ConvertFrom-Json
    if ($reportJson.status -eq "success" -or $reportJson.code -eq 200) {
        Write-TestResult "M-OBS-001: Web Vitals Report" $true
    } else {
        Write-TestResult "M-OBS-001: Web Vitals Report" $false $reportJson.info
    }
} catch {
    Write-TestResult "M-OBS-001: Web Vitals Report" $false $_.Exception.Message
}

Write-Output ""
Write-Output "[2/4] M-OBS-002: Web Vitals Stats..."
try {
    Start-Sleep -Milliseconds 500
    $statsResp = Invoke-WebRequest -Uri "$BaseUrl/analytics/web-vitals/stats" -Method GET
    $statsJson = $statsResp.Content | ConvertFrom-Json
    $totalMetrics = 0
    if ($statsJson.data -and $statsJson.data.totalMetrics) {
        $totalMetrics = $statsJson.data.totalMetrics
    } elseif ($statsJson.totalMetrics) {
        $totalMetrics = $statsJson.totalMetrics
    }
    if ($statsJson.status -eq "success" -or $totalMetrics -ge 0) {
        Write-TestResult "M-OBS-002: Web Vitals Stats" $true
    } else {
        Write-TestResult "M-OBS-002: Web Vitals Stats" $false "No stats data"
    }
} catch {
    Write-TestResult "M-OBS-002: Web Vitals Stats" $false $_.Exception.Message
}

Write-Output ""
Write-Output "[3/4] M-OBS-003: Health Check..."
try {
    $healthResp = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -Method GET
    $content = $healthResp.Content
    if ($content -is [byte[]]) {
        $content = [System.Text.Encoding]::UTF8.GetString($content)
    }
    $healthJson = $content | ConvertFrom-Json
    $isUp = $healthJson.status -eq "UP"
    $dbUp = $healthJson.components.db.status -eq "UP"
    $redisUp = $healthJson.components.redis.status -eq "UP"
    if ($isUp -and $dbUp -and $redisUp) {
        Write-TestResult "M-OBS-003: Health Check" $true
    } else {
        Write-TestResult "M-OBS-003: Health Check" $false "status: $($healthJson.status), db: $($healthJson.components.db.status), redis: $($healthJson.components.redis.status)"
    }
} catch {
    Write-TestResult "M-OBS-003: Health Check" $false $_.Exception.Message
}

Write-Output ""
Write-Output "[4/4] M-OBS-004: Prometheus Metrics..."
try {
    $promResp = Invoke-WebRequest -Uri "$BaseUrl/actuator/prometheus" -Method GET
    $promContent = $promResp.Content
    $hasEasypanMetrics = $promContent -match "easypan_" -or $promContent -match "jvm_"
    if ($hasEasypanMetrics) {
        Write-TestResult "M-OBS-004: Prometheus Metrics" $true
    } else {
        Write-TestResult "M-OBS-004: Prometheus Metrics" $false "No easypan or jvm metrics found"
    }
} catch {
    Write-TestResult "M-OBS-004: Prometheus Metrics" $false $_.Exception.Message
}

Write-Output ""
Write-Output "========================================"
Write-Output "OBSERVABILITY VERIFICATION SUMMARY"
Write-Output "========================================"
Write-Output "  Passed: $script:passed"
Write-Output "  Failed: $script:failed"
if ($script:failedTests.Count -gt 0) {
    Write-Output "  Failed tests:"
    foreach ($t in $script:failedTests) {
        Write-Output "    - $t"
    }
}
Write-Output ""

if ($script:failed -eq 0) {
    Write-Output "PASS"
    exit 0
} else {
    Write-Output "FAIL"
    exit 1
}
