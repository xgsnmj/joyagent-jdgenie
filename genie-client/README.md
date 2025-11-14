# 华创证券智能体底座 - MCP客户端服务

## 项目简介

华创证券智能体底座MCP客户端服务是基于 Python 和 FastAPI 构建的模型上下文协议（Model Context Protocol，MCP）客户端实现，为智能体提供标准化的外部工具集成能力。本服务支持连接和调用遵循MCP协议的第三方工具服务器，实现智能体与外部能力的无缝对接。

**开发单位**: 华创证券科技研发中心
**项目定位**: 企业内部智能体基础设施平台工具集成层

**协议版本**: MCP 1.9.4

## 什么是MCP？

Model Context Protocol (MCP) 是一个开放的标准化协议，旨在让大语言模型（LLM）能够安全、统一地访问外部工具和数据源。MCP提供了一套标准的接口规范，使得智能体可以通过统一的方式调用各种第三方服务。

**MCP的优势**:
- **标准化**: 统一的协议规范，降低集成成本
- **安全性**: 规范的认证和授权机制
- **可扩展**: 支持动态发现和注册工具
- **社区生态**: 丰富的第三方MCP服务器实现

## 技术栈

### 核心框架
- **Python 3.10-3.13**: 兼容多版本Python
- **FastAPI**: 高性能异步Web框架
- **Uvicorn**: ASGI服务器
- **MCP 1.9.4**: 模型上下文协议官方SDK

### 通信协议
- **SSE (Server-Sent Events)**: 支持SSE传输层
- **HTTP**: 支持标准HTTP传输层
- **Pydantic**: 数据验证和序列化

## 项目结构

```
genie-client/
├── mcp_client/
│   ├── __init__.py
│   ├── client.py              # MCP客户端核心实现
│   ├── server.py              # FastAPI服务器
│   ├── models.py              # 数据模型定义
│   └── config.py              # 配置管理
├── tests/                     # 测试代码
│   ├── test_client.py
│   └── test_server.py
├── .env_template              # 环境变量模板
├── pyproject.toml             # 项目依赖配置
├── requirements.txt           # pip依赖列表
├── server.py                  # 启动文件
└── README.md
```

## 核心功能

### 1. 工具发现与列表

**功能**: 连接MCP服务器，获取所有可用工具列表

**API端点**: `POST /v1/tool/list`

**请求示例**:
```json
{
  "server_url": "https://mcp.amap.com/sse?key=your_api_key"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "name": "maps_text_search",
      "description": "关键字搜索 API 根据用户输入的关键字进行 POI 搜索",
      "inputSchema": {
        "type": "object",
        "properties": {
          "keywords": {
            "type": "string",
            "description": "查询关键字"
          },
          "city": {
            "type": "string",
            "description": "查询城市"
          }
        },
        "required": ["keywords"]
      },
      "annotations": null
    }
  ]
}
```

### 2. 工具调用

**功能**: 调用MCP服务器提供的具体工具

**API端点**: `POST /v1/tool/call`

**请求示例**:
```json
{
  "server_url": "https://mcp.amap.com/sse?key=your_api_key",
  "name": "maps_geo",
  "arguments": {
    "address": "经海路地铁站"
  }
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "_meta": null,
    "content": [
      {
        "type": "text",
        "text": "{\"results\":[{\"country\":\"中国\",\"province\":\"北京市\",\"city\":\"北京市\",\"location\":\"116.562245,39.783587\",\"level\":\"公交地铁站点\"}]}",
        "annotations": null
      }
    ],
    "isError": false
  }
}
```

### 3. 健康检查

**功能**: 检查服务运行状态

**API端点**: `GET /health`

**响应示例**:
```json
{
  "status": "healthy",
  "timestamp": "2025-07-09T18:05:47.537919",
  "version": "0.1.0"
}
```

## 支持的MCP服务器示例

### 高德地图MCP服务器
- **URL**: `https://mcp.amap.com/sse?key={your_api_key}`
- **工具类别**: 地图搜索、路径规划、地理编码、天气查询
- **典型工具**:
  - `maps_text_search`: 关键字POI搜索
  - `maps_direction_driving`: 驾车路径规划
  - `maps_geo`: 地址转坐标
  - `maps_weather`: 天气查询

