@echo off
setlocal EnableExtensions

set "DOCKER_DIR=%~dp0"
for %%i in ("%DOCKER_DIR%..\..") do set "REPO_ROOT=%%~fi"
cd /d "%DOCKER_DIR%"

set "COMMON_SCRIPT=%REPO_ROOT%\ops\lib\common.bat"
set "REMOVE_VOLUMES=0"
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

call "%COMMON_SCRIPT%" require_cmd docker "Docker Desktop"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_docker_compose
if errorlevel 1 exit /b 1

if "%REMOVE_VOLUMES%"=="1" (
    echo [INFO] Stopping containers and removing volumes...
    docker compose down -v
) else (
    echo [INFO] Stopping containers...
    docker compose down
)

if errorlevel 1 (
    echo [ERROR] Failed to stop containers.
    exit /b 1
)

echo [OK] All EasyCloudPan containers are stopped.
if "%REMOVE_VOLUMES%"=="0" (
    echo To also remove volumes, run: ops\docker\stop_docker.bat --volumes
)
exit /b 0

:parse_args
if "%~1"=="" exit /b 0

if /I "%~1"=="--volumes" (
    set "REMOVE_VOLUMES=1"
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
echo Usage: ops\docker\stop_docker.bat [--volumes]
echo.
echo   --volumes   Stop containers and remove named volumes ^(destructive^)
exit /b 0
