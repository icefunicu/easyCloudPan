@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fi"
cd /d "%REPO_ROOT%"

set "CONFIG_FILE=%REPO_ROOT%\backend\src\main\resources\application-local.properties"
set "FORCE=0"
set "SKIP_NPM=0"
set "REQUEST_HELP=0"

call :parse_args %*
if errorlevel 1 exit /b 1
if "%REQUEST_HELP%"=="1" (
    call :print_usage
    exit /b 0
)

echo ===========================================================================
echo EasyCloudPan Local Setup
echo ===========================================================================

call :check_prerequisites
if errorlevel 1 exit /b 1

echo [1/5] Toolchain check passed.

if "%SKIP_NPM%"=="1" (
    echo [2/5] Skipping frontend dependency installation ^(--skip-npm^).
) else (
    echo [2/5] Installing frontend dependencies...
    pushd "%REPO_ROOT%\frontend"
    call npm install
    if errorlevel 1 (
        popd
        echo [ERROR] npm install failed.
        exit /b 1
    )
    popd
    echo [OK] Frontend dependencies are ready.
)

echo [3/5] Building backend dependencies...
pushd "%REPO_ROOT%\backend"
call mvn clean install -DskipTests
if errorlevel 1 (
    popd
    echo [ERROR] Backend build failed.
    exit /b 1
)
popd
echo [OK] Backend built successfully.

echo [4/5] Preparing local runtime directories...
for %%d in (
    "%REPO_ROOT%\backend\file"
    "%REPO_ROOT%\backend\file\temp"
    "%REPO_ROOT%\backend\file\file"
    "%REPO_ROOT%\backend\file\avatar"
) do (
    if not exist "%%~d" mkdir "%%~d"
)
echo [OK] Local directories prepared.

echo [5/5] Setting up environment configuration...
if not exist "%REPO_ROOT%\ops\docker\.env" (
    echo [INFO] ops\docker\.env not found. Creating from example...
    copy "%REPO_ROOT%\ops\docker\.env.example" "%REPO_ROOT%\ops\docker\.env"
    echo [OK] Created ops\docker\.env
) else (
    echo [INFO] ops\docker\.env already exists. Skipping creation.
)

if exist "%CONFIG_FILE%" (
    echo [WARN] Found legacy %CONFIG_FILE%. 
    echo        Renaming to application-local.properties.bak to enforce .env usage.
    move /y "%CONFIG_FILE%" "%CONFIG_FILE%.bak"
)

echo ===========================================================================
echo Setup finished.
echo Next step:
echo   1^) ops\local\startup.bat           ^(local dev one-click start^)
echo   2^) ops\docker\deploy_docker.bat    ^(full docker deployment^)
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

if /I "%~1"=="--force" (
    set "FORCE=1"
    shift
    goto :parse_args
)

if /I "%~1"=="--skip-npm" (
    set "SKIP_NPM=1"
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
echo Usage: ops\local\setup.bat [--force] [--skip-npm]
echo.
echo   --force      Overwrite existing application-local.properties
echo   --skip-npm   Skip frontend dependency installation
exit /b 0
