@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fi"
set "DOCKER_DIR=%REPO_ROOT%\ops\docker"
cd /d "%REPO_ROOT%"

set "ENV_FILE=%REPO_ROOT%\ops\docker\.env"
set "OPEN_BROWSER=1"
set "REQUEST_HELP=0"

call :parse_args %*
if errorlevel 1 exit /b 1
if "%REQUEST_HELP%"=="1" (
    call :print_usage
    exit /b 0
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

REM Map Docker environment variables to Spring Boot properties
set "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/%POSTGRES_DB%"
set "SPRING_DATASOURCE_USERNAME=%POSTGRES_USER%"
set "SPRING_DATASOURCE_PASSWORD=%POSTGRES_PASSWORD%"
set "SPRING_DATA_REDIS_PASSWORD=%REDIS_PASSWORD%"
set "MINIO_ENDPOINT=http://localhost:9000"
set "MINIO_ACCESS_KEY=%MINIO_ROOT_USER%"
set "MINIO_SECRET_KEY=%MINIO_ROOT_PASSWORD%"
set "MINIO_BUCKET_NAME=%MINIO_BUCKET%"

echo ===========================================================================
echo EasyCloudPan One-Click Local Start
echo ===========================================================================

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
start "EasyCloudPan Backend" cmd /k "chcp 65001 >nul && cd /d "%REPO_ROOT%\backend" && set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 && mvn spring-boot:run -Dspring-boot.run.profiles=local"

echo [3/3] Starting frontend in a new window...
start "EasyCloudPan Frontend" cmd /k "cd /d "%REPO_ROOT%\frontend" && npm run dev"

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
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: Java ^(JDK 21+^)
    exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: JDK compiler
    exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: Maven
    exit /b 1
)
where node >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: Node.js ^(20+^)
    exit /b 1
)
where npm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: npm
    exit /b 1
)
where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: Docker Desktop
    exit /b 1
)
docker compose version >nul 2>nul
if errorlevel 1 (
    echo [ERROR] docker compose is unavailable. Please enable Docker Compose V2.
    exit /b 1
)

set "JAVAC_VERSION="
set "JAVA_MAJOR="
for /f "tokens=2 delims= " %%v in ('javac -version 2^>^&1') do set "JAVAC_VERSION=%%v"
for /f "tokens=1 delims=." %%v in ("%JAVAC_VERSION%") do set "JAVA_MAJOR=%%v"
if "%JAVA_MAJOR%"=="" (
    echo [ERROR] Unable to detect Java version.
    exit /b 1
)
if %JAVA_MAJOR% LSS 21 (
    echo [ERROR] JDK 21+ is required. Current javac version: %JAVAC_VERSION%
    exit /b 1
)

set "NODE_MAJOR="
for /f "delims=." %%v in ('node -v') do set "NODE_MAJOR=%%v"
set "NODE_MAJOR=%NODE_MAJOR:~1%"
if "%NODE_MAJOR%"=="" (
    echo [ERROR] Unable to detect Node.js version.
    exit /b 1
)
if %NODE_MAJOR% LSS 20 (
    echo [ERROR] Node.js 20+ is required. Current major version: %NODE_MAJOR%
    exit /b 1
)
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
