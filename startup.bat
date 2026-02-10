@echo off
setlocal

echo ==================================================================================
echo                  EasyCloudPan - 一键启动脚本 (Startup)
echo ==================================================================================
echo.

:: 检查配置文件是否存在
if not exist "backend\src\main\resources\application-local.properties" (
    echo [ERROR] 未找到配置文件，请先运行 setup.bat 进行初始化配置。
    pause
    exit /b 1
)

:: 设置本地工具路径
set "TOOLS_DIR=%~dp0tools"
set "PATH=%TOOLS_DIR%\ffmpeg\bin;%PATH%"

:: 启动本地 Redis (如果存在)
if exist "%TOOLS_DIR%\redis\redis-server.exe" (
    echo [INFO] 正在启动本地 Redis...
    start "EasyPan Redis" /MIN cmd /c "%TOOLS_DIR%\redis\redis-server.exe"
)

echo [1/2] 正在启动后端服务 (Spring Boot)...
echo 请勿关闭此窗口。后端启动可能需要几分钟...
echo.

start "EasyPan Backend" cmd /c "cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local"

:: 等待一段时间让后端先跑起来（可选）
timeout /t 5 >nul

echo [2/2] 正在启动前端服务 (Vue)...
echo.

start "EasyPan Frontend" cmd /c "cd frontend && npm run dev"

echo.
echo ==================================================================================
echo                  服务已启动！
echo                  后端: http://localhost:7090
echo                  前端: http://localhost:5173
echo.
echo                  正在自动打开浏览器...
echo ==================================================================================

timeout /t 3 >nul
explorer "http://localhost:5173"
