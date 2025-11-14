# ============================================
# 华创证券智能体底座 - AI工具服务 Dockerfile
# ============================================

FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TZ=Asia/Shanghai

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# 安装uv（推荐的Python包管理工具）
RUN pip install --no-cache-dir uv

# 复制依赖文件
COPY pyproject.toml .
COPY requirements.txt* .

# 安装Python依赖
RUN if [ -f pyproject.toml ]; then \
        uv pip install --system -e .; \
    elif [ -f requirements.txt ]; then \
        uv pip install --system -r requirements.txt; \
    else \
        echo "No dependency file found"; \
        exit 1; \
    fi

# 复制源代码
COPY . .

# 创建数据目录
RUN mkdir -p /app/data && \
    chmod 755 /app/data

# 初始化数据库（如果需要）
RUN python -m genie_tool.db.db_engine || true

# 创建非root用户
RUN useradd -m -u 1000 genie && \
    chown -R genie:genie /app

# 切换到非root用户
USER genie

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动服务
CMD ["python", "server.py"]
