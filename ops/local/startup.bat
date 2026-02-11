@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fi"
set "DOCKER_DIR=%REPO_ROOT%\ops\docker"
cd /d "%REPO_ROOT%"

set "COMMON_SCRIPT=%REPO_ROOT%\ops\lib\common.bat"
set "ENV_FILE=%REPO_ROOT%\ops\docker\.env"
set "OPEN_BROWSER=1"
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

if not exist "%ENV_FILE%" (
    echo [ERROR] %ENV_FILE% not found.
    echo Run ops\local\setup.bat first to create it.
    exit /b 1
)

echo [INFO] Loading environment variables from %ENV_FILE%...
for /f "usebackq eol=# tokens=1* delims==" %%A in ("%ENV_FILE%") do (
    set "%%A=%%B"
)

call "%COMMON_SCRIPT%" print_header "EasyCloudPan One-Click Local Start"
if errorlevel 1 exit /b 1

call :check_prerequisites
if errorlevel 1 exit /b 1

echo [1/3] Starting infrastructure containers (PostgreSQL, Redis, MinIO)...
pushd "%DOCKER_DIR%"
docker compose up -d postgres redis minio minio-init
set "COMPOSE_EXIT=%ERRORLEVEL%"
popd
if not "%COMPOSE_EXIT%"=="0" (
    echo [ERROR] Failed to start infrastructure containers.
    exit /b 1
)

echo [2/3] Starting backend in a new window...
start "EasyCloudPan Backend" cmd /k "cd /d \"%REPO_ROOT%\backend\" && mvn spring-boot:run -Dspring-boot.run.profiles=local"

echo [3/3] Starting frontend in a new window...
start "EasyCloudPan Frontend" cmd /k "cd /d \"%REPO_ROOT%\frontend\" && npm run dev"

if "%OPEN_BROWSER%"=="1" (
    timeout /t 3 >nul
    start "" "http://localhost:8080"
)

echo ===========================================================================
echo Started.
echo Frontend: http://localhost:8080
echo Backend : http://localhost:7090/api
echo MinIO   : http://localhost:9001
echo ===========================================================================
exit /b 0

:check_prerequisites
call "%COMMON_SCRIPT%" require_cmd java "Java (JDK 21+)"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_cmd javac "JDK compiler"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_cmd mvn "Maven"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_cmd node "Node.js (20+)"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_cmd npm "npm"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_cmd docker "Docker Desktop"
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_docker_compose
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_java_major 21
if errorlevel 1 exit /b 1
call "%COMMON_SCRIPT%" require_node_major 20
if errorlevel 1 exit /b 1
exit /b 0

:parse_args
if "%~1"=="" exit /b 0

if /I "%~1"=="--no-browser" (
    set "OPEN_BROWSER=0"
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
echo Usage: ops\local\startup.bat [--no-browser]
echo.
echo   --no-browser   Do not open browser automatically
exit /b 0
