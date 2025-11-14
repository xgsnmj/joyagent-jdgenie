# 华创证券智能体底座 - Docker 部署文档

## 目录

- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [服务管理](#服务管理)
- [常用命令](#常用命令)
- [故障排除](#故障排除)
- [生产部署](#生产部署)
- [监控和日志](#监控和日志)
- [备份和恢复](#备份和恢复)

## 前置要求

### 系统要求

- **操作系统**:
  - Linux (Ubuntu 20.04+, CentOS 7+)
  - macOS (10.15+)
  - Windows 10/11 (WSL2推荐)

- **硬件要求**:
  - CPU: 4核或以上
  - 内存: 8GB或以上（推荐16GB）
  - 硬盘: 至少20GB可用空间
  - 网络: 稳定的互联网连接

### 软件要求

#### 1. Docker

**版本要求**: Docker 20.10+

**安装方法**:

<details>
<summary>Linux (Ubuntu/Debian)</summary>

```bash
# 卸载旧版本
sudo apt-get remove docker docker-engine docker.io containerd runc

# 安装依赖
sudo apt-get update
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# 添加Docker官方GPG密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 设置仓库
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
```
</details>

<details>
<summary>macOS</summary>

1. 下载 [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)
2. 双击安装包进行安装
3. 启动Docker Desktop
4. 验证安装: `docker --version`
</details>

<details>
<summary>Windows</summary>

1. 启用WSL2（推荐）
2. 下载 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
3. 双击安装包进行安装
4. 启动Docker Desktop
5. 验证安装: `docker --version`
</details>

#### 2. Docker Compose

**版本要求**: Docker Compose 2.0+

Docker Desktop已包含Docker Compose。如果使用Linux，请单独安装：

```bash
# Linux安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

## 快速开始

### 1. 克隆项目

```bash
cd /opt
git clone [内部Git仓库地址] joyagent-jdgenie
cd joyagent-jdgenie
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑.env文件
vim .env
```

**必填配置项**:
```env
# 数据库密码（请设置强密码）
DB_ROOT_PASSWORD=YourRootPassword123!
DB_PASSWORD=YourDbPassword123!

# LLM API配置
LLM_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
LLM_BASE_URL=https://api.deepseek.com/v1

# JWT密钥（至少32字符）
JWT_SECRET=your-jwt-secret-key-at-least-32-characters-long-change-this
```

### 3. 启动服务

**Linux/Mac**:
```bash
chmod +x scripts/*.sh
./scripts/start.sh
```

**Windows**:
```cmd
scripts\start.bat
```

### 4. 访问应用

启动完成后，可访问以下地址：

- **前端应用**: http://localhost
- **后端API文档**: http://localhost:8080/swagger-ui.html
- **AI工具文档**: http://localhost:8000/docs
- **MCP客户端文档**: http://localhost:8188/docs

**默认管理员账号**:
- 用户名: `admin`
- 密码: `admin123`

⚠️ **重要**: 首次登录后请立即修改默认密码！

## 环境配置

### 完整配置说明

#### 数据库配置

```env
DB_ROOT_PASSWORD=root用户密码
DB_NAME=genie_db              # 数据库名称
DB_USER=genie                 # 应用用户名
DB_PASSWORD=应用用户密码
```

#### LLM配置

**DeepSeek示例**:
```env
LLM_API_KEY=sk-xxxxx
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-chat
```

**通义千问示例**:
```env
LLM_API_KEY=sk-xxxxx
LLM_BASE_URL=https://dashscope.aliyuncs.com/api/v1
LLM_MODEL=qwen-turbo
```

#### 端口配置

```env
BACKEND_PORT=8080     # 后端服务端口
FRONTEND_PORT=80      # 前端应用端口
TOOL_PORT=8000        # AI工具服务端口
CLIENT_PORT=8188      # MCP客户端端口
```

如需修改端口，确保：
1. 端口未被占用
2. 防火墙允许访问
3. 修改后重新启动服务

#### 外部智能体平台配置

**Coze平台**:
```env
COZE_API_KEY=your_coze_api_key
COZE_BASE_URL=https://api.coze.cn/v3
```

**通义千问平台**:
```env
TONGYI_API_KEY=your_tongyi_api_key
```

**融汇平台**:
```env
RONGHUI_API_KEY=your_ronghui_api_key
RONGHUI_BASE_URL=https://api.ronghui.ai
```

## 服务管理

### 启动服务

```bash
# Linux/Mac
./scripts/start.sh

# Windows
scripts\start.bat

# 或直接使用docker-compose
docker-compose up -d
```

### 停止服务

```bash
# Linux/Mac
./scripts/stop.sh

# Windows
scripts\stop.bat

# 或直接使用docker-compose
docker-compose down
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart backend
docker-compose restart frontend
docker-compose restart genie-tool
docker-compose restart genie-client
docker-compose restart mysql
```

### 查看服务状态

```bash
# 查看所有服务状态
docker-compose ps

# 查看服务资源使用
docker stats

# 查看特定服务状态
docker-compose ps backend
```

### 查看日志

```bash
# Linux/Mac
./scripts/logs.sh              # 所有服务
./scripts/logs.sh backend      # 指定服务

# Windows
scripts\logs.bat
scripts\logs.bat backend

# 或直接使用docker-compose
docker-compose logs -f                    # 所有服务
docker-compose logs -f backend            # 指定服务
docker-compose logs -f --tail=100 backend # 最后100行
```

## 常用命令

### 进入容器

```bash
# 进入后端容器
docker-compose exec backend sh

# 进入MySQL容器
docker-compose exec mysql bash

# 进入前端容器
docker-compose exec frontend sh
```

### 执行MySQL命令

```bash
# 进入MySQL命令行
docker-compose exec mysql mysql -uroot -p

# 备份数据库
docker-compose exec mysql mysqldump -uroot -p genie_db > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -uroot -p genie_db < backup.sql
```

### 清理和重置

```bash
# 停止并删除所有容器
docker-compose down

# 停止并删除所有容器和数据卷（⚠️ 会删除数据）
docker-compose down -v

# 删除所有镜像
docker-compose down --rmi all

# 重新构建所有镜像
docker-compose build --no-cache

# 完全重置（删除所有容器、镜像、数据卷）
docker-compose down -v --rmi all
docker-compose up -d --build
```

### 更新服务

```bash
# 拉取最新代码
git pull

# 重新构建并启动服务
docker-compose up -d --build

# 仅重新构建特定服务
docker-compose up -d --build backend
```

## 故障排除

### 常见问题

#### 1. 端口占用

**错误信息**: `Error: bind: address already in use`

**解决方法**:
```bash
# 查看端口占用
netstat -tulpn | grep <端口号>
lsof -i :<端口号>

# 修改.env文件中的端口配置
# 或停止占用端口的程序
```

#### 2. MySQL连接失败

**错误信息**: `Failed to connect to MySQL`

**解决方法**:
```bash
# 检查MySQL容器状态
docker-compose ps mysql

# 查看MySQL日志
docker-compose logs mysql

# 检查数据库密码是否正确
cat .env | grep DB_PASSWORD

# 重启MySQL
docker-compose restart mysql
```

#### 3. 后端服务启动失败

**错误信息**: `Application failed to start`

**解决方法**:
```bash
# 查看后端日志
docker-compose logs backend

# 检查环境变量配置
cat .env

# 检查数据库是否已启动
docker-compose ps mysql

# 重新构建后端镜像
docker-compose up -d --build backend
```

#### 4. 前端访问404

**解决方法**:
```bash
# 检查Nginx配置
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf

# 检查构建产物
docker-compose exec frontend ls -la /usr/share/nginx/html

# 重新构建前端
docker-compose up -d --build frontend
```

#### 5. 磁盘空间不足

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的数据卷
docker volume prune

# 清理全部未使用资源
docker system prune -a --volumes
```

#### 6. 内存不足

**修改Docker内存限制**:

1. 打开Docker Desktop设置
2. 进入Resources → Advanced
3. 增加Memory限制（推荐8GB+）
4. 重启Docker

### 日志分析

#### 查看实时日志
```bash
docker-compose logs -f --tail=50 backend
```

#### 过滤错误日志
```bash
docker-compose logs backend | grep ERROR
docker-compose logs backend | grep -i exception
```

#### 导出日志
```bash
docker-compose logs > logs/all-services.log
docker-compose logs backend > logs/backend.log
```

## 生产部署

### 安全加固

#### 1. 修改默认密码

首次部署后立即修改：
- 管理员账号密码
- 数据库root密码
- JWT密钥

#### 2. 配置HTTPS

创建 `nginx-ssl.conf`:

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # SSL配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 其他配置同nginx.conf
    ...
}

# HTTP重定向到HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

修改 `docker-compose.yml`:
```yaml
frontend:
  volumes:
    - ./nginx-ssl.conf:/etc/nginx/conf.d/default.conf
    - ./ssl:/etc/nginx/ssl:ro
  ports:
    - "80:80"
    - "443:443"
```

#### 3. 限制网络访问

```yaml
# docker-compose.yml
services:
  mysql:
    # 移除ports配置，仅容器内部访问
    # ports:
    #   - "3306:3306"
```

#### 4. 使用密钥管理

推荐使用Docker Secrets或外部密钥管理服务（如HashiCorp Vault）。

### 性能优化

#### 1. 调整JVM参数

修改 `genie-backend/Dockerfile`:
```dockerfile
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### 2. 配置MySQL优化

创建 `mysql/my.cnf`:
```ini
[mysqld]
max_connections = 500
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
```

修改 `docker-compose.yml`:
```yaml
mysql:
  volumes:
    - ./mysql/my.cnf:/etc/mysql/conf.d/my.cnf
```

#### 3. 启用Nginx缓存

修改 `ui/nginx.conf`，添加缓存配置。

### 高可用部署

#### 使用Docker Swarm

```bash
# 初始化Swarm
docker swarm init

# 部署Stack
docker stack deploy -c docker-compose.yml genie

# 扩展服务
docker service scale genie_backend=3
```

#### 使用Kubernetes

参考项目 `k8s/` 目录下的配置文件（需另行创建）。

## 监控和日志

### 集成Prometheus + Grafana

创建 `docker-compose.monitoring.yml`:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:latest
    volumes:
      - grafana_data:/var/lib/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

volumes:
  prometheus_data:
  grafana_data:
```

启动监控服务：
```bash
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

### 集成ELK日志系统

创建 `docker-compose.elk.yml`（配置Elasticsearch + Logstash + Kibana）。

## 备份和恢复

### 数据备份

#### 1. 备份MySQL数据

```bash
# 备份脚本
#!/bin/bash
BACKUP_DIR=/path/to/backup
DATE=$(date +%Y%m%d_%H%M%S)

docker-compose exec -T mysql mysqldump -uroot -p${DB_ROOT_PASSWORD} \
  --all-databases --single-transaction --quick --lock-tables=false \
  > $BACKUP_DIR/mysql_backup_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/mysql_backup_$DATE.sql
```

#### 2. 备份数据卷

```bash
# 备份MySQL数据卷
docker run --rm \
  -v joyagent-jdgenie_mysql_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/mysql_data_backup.tar.gz /data

# 备份AI工具数据卷
docker run --rm \
  -v joyagent-jdgenie_tool_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/tool_data_backup.tar.gz /data
```

#### 3. 定时备份（Cron）

```bash
# 编辑crontab
crontab -e

# 添加定时任务（每天凌晨2点备份）
0 2 * * * /path/to/backup-script.sh >> /var/log/backup.log 2>&1
```

### 数据恢复

#### 1. 恢复MySQL数据

```bash
# 解压备份文件
gunzip mysql_backup_YYYYMMDD_HHMMSS.sql.gz

# 恢复数据
docker-compose exec -T mysql mysql -uroot -p${DB_ROOT_PASSWORD} \
  < mysql_backup_YYYYMMDD_HHMMSS.sql
```

#### 2. 恢复数据卷

```bash
# 停止服务
docker-compose down

# 恢复数据卷
docker run --rm \
  -v joyagent-jdgenie_mysql_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar xzf /backup/mysql_data_backup.tar.gz -C /

# 重启服务
docker-compose up -d
```

## 技术支持

**内部技术支持**:
- 联系人: 华创证券科技研发中心
- 技术文档: 见项目Wiki
- Issue追踪: 内部JIRA系统

**相关文档**:
- [主README](README.md)
- [后端服务文档](genie-backend/README.md)
- [前端应用文档](ui/README.md)
- [AI工具服务文档](genie-tool/README.md)
- [MCP客户端文档](genie-client/README.md)
- [数据库设计文档](database_schema.md)

---

**最后更新**: 2025-01-14
