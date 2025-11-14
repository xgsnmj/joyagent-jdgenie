@echo off
REM ============================================
REM 华创证券智能体底座 - 停止脚本 (Windows)
REM ============================================

chcp 65001 >nul

echo ==========================================
echo   华创证券智能体底座 - 停止服务
echo ==========================================
echo.

REM 询问是否停止
set /p continue="确认要停止所有服务吗？(y/n) "
if /i not "%continue%"=="y" (
    echo ❌ 已取消停止
    pause
    exit /b 0
)

echo.
echo 🛑 正在停止所有服务...
echo.

REM 停止服务
docker-compose down

echo.
echo ✅ 所有服务已停止
echo.
echo 如需删除数据卷，请执行:
echo   docker-compose down -v
echo.
pause