### 其他MCP服务器
- **文件系统服务器**: 提供本地文件操作能力
- **数据库服务器**: 提供数据库查询能力
- **自定义业务服务器**: 企业内部业务系统封装的MCP服务

## 安装和运行

### 前置要求

- **Python**: 3.10-3.13 版本
- **uv**: Python包管理工具（推荐）或 pip

### 使用 uv 安装（推荐）

1. 安装uv:
```bash
pip install uv
```

2. 创建虚拟环境:
```bash
cd genie-client
uv venv
```

3. 激活虚拟环境:
```bash
# Linux/Mac
source .venv/bin/activate

# Windows
.venv\Scripts\activate
```

4. 安装依赖:
```bash
uv pip install -e .
```

5. 配置环境变量（可选）:
```bash
# Linux/Mac
cp .env_template .env

# Windows
copy .env_template .env

# 编辑.env文件，配置默认MCP服务器等
```

6. 启动服务:
```bash
python server.py
```

### 使用 pip 安装

1. 创建虚拟环境:
```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

2. 安装依赖:
```bash
pip install -r requirements.txt
```

3. 启动服务:
```bash
python server.py
```

### 验证服务

1. 访问API文档: `http://localhost:8188/docs`
2. 健康检查:
```bash
curl http://localhost:8188/health
```

3. 测试工具列表（需要有效的MCP服务器URL）:
```bash
curl -X POST 'http://localhost:8188/v1/tool/list' \
  -H "Content-Type: application/json" \
  -d '{
    "server_url": "https://mcp.amap.com/sse?key=your_api_key"
  }'
```

## API文档

启动服务后访问:
- **Swagger UI**: `http://localhost:8188/docs`
- **ReDoc**: `http://localhost:8188/redoc`

### API端点列表

| 端点 | 方法 | 功能描述 |
|------|------|----------|
| `/health` | GET | 健康检查 |
| `/v1/tool/list` | POST | 获取MCP服务器工具列表 |
| `/v1/tool/call` | POST | 调用MCP服务器工具 |

## 配置说明

### 环境变量配置

创建 `.env` 文件:
```env
# 服务配置
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8188
LOG_LEVEL=INFO

# MCP配置
MCP_DEFAULT_SERVER=https://mcp.amap.com/sse
MCP_TIMEOUT=30
MCP_RETRY_TIMES=3

# 安全配置
API_KEY_REQUIRED=false
ALLOWED_ORIGINS=*
```

### MCP服务器注册

支持在配置文件中预注册常用MCP服务器:

```python
# config.py
MCP_SERVERS = {
    "amap": {
        "url": "https://mcp.amap.com/sse",
        "description": "高德地图MCP服务",
        "requires_api_key": True
    },
    "filesystem": {
        "url": "http://localhost:9000/mcp",
        "description": "本地文件系统MCP服务",
        "requires_api_key": False
    }
}
```

## 开发指南

### 添加新的MCP服务器支持

1. 在配置文件中注册服务器:
```python
# config.py
MCP_SERVERS["custom_service"] = {
    "url": "https://custom.example.com/mcp",
    "description": "自定义MCP服务",
    "requires_api_key": True,
    "default_headers": {
        "X-Custom-Header": "value"
    }
}
```

2. 如需特殊处理，可扩展客户端:
```python
# mcp_client/client.py
class CustomMCPClient(MCPClient):
    def __init__(self, server_url: str):
        super().__init__(server_url)
        # 自定义初始化逻辑

    async def call_tool(self, name: str, arguments: dict) -> dict:
        # 自定义工具调用逻辑
        result = await super().call_tool(name, arguments)
        # 后处理逻辑
        return result
```

### 运行测试

```bash
# 使用uv运行测试
uv run pytest

# 使用pytest运行测试
pytest

# 运行特定测试
pytest tests/test_client.py -v

# 查看测试覆盖率
pytest --cov=mcp_client tests/
```

### 调试技巧

**启用详细日志**:
```env
LOG_LEVEL=DEBUG
```

**查看MCP通信详情**:
```python
# 在client.py中添加日志
import logging
logging.basicConfig(level=logging.DEBUG)
```

**使用交互式文档测试**:
访问 `http://localhost:8188/docs`，使用Swagger UI进行交互式测试。

