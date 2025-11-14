@echo off
REM ============================================
REM 华创证券智能体底座 - 启动脚本 (Windows)
REM ============================================

chcp 65001 >nul

echo ==========================================
echo   华创证券智能体底座 - 启动脚本
echo ==========================================
echo.

REM 检查.env文件
if not exist .env (
    echo ❌ 错误: .env 文件不存在
    echo.
    echo 请执行以下步骤：
    echo   1. 复制 .env.example 为 .env
    echo   2. 编辑 .env 文件，填写配置信息
    echo.
    echo 命令: copy .env.example .env
    pause
    exit /b 1
)

REM 检查Docker
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: Docker 未安装或未启动
    echo.
    echo 请访问 https://docs.docker.com/get-docker/ 安装 Docker Desktop
    pause
    exit /b 1
)

REM 检查Docker Compose
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: Docker Compose 未安装
    echo.
    echo Docker Desktop已包含Docker Compose，请检查Docker是否正常安装
    pause
    exit /b 1
)

echo ✅ 环境检查通过
echo.

REM 显示配置信息
echo 📋 当前配置：
echo   - 数据库: MySQL 8.0
echo   - 后端端口: 8080
echo   - 前端端口: 80
echo   - AI工具端口: 8000
echo   - MCP客户端端口: 8188
echo.

REM 询问是否继续
set /p continue="是否继续启动服务？(y/n) "
if /i not "%continue%"=="y" (
    echo ❌ 已取消启动
    pause
    exit /b 0
)

echo.
echo 🚀 正在启动所有服务...
echo.

REM 启动服务
docker-compose up -d --build

echo.
echo ⏳ 等待服务启动...
timeout /t 15 /nobreak >nul

echo.
echo 📊 服务状态：
echo.
docker-compose ps

echo.
echo ==========================================
echo   ✅ 服务启动完成！
echo ==========================================
echo.
echo 访问地址：
echo   - 前端应用:    http://localhost
echo   - 后端API:     http://localhost:8080
echo   - API文档:     http://localhost:8080/swagger-ui.html
echo   - AI工具文档:  http://localhost:8000/docs
echo   - MCP客户端:   http://localhost:8188/docs
echo.
echo 默认管理员账号：
echo   用户名: admin
echo   密码:   admin123
echo   ⚠️  请在生产环境中务必修改默认密码！
echo.
echo 常用命令：
echo   查看日志:   scripts\logs.bat [服务名]
echo   停止服务:   scripts\stop.bat
echo   重启服务:   scripts\stop.bat ^&^& scripts\start.bat
echo.
echo ==========================================
pause
