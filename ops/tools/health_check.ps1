# EasyCloudPan Health Check Script
# Usage: .\ops\tools\health_check.ps1

$ErrorActionPreference = "Continue"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$DockerDir = "$RepoRoot\ops\docker"

function Print-Header {
    param([string]$Title)
    Write-Host ("=" * 74)
    Write-Host $Title
    Write-Host ("=" * 74)
}

Print-Header "EasyCloudPan Service Health Check"

$allOk = $true

# Check Docker container status
Write-Host "`n[1/5] Checking Docker container status..."
Push-Location $DockerDir
docker compose ps 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[WARN] Cannot get container status. Docker may not be running." -ForegroundColor Yellow
    $allOk = $false
}
Pop-Location

# Check PostgreSQL
Write-Host "`n[2/5] Checking PostgreSQL database..."
try {
    $result = docker exec easypan-postgres pg_isready -U postgres -d easypan 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] PostgreSQL is running" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] PostgreSQL is not ready" -ForegroundColor Red
        $allOk = $false
    }
} catch {
    Write-Host "[FAIL] PostgreSQL is not ready" -ForegroundColor Red
    $allOk = $false
}

# Check Redis
Write-Host "`n[3/5] Checking Redis..."
try {
    $result = docker exec easypan-redis redis-cli -a password123 ping 2>&1
    if ($result -match "PONG") {
        Write-Host "[OK] Redis is running" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Redis is not ready" -ForegroundColor Red
        $allOk = $false
    }
} catch {
    Write-Host "[FAIL] Redis is not ready" -ForegroundColor Red
    $allOk = $false
}

# Check MinIO
Write-Host "`n[4/5] Checking MinIO..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:9000/minio/health/live" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "[OK] MinIO is running" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] MinIO is not ready" -ForegroundColor Red
        $allOk = $false
    }
} catch {
    Write-Host "[FAIL] MinIO is not ready" -ForegroundColor Red
    $allOk = $false
}

# Check Backend API
Write-Host "`n[5/5] Checking Backend API..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:7090/api/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "[OK] Backend API is running" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Backend API not responding (if local dev mode, please start backend first)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARN] Backend API not responding (if local dev mode, please start backend first)" -ForegroundColor Yellow
}

Write-Host "`n" + ("=" * 74)
if ($allOk) {
    Write-Host "[SUCCESS] All infrastructure services are running!" -ForegroundColor Green
    Write-Host "`nAccess URLs:"
    Write-Host "  Frontend: http://localhost:8080"
    Write-Host "  Backend : http://localhost:7090/api"
    Write-Host "  MinIO   : http://localhost:9001"
} else {
    Write-Host "[FAILED] Some services are not ready. Please check the output above." -ForegroundColor Red
    Write-Host "`nTroubleshooting tips:"
    Write-Host "  1. Make sure Docker Desktop is running"
    Write-Host "  2. Run .\ops\local\startup.ps1 to start services"
    Write-Host "  3. Check logs: docker compose -f ops\docker\docker-compose.yml logs"
}
Write-Host ("=" * 74)
