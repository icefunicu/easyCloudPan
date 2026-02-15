# EasyCloudPan Local One-Click Startup Script
# Usage: .\ops\local\startup.ps1 [-NoBrowser] [-AllowDevMode] [-ExposeCaptcha]

param(
    [switch]$NoBrowser,
    [switch]$AllowDevMode,
    [switch]$ExposeCaptcha
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Get-Item $PSScriptRoot).Parent.Parent.FullName
$DockerDir = "$RepoRoot\ops\docker"
$EnvFile = "$DockerDir\.env"

function Print-Header {
    param([string]$Title)
    Write-Host "=" * 74
    Write-Host $Title
    Write-Host "=" * 74
}

# Check config file
if (-not (Test-Path $EnvFile)) {
    Write-Host "[ERROR] $EnvFile not found." -ForegroundColor Red
    Write-Host "Run .\ops\local\setup.ps1 first to create it."
    exit 1
}

Print-Header "EasyCloudPan One-Click Local Start"

# Load environment variables
Write-Host "[INFO] Loading environment variables from $EnvFile..."
Get-Content $EnvFile | Where-Object { $_ -notmatch '^#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_.Split('=', 2)
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
}

if (-not $AllowDevMode) {
    # Enforce real authentication flow by default in local one-click startup.
    $env:DEV_MODE = "false"
    Write-Host "[INFO] DEV_MODE is forced to false for auth verification. Use -AllowDevMode to keep bypass."
}
else {
    Write-Host "[WARN] DEV_MODE from .env is kept because -AllowDevMode is specified."
}

if ($ExposeCaptcha) {
    # Convenience for local automation (smoke tests): expose captcha code in response header.
    $env:CAPTCHA_DEBUG_HEADER = "true"
    Write-Host "[WARN] CAPTCHA_DEBUG_HEADER is enabled. /api/checkCode will include X-EasyPan-CheckCode header."
}
else {
    $env:CAPTCHA_DEBUG_HEADER = "false"
}

# Local dev (Windows) should not use the Docker-style PROJECT_FOLDER (e.g. /data/easypan/),
# otherwise multipart uploads may be written relative to Tomcat's temp dir.
$projectFolder = ($RepoRoot -replace '\\\\', '/') + '/backend/file/'
$env:PROJECT_FOLDER = $projectFolder
Write-Host "[INFO] PROJECT_FOLDER is set to $projectFolder"

# Set Spring Boot properties
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/$env:POSTGRES_DB"
$env:SPRING_DATASOURCE_USERNAME = $env:POSTGRES_USER
$env:SPRING_DATASOURCE_PASSWORD = $env:POSTGRES_PASSWORD
$env:SPRING_DATA_REDIS_PASSWORD = $env:REDIS_PASSWORD
$env:MINIO_ENDPOINT = "http://localhost:9000"
$env:MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
$env:MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
$env:MINIO_BUCKET_NAME = $env:MINIO_BUCKET

# Start infrastructure containers
Write-Host "[1/3] Starting infrastructure containers (PostgreSQL, Redis, MinIO)..."
Push-Location $DockerDir
docker compose up -d postgres redis minio minio-init
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    Write-Host "[ERROR] Failed to start infrastructure containers." -ForegroundColor Red
    exit 1
}
Pop-Location

# Wait a bit for infrastructure to be ready
Write-Host "[2/3] Waiting for infrastructure to be ready..."
Start-Sleep -Seconds 5
Write-Host "[OK] Infrastructure should be ready." -ForegroundColor Green

# Create backend startup script
$backendScript = "$env:TEMP\easypan-backend.ps1"
$pgDb = $env:POSTGRES_DB
$pgUser = $env:POSTGRES_USER
$pgPass = $env:POSTGRES_PASSWORD
$redisPass = $env:REDIS_PASSWORD
$minioUser = $env:MINIO_ROOT_USER
$minioPass = $env:MINIO_ROOT_PASSWORD
$minioBucket = $env:MINIO_BUCKET
$backendContent = @"
chcp 65001 > `$null
`$env:SPRING_PROFILES_ACTIVE='local'
`$env:SERVER_PORT='7090'
`$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5433/$pgDb'
`$env:SPRING_DATASOURCE_USERNAME='$pgUser'
`$env:SPRING_DATASOURCE_PASSWORD='$pgPass'
`$env:SPRING_DATA_REDIS_PASSWORD='$redisPass'
`$env:MINIO_ENDPOINT='http://localhost:9000'
`$env:MINIO_ACCESS_KEY='$minioUser'
`$env:MINIO_SECRET_KEY='$minioPass'
`$env:MINIO_BUCKET_NAME='$minioBucket'
`$env:JWT_SECRET='$env:JWT_SECRET'
`$env:JASYPT_ENCRYPTOR_PASSWORD='$env:JASYPT_ENCRYPTOR_PASSWORD'
`$env:CAPTCHA_DEBUG_HEADER='$env:CAPTCHA_DEBUG_HEADER'
`$env:PROJECT_FOLDER='$projectFolder'
`$env:DEV_MODE='$env:DEV_MODE'
`$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8'
cd '$RepoRoot\backend'
mvn spring-boot:run
"@
Set-Content -Path $backendScript -Value $backendContent -Encoding UTF8

# Start backend
Write-Host "[3/5] Starting backend in a new window..."
Start-Process powershell -ArgumentList "-NoExit", "-File", $backendScript

# Wait for backend to be ready
Write-Host "[4/5] Waiting for backend to be ready..."
$backendReady = $false
$maxAttempts = 60
$attempt = 0
while (-not $backendReady -and $attempt -lt $maxAttempts) {
    $attempt++
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:7090/api/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
            Write-Host "[OK] Backend is ready!" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "  Waiting... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 2
    }
}

if (-not $backendReady) {
    Write-Host "[WARN] Backend did not become ready within timeout. Starting frontend anyway..." -ForegroundColor Yellow
}

# Start frontend
Write-Host "[5/5] Starting frontend in a new window..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$RepoRoot\frontend'; npm run dev"

# Open browser
if (-not $NoBrowser) {
    Start-Sleep -Seconds 2
    Start-Process "http://localhost:8080"
}

Write-Host "=" * 74
Write-Host "Started." -ForegroundColor Green
Write-Host "Frontend: http://localhost:8080"
Write-Host "Backend : http://localhost:7090/api"
Write-Host "MinIO   : http://localhost:9001"
Write-Host "=" * 74