## 集成到智能体系统

### 后端集成示例

在 `genie-backend` 中调用MCP客户端服务:

```java
// McpTool.java
public class McpTool implements Tool {
    private static final String MCP_CLIENT_URL = "http://localhost:8188";

    public String listTools(String serverUrl) {
        // 调用 /v1/tool/list 接口
        Map<String, Object> request = Map.of("server_url", serverUrl);
        String response = httpClient.post(
            MCP_CLIENT_URL + "/v1/tool/list",
            request
        );
        return response;
    }

    public String callTool(String serverUrl, String toolName, Map<String, Object> args) {
        // 调用 /v1/tool/call 接口
        Map<String, Object> request = Map.of(
            "server_url", serverUrl,
            "name", toolName,
            "arguments", args
        );
        String response = httpClient.post(
            MCP_CLIENT_URL + "/v1/tool/call",
            request
        );
        return response;
    }
}
```

### 智能体调用流程

1. **工具发现**: 智能体启动时，通过 `/v1/tool/list` 获取可用工具
2. **工具注册**: 将MCP工具注册到智能体的工具库
3. **工具调用**: 智能体执行任务时，通过 `/v1/tool/call` 调用具体工具
4. **结果处理**: 解析工具返回结果，继续后续任务

## 生产部署

### 使用Docker部署

1. 构建Docker镜像:
```bash
docker build -t genie-client:1.0 .
```

2. 运行容器:
```bash
docker run -d \
  --name genie-client \
  -p 8188:8188 \
  genie-client:1.0
```

### 使用systemd管理服务

1. 创建服务文件:
```ini
# /etc/systemd/system/genie-client.service
[Unit]
Description=Genie MCP Client Service
After=network.target

[Service]
Type=simple
User=genie
WorkingDirectory=/opt/genie-client
Environment="PATH=/opt/genie-client/.venv/bin"
ExecStart=/opt/genie-client/.venv/bin/python server.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

2. 启动服务:
```bash
sudo systemctl daemon-reload
sudo systemctl enable genie-client
sudo systemctl start genie-client
sudo systemctl status genie-client
```

### 高可用部署

使用Nginx进行负载均衡:

```nginx
upstream mcp_client_backend {
    server 127.0.0.1:8188;
    server 127.0.0.1:8189;
    server 127.0.0.1:8190;
}

server {
    listen 80;
    server_name mcp-client.example.com;

    location / {
        proxy_pass http://mcp_client_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 常见问题

### Q1: 连接MCP服务器失败

**A**:
1. 检查MCP服务器URL是否正确
2. 验证网络连接是否畅通
3. 确认API密钥（如需要）是否有效
4. 查看服务日志获取详细错误信息

### Q2: 工具调用返回错误

**A**:
1. 确认工具名称和参数是否正确
2. 检查工具的inputSchema要求
3. 验证必填参数是否都已提供
4. 查看MCP服务器返回的详细错误信息

### Q3: 服务启动失败

**A**:
1. 检查Python版本是否在3.10-3.13范围内
2. 确认所有依赖已正确安装
3. 检查端口8188是否被占用
4. 查看启动日志中的错误信息

### Q4: SSE连接超时

**A**:
1. 增加超时时间配置
2. 检查MCP服务器是否支持SSE
3. 验证网络环境是否稳定
4. 尝试使用HTTP传输层

### Q5: 如何调试MCP通信问题？

**A**:
1. 启用DEBUG日志级别
2. 使用Wireshark抓包分析
3. 查看MCP服务器端日志
4. 使用Swagger UI进行交互式测试

## MCP协议规范

本服务遵循 MCP 1.9.4 协议规范，详细协议文档请参考:
- **官方文档**: https://modelcontextprotocol.io/
- **GitHub仓库**: https://github.com/modelcontextprotocol

## 技术支持

**内部技术支持**:
- 联系人: 华创证券科技研发中心
- 技术文档: 见项目Wiki
- Issue追踪: 内部JIRA系统

**相关文档**:
- [后端服务文档](../genie-backend/README.md)
- [AI工具服务文档](../genie-tool/README.md)
- [前端应用文档](../ui/README.md)
- [MCP协议文档](https://modelcontextprotocol.io/)
