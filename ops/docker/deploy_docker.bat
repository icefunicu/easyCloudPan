@echo off
setlocal EnableExtensions

set "DOCKER_DIR=%~dp0"
for %%i in ("%DOCKER_DIR%..\..") do set "REPO_ROOT=%%~fi"
cd /d "%DOCKER_DIR%"

set "COMMON_SCRIPT=%REPO_ROOT%\ops\lib\common.bat"
set "BUILD_IMAGES=1"
set "REQUEST_HELP=0"

call :parse_args %*
if errorlevel 1 exit /b 1
if "%REQUEST_HELP%"=="1" (
    call :print_usage
    exit /b 0
)

if not exist "%COMMON_SCRIPT%" (
    echo [ERROR] Missing common script: %COMMON_SCRIPT%
    exit /b 1
)

call "%COMMON_SCRIPT%" print_header "EasyCloudPan One-Click Docker Deployment"
if errorlevel 1 exit /b 1

call "%COMMON_SCRIPT%" require_cmd docker "Docker Desktop"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_docker_compose
if errorlevel 1 exit /b 1

if "%BUILD_IMAGES%"=="1" (
    echo [1/2] Building and starting containers...
    docker compose up -d --build
) else (
    echo [1/2] Starting containers without rebuilding images...
    docker compose up -d
)

if errorlevel 1 (
    echo [ERROR] Deployment failed.
    exit /b 1
)

echo [2/2] Checking container status...
docker compose ps

echo.
echo Frontend: http://localhost:8080
echo Backend : http://localhost:7090/api
echo MinIO   : http://localhost:9001
echo.
echo To stop all containers, run: ops\docker\stop_docker.bat
exit /b 0

:parse_args
if "%~1"=="" exit /b 0

if /I "%~1"=="--no-build" (
    set "BUILD_IMAGES=0"
    shift
    goto :parse_args
)

if /I "%~1"=="--help" (
    set "REQUEST_HELP=1"
    exit /b 0
)

echo [ERROR] Unknown argument: %~1
call :print_usage
exit /b 1

:print_usage
echo Usage: ops\docker\deploy_docker.bat [--no-build]
echo.
echo   --no-build   Skip image build and run existing images
exit /b 0
