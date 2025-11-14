#!/bin/bash
# ============================================
# 华创证券智能体底座 - 启动脚本 (Linux/Mac)
# ============================================

set -e

echo "=========================================="
echo "  华创证券智能体底座 - 启动脚本"
echo "=========================================="
echo ""

# 检查.env文件
if [ ! -f .env ]; then
    echo "❌ 错误: .env 文件不存在"
    echo ""
    echo "请执行以下步骤："
    echo "  1. 复制 .env.example 为 .env"
    echo "  2. 编辑 .env 文件，填写配置信息"
    echo ""
    echo "命令: cp .env.example .env"
    exit 1
fi

# 检查Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: Docker 未安装"
    echo ""
    echo "请访问 https://docs.docker.com/get-docker/ 安装 Docker"
    exit 1
fi

# 检查Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ 错误: Docker Compose 未安装"
    echo ""
    echo "请访问 https://docs.docker.com/compose/install/ 安装 Docker Compose"
    exit 1
fi

echo "✅ 环境检查通过"
echo ""

# 显示配置信息
echo "📋 当前配置："
echo "  - 数据库: MySQL 8.0"
echo "  - 后端端口: ${BACKEND_PORT:-8080}"
echo "  - 前端端口: ${FRONTEND_PORT:-80}"
echo "  - AI工具端口: ${TOOL_PORT:-8000}"
echo "  - MCP客户端端口: ${CLIENT_PORT:-8188}"
echo ""

# 询问是否继续
read -p "是否继续启动服务？(y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 已取消启动"
    exit 0
fi

echo "🚀 正在启动所有服务..."
echo ""

# 启动服务
docker-compose up -d --build

echo ""
echo "⏳ 等待服务启动..."
sleep 15

echo ""
echo "📊 服务状态："
echo ""
docker-compose ps

echo ""
echo "=========================================="
echo "  ✅ 服务启动完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  - 前端应用:    http://localhost:${FRONTEND_PORT:-80}"
echo "  - 后端API:     http://localhost:${BACKEND_PORT:-8080}"
echo "  - API文档:     http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html"
echo "  - AI工具文档:  http://localhost:${TOOL_PORT:-8000}/docs"
echo "  - MCP客户端:   http://localhost:${CLIENT_PORT:-8188}/docs"
echo ""
echo "默认管理员账号："
echo "  用户名: admin"
echo "  密码:   admin123"
echo "  ⚠️  请在生产环境中务必修改默认密码！"
echo ""
echo "常用命令："
echo "  查看日志:   ./scripts/logs.sh [服务名]"
echo "  停止服务:   ./scripts/stop.sh"
echo "  重启服务:   ./scripts/stop.sh && ./scripts/start.sh"
echo ""
echo "=========================================="
