@echo off
setlocal EnableExtensions

set "ACTION=%~1"
if "%ACTION%"=="" (
    echo [ERROR] Missing common action.
    exit /b 1
)

shift

if /I "%ACTION%"=="print_header" goto :print_header
if /I "%ACTION%"=="require_cmd" goto :require_cmd
if /I "%ACTION%"=="require_docker_compose" goto :require_docker_compose
if /I "%ACTION%"=="require_java_major" goto :require_java_major
if /I "%ACTION%"=="require_node_major" goto :require_node_major
if /I "%ACTION%"=="ensure_dir" goto :ensure_dir

echo [ERROR] Unknown common action: %ACTION%
exit /b 1

:print_header
echo ===========================================================================
echo %~1
echo ===========================================================================
exit /b 0

:require_cmd
set "CMD_NAME=%~1"
set "DISPLAY_NAME=%~2"
if "%DISPLAY_NAME%"=="" set "DISPLAY_NAME=%CMD_NAME%"

where "%CMD_NAME%" >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Missing dependency: %DISPLAY_NAME%
    exit /b 1
)
exit /b 0

:require_docker_compose
docker compose version >nul 2>nul
if errorlevel 1 (
    echo [ERROR] docker compose is unavailable. Please enable Docker Compose V2.
    exit /b 1
)
exit /b 0

:require_java_major
set "MIN_JAVA=%~1"
if "%MIN_JAVA%"=="" set "MIN_JAVA=21"

set "JAVAC_VERSION="
set "JAVA_MAJOR="
for /f "tokens=2 delims= " %%v in ('javac -version 2^>^&1') do set "JAVAC_VERSION=%%v"
for /f "tokens=1 delims=." %%v in ("%JAVAC_VERSION%") do set "JAVA_MAJOR=%%v"
if "%JAVA_MAJOR%"=="" (
    echo [ERROR] Unable to detect Java version.
    exit /b 1
)
if %JAVA_MAJOR% LSS %MIN_JAVA% (
    echo [ERROR] JDK %MIN_JAVA%+ is required. Current javac version: %JAVAC_VERSION%
    exit /b 1
)
exit /b 0

:require_node_major
set "MIN_NODE=%~1"
if "%MIN_NODE%"=="" set "MIN_NODE=20"

set "NODE_MAJOR="
for /f %%v in ('node -p "process.versions.node.split(\".\")[0]"') do set "NODE_MAJOR=%%v"
if "%NODE_MAJOR%"=="" (
    echo [ERROR] Unable to detect Node.js version.
    exit /b 1
)
if %NODE_MAJOR% LSS %MIN_NODE% (
    echo [ERROR] Node.js %MIN_NODE%+ is required. Current major version: %NODE_MAJOR%
    exit /b 1
)
exit /b 0

:ensure_dir
set "TARGET_DIR=%~1"
if "%TARGET_DIR%"=="" (
    echo [ERROR] Missing directory path.
    exit /b 1
)
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
exit /b 0
