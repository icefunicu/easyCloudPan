@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%i in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fi"
cd /d "%REPO_ROOT%"

set "COMMON_SCRIPT=%REPO_ROOT%\ops\lib\common.bat"
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

if not exist "%COMMON_SCRIPT%" (
    echo [ERROR] Missing common script: %COMMON_SCRIPT%
    exit /b 1
)

call "%COMMON_SCRIPT%" print_header "EasyCloudPan Local Setup"
if errorlevel 1 exit /b 1

call :check_prerequisites
if errorlevel 1 exit /b 1

echo [1/4] Toolchain check passed.

if "%SKIP_NPM%"=="1" (
    echo [2/4] Skipping frontend dependency installation ^(--skip-npm^).
) else (
    echo [2/4] Installing frontend dependencies...
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

echo [3/4] Preparing local runtime directories...
for %%d in (
    "%REPO_ROOT%\backend\file"
    "%REPO_ROOT%\backend\file\temp"
    "%REPO_ROOT%\backend\file\file"
    "%REPO_ROOT%\backend\file\avatar"
) do (
    call "%COMMON_SCRIPT%" ensure_dir %%~d
    if errorlevel 1 exit /b 1
)
echo [OK] Local directories prepared.

echo [4/4] Generating backend local config...
echo [4/4] Setting up environment configuration...
if not exist "%REPO_ROOT%\ops\docker\.env" (
    echo [INFO] ops\docker\.env not found. Creating from example...
    copy "%REPO_ROOT%\ops\docker\.env.example" "%REPO_ROOT%\ops\docker\.env"
    echo [OK] Created ops\docker\.env
) else (
    echo [INFO] ops\docker\.env already exists. Skipping creation.
)

REM Remove legacy application-local.properties if it exists to avoid confusion
if exist "%CONFIG_FILE%" (
    echo [WARN] Found legacy %CONFIG_FILE%. 
    echo        Renaming to application-local.properties.bak to enforce .env usage.
    move /y "%CONFIG_FILE%" "%CONFIG_FILE%.bak"
)

:done
echo ===========================================================================
echo Setup finished.
echo Next step:
echo   1^)^ ops\local\startup.bat           ^(local dev one-click start^)
echo   2^)^ ops\docker\deploy_docker.bat    ^(full docker deployment^)
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
