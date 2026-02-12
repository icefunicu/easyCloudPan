# EasyCloudPan 本地一键启动脚本
# 用法: .\ops\local\startup.ps1 [-NoBrowser]

param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$DockerDir = "$RepoRoot\ops\docker"
$EnvFile = "$DockerDir\.env"

function Print-Header {
    param([string]$Title)
    Write-Host "=" * 74
    Write-Host $Title
    Write-Host "=" * 74
}

# 检查配置文件
if (-not (Test-Path $EnvFile)) {
    Write-Host "[ERROR] $EnvFile not found." -ForegroundColor Red
    Write-Host "Run .\ops\local\setup.ps1 first to create it."
    exit 1
}

Print-Header "EasyCloudPan One-Click Local Start"

# 加载环境变量
Write-Host "[INFO] Loading environment variables from $EnvFile..."
Get-Content $EnvFile | Where-Object { $_ -notmatch '^#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_.Split('=', 2)
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
}

# 设置 Spring Boot 属性
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/$env:POSTGRES_DB"
$env:SPRING_DATASOURCE_USERNAME = $env:POSTGRES_USER
$env:SPRING_DATASOURCE_PASSWORD = $env:POSTGRES_PASSWORD
$env:SPRING_DATA_REDIS_PASSWORD = $env:REDIS_PASSWORD
$env:MINIO_ENDPOINT = "http://localhost:9000"
$env:MINIO_ACCESS_KEY = $env:MINIO_ROOT_USER
$env:MINIO_SECRET_KEY = $env:MINIO_ROOT_PASSWORD
$env:MINIO_BUCKET_NAME = $env:MINIO_BUCKET

# 启动基础设施容器
Write-Host "[1/3] Starting infrastructure containers (PostgreSQL, Redis, MinIO)..."
Push-Location $DockerDir
docker compose up -d postgres redis minio minio-init
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    Write-Host "[ERROR] Failed to start infrastructure containers." -ForegroundColor Red
    exit 1
}
Pop-Location

# 启动后端
Write-Host "[2/3] Starting backend in a new window..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$RepoRoot\backend'; mvn spring-boot:run -Dspring-boot.run.profiles=local"

# 启动前端
Write-Host "[3/3] Starting frontend in a new window..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$RepoRoot\frontend'; npm run dev"

# 打开浏览器
if (-not $NoBrowser) {
    Start-Sleep -Seconds 3
    Start-Process "http://localhost:8080"
}

Write-Host "=" * 74
Write-Host "Started." -ForegroundColor Green
Write-Host "Frontend: http://localhost:8080"
Write-Host "Backend : http://localhost:7090/api"
Write-Host "MinIO   : http://localhost:9001"
Write-Host "=" * 74
