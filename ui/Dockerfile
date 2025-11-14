# ============================================
# 华创证券智能体底座 - 前端应用 Dockerfile
# ============================================

# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

# 复制package.json和锁文件
COPY package.json package-lock.json* pnpm-lock.yaml* ./

# 安装pnpm
RUN npm install -g pnpm

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 构建参数
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 构建生产版本
RUN pnpm run build

# ============================================
# 运行阶段
# ============================================
FROM nginx:1.25-alpine

# 安装curl用于健康检查
RUN apk add --no-cache curl

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 创建健康检查脚本
RUN echo '#!/bin/sh' > /health-check.sh && \
    echo 'curl -f http://localhost/health || exit 1' >> /health-check.sh && \
    chmod +x /health-check.sh

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD /health-check.sh

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]
