#!/bin/bash
# ============================================
# 华创证券智能体底座 - 日志查看脚本 (Linux/Mac)
# ============================================

SERVICE_NAME=$1

echo "=========================================="
echo "  华创证券智能体底座 - 日志查看"
echo "=========================================="
echo ""

if [ -z "$SERVICE_NAME" ]; then
    echo "📋 可用服务列表："
    echo "  - mysql        MySQL数据库"
    echo "  - backend      后端服务"
    echo "  - frontend     前端应用"
    echo "  - genie-tool   AI工具服务"
    echo "  - genie-client MCP客户端"
    echo ""
    echo "使用方法:"
    echo "  $0 <服务名>     # 查看指定服务日志"
    echo "  $0             # 查看所有服务日志"
    echo ""

    read -p "查看所有服务日志？(y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "📊 显示所有服务日志（按 Ctrl+C 退出）..."
        echo ""
        docker-compose logs -f
    fi
else
    echo "📊 显示 $SERVICE_NAME 服务日志（按 Ctrl+C 退出）..."
    echo ""
    docker-compose logs -f $SERVICE_NAME
fi
