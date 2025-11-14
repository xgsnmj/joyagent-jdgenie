# 华创证券智能体底座 - AI工具服务

## 项目简介

华创证券智能体底座AI工具服务是基于 Python 3.11 和 FastAPI 构建的高性能工具执行引擎，为智能体提供代码执行、深度搜索、文件处理、数据分析、报告生成等核心能力。本服务采用微服务架构，独立部署运行，通过HTTP接口为后端服务提供工具调用能力。

**开发单位**: 华创证券科技研发中心
**项目定位**: 企业内部智能体基础设施平台工具层

**最低要求**: Python >= 3.11

## 技术栈

### 核心框架
- **Python 3.11**: 采用最新Python版本，支持现代语法特性
- **FastAPI**: 高性能异步Web框架，自动生成OpenAPI文档
- **Uvicorn**: ASGI服务器，支持异步并发处理

### LLM集成
- **LiteLLM 1.74.0+**: 统一LLM调用接口，支持多种模型
- **OpenAI SDK**: OpenAI模型调用
- **DeepSeek SDK**: DeepSeek模型调用

### 数据处理
- **Pandas**: 数据分析和处理
- **NumPy**: 数值计算
- **Matplotlib**: 数据可视化
- **Seaborn**: 统计图表绘制
- **Plotly**: 交互式图表

### 搜索与爬虫
- **DuckDuckGo Search**: 网页搜索引擎
- **BeautifulSoup4**: HTML解析
- **Requests**: HTTP客户端

### 数据库
- **SQLAlchemy**: ORM框架
- **MySQL Connector**: MySQL数据库驱动

### 工具库
- **python-dotenv**: 环境变量管理
- **Jinja2**: 模板引擎（用于报告生成）
- **Markdown**: Markdown解析
- **python-pptx**: PPT文件生成

## 项目结构

```
genie-tool/
├── genie_tool/
│   ├── api/                          # API服务层
│   │   ├── code_interpreter.py       # 代码解释器API
│   │   ├── search.py                 # 深度搜索API
│   │   ├── file_tool.py              # 文件处理API
│   │   ├── data_analysis.py          # 数据分析API
│   │   ├── report_generator.py       # 报告生成API
│   │   └── __init__.py
│   ├── tool/                         # 工具执行逻辑
│   │   ├── code_interpreter/         # 代码解释器工具
│   │   │   ├── executor.py           # 代码执行器
│   │   │   ├── sandbox.py            # 沙箱环境
│   │   │   └── validator.py          # 代码安全校验
│   │   ├── search/                   # 搜索工具
│   │   │   ├── web_search.py         # 网页搜索
│   │   │   ├── content_extractor.py  # 内容提取器
│   │   │   └── summarizer.py         # 内容总结
│   │   ├── file/                     # 文件工具
│   │   │   ├── reader.py             # 文件读取
│   │   │   ├── writer.py             # 文件写入
│   │   │   └── parser.py             # 文件解析（PDF/Excel/Word）
│   │   ├── data_analysis/            # 数据分析工具
│   │   │   ├── analyzer.py           # 数据分析器
│   │   │   ├── visualizer.py         # 数据可视化
│   │   │   └── statistics.py         # 统计分析
│   │   └── report/                   # 报告生成工具
│   │       ├── html_generator.py     # HTML报告生成
│   │       ├── markdown_generator.py # Markdown报告生成
│   │       └── ppt_generator.py      # PPT报告生成
│   ├── model/                        # 数据模型
│   │   ├── request.py                # 请求模型
│   │   ├── response.py               # 响应模型
│   │   └── tool_result.py            # 工具执行结果模型
│   ├── prompt/                       # Prompt仓库
│   │   ├── search_prompt.py          # 搜索相关Prompt
│   │   ├── analysis_prompt.py        # 分析相关Prompt
│   │   └── report_prompt.py          # 报告相关Prompt
│   ├── util/                         # 工具类
│   │   ├── logger.py                 # 日志工具
│   │   ├── file_util.py              # 文件工具
│   │   ├── http_util.py              # HTTP工具
│   │   └── db_util.py                # 数据库工具
│   ├── db/                           # 数据库模块
│   │   ├── db_engine.py              # 数据库引擎初始化
│   │   ├── models.py                 # 数据库模型
│   │   └── crud.py                   # 数据库操作
│   └── config/                       # 配置模块
│       ├── settings.py               # 配置管理
│       └── constants.py              # 常量定义
├── tests/                            # 测试代码
│   ├── test_code_interpreter.py
│   ├── test_search.py
│   └── test_file_tool.py
├── .env_template                     # 环境变量模板
├── pyproject.toml                    # 项目依赖配置（uv）
├── requirements.txt                  # pip依赖列表
├── server.py                         # FastAPI服务启动文件
├── start.sh                          # 启动脚本（Linux/Mac）
├── start.bat                         # 启动脚本（Windows）
└── README.md
```

## 核心工具模块

### 1. 代码解释器 (Code Interpreter)

**功能描述**:
- 安全执行Python代码（沙箱环境）
- 支持数据分析、数值计算、绘图等操作
- 自动安装第三方依赖包
- 结果自动序列化返回

**API端点**: `POST /code_interpreter`

**请求示例**:
```json
{
  "code": "import pandas as pd\ndf = pd.DataFrame({'A': [1,2,3], 'B': [4,5,6]})\nprint(df.describe())",
  "timeout": 30
}
```

**响应示例**:
```json
{
  "success": true,
  "output": "             A         B\ncount  3.000000  3.000000\nmean   2.000000  5.000000\nstd    1.000000  1.000000\nmin    1.000000  4.000000\n...",
  "error": null,
  "execution_time": 0.125
}
```

**安全特性**:
- 禁止访问文件系统敏感路径
- 禁止执行系统命令
- 内存和CPU资源限制
- 执行超时保护

### 2. 深度搜索 (Deep Search)

**功能描述**:
- 多源网页搜索（DuckDuckGo等）
- 智能内容提取和清洗
- AI总结搜索结果
- 支持深度爬取链接内容

**API端点**: `POST /search`

**请求示例**:
```json
{
  "query": "华创证券最新研报",
  "num_results": 5,
  "deep_search": true
}
```

**响应示例**:
```json
{
  "success": true,
  "results": [
    {
      "title": "华创证券2024年度报告",
      "url": "https://example.com/report",
      "snippet": "华创证券发布2024年度报告...",
      "content": "完整内容...",
      "relevance_score": 0.95
    }
  ],
  "summary": "AI生成的总结内容...",
  "total_results": 5
}
```

**高级特性**:
- 智能去重
- 相关性排序
- 多语言支持
- 反爬虫策略

### 3. 文件处理 (File Tool)

**功能描述**:
- 支持多种文件格式读取（PDF、Word、Excel、TXT、CSV、JSON、XML）
- 文件内容解析和提取
- 文件格式转换
- 文件元数据提取

**API端点**:
- `POST /file_tool/read` - 读取文件
- `POST /file_tool/write` - 写入文件
- `POST /file_tool/parse` - 解析文件

**支持格式**:
- **文档类**: PDF, DOCX, TXT, MD
- **表格类**: XLSX, XLS, CSV
- **数据类**: JSON, XML, YAML
- **图片类**: PNG, JPG, JPEG（OCR识别）

**请求示例**:
```json
{
  "file_path": "/path/to/document.pdf",
  "extract_type": "text"
}
```

### 4. 数据分析 (Data Analysis)

**功能描述**:
- 结构化数据分析（CSV、Excel、SQL查询结果）
- 统计分析和描述性统计
- 数据可视化（折线图、柱状图、饼图、散点图等）
- 异常检测和趋势分析

**API端点**: `POST /data_analysis`

**请求示例**:
```json
{
  "data_source": "sql",
  "query": "SELECT * FROM sales_data WHERE date >= '2024-01-01'",
  "analysis_type": "trend",
  "visualization": true
}
```

**响应示例**:
```json
{
  "success": true,
  "statistics": {
    "total_rows": 1000,
    "mean_sales": 15234.5,
    "median_sales": 12500,
    "std_sales": 3456.7
  },
  "insights": [
    "销售额呈上升趋势",
    "周末销售额显著高于工作日"
  ],
  "charts": [
    {
      "type": "line",
      "title": "销售趋势图",
      "data_url": "/files/chart_abc123.png"
    }
  ]
}
```

**分析能力**:
- 描述性统计
- 相关性分析
- 时间序列分析
- 分组聚合分析

### 5. 报告生成 (Report Generator)

**功能描述**:
- 自动生成分析报告（HTML、Markdown、PPT）
- 模板化报告生成
- 数据和图表自动嵌入
- 支持自定义样式

**API端点**: `POST /report_generator`

**请求示例**:
```json
{
  "report_type": "html",
  "title": "销售数据分析报告",
  "sections": [
    {
      "type": "text",
      "content": "本报告分析了2024年第一季度的销售数据..."
    },
    {
      "type": "chart",
      "chart_data": {...}
    },
    {
      "type": "table",
      "table_data": [...]
    }
  ],
  "template": "default"
}
```

**支持格式**:
- **HTML**: 响应式网页报告，支持交互式图表
- **Markdown**: 通用文本报告，易于编辑
- **PPT**: PowerPoint演示文稿，支持自动排版

## 配置说明

### 环境变量配置

复制 `.env_template` 为 `.env` 并填写配置：

```env
# LLM配置
LLM_API_KEY=your_llm_api_key
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-chat

# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_db_password
DB_NAME=genie_db

# 服务配置
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8000
LOG_LEVEL=INFO

# 代码解释器配置
CODE_TIMEOUT=30
CODE_MAX_MEMORY_MB=512
CODE_ALLOWED_PACKAGES=pandas,numpy,matplotlib,seaborn,scipy

# 搜索配置
SEARCH_MAX_RESULTS=10
SEARCH_TIMEOUT=10
SEARCH_USER_AGENT=Mozilla/5.0

# 文件配置
FILE_UPLOAD_MAX_SIZE_MB=10
FILE_ALLOWED_EXTENSIONS=.pdf,.docx,.xlsx,.txt,.csv,.json,.xml,.md

# 报告配置
REPORT_OUTPUT_DIR=/tmp/reports
REPORT_TEMPLATE_DIR=./templates
```

### 依赖安装配置 (pyproject.toml)

```toml
[project]
name = "genie-tool"
version = "1.0.0"
description = "华创证券智能体底座AI工具服务"
requires-python = ">=3.11"

dependencies = [
    "fastapi>=0.115.0",
    "uvicorn[standard]>=0.35.0",
    "litellm>=1.74.0",
    "pandas>=2.2.0",
    "numpy>=2.0.0",
    "matplotlib>=3.9.0",
    "seaborn>=0.13.0",
    "plotly>=5.24.0",
    "beautifulsoup4>=4.12.0",
    "requests>=2.32.0",
    "sqlalchemy>=2.0.0",
    "pymysql>=1.1.0",
    "python-dotenv>=1.0.0",
    "jinja2>=3.1.0",
    "markdown>=3.7.0",
    "python-pptx>=1.0.0",
    "pydantic>=2.0.0"
]

[tool.uv]
dev-dependencies = [
    "pytest>=8.3.0",
    "pytest-asyncio>=0.24.0",
    "httpx>=0.27.0"
]
```

## 安装和运行

### 前置要求

- **Python**: 3.11 或更高版本
- **uv**: Python包管理工具（推荐）或 pip
- **MySQL**: 8.0 或更高版本（用于数据分析）

### 使用 uv 安装（推荐）

1. 安装uv:
```bash
pip install uv
```

2. 进入项目目录:
```bash
cd genie-tool
```

3. 同步依赖（自动创建虚拟环境）:
```bash
uv sync
```

4. 激活虚拟环境:
```bash
# Linux/Mac
source .venv/bin/activate

# Windows
.venv\Scripts\activate
```

5. 初始化数据库（仅首次启动需要）:
```bash
python -m genie_tool.db.db_engine
```

6. 配置环境变量:
```bash
# Linux/Mac
cp .env_template .env

# Windows
copy .env_template .env

# 编辑.env文件，填写实际配置
```

7. 启动服务:
```bash
uv run python server.py
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

3. 初始化数据库（仅首次启动需要）:
```bash
python -m genie_tool.db.db_engine
```

4. 启动服务:
```bash
python server.py
```

### 使用启动脚本

**Linux/Mac**:
```bash
chmod +x start.sh
./start.sh
```

**Windows**:
```cmd
start.bat
```

### 验证服务

1. 访问API文档: `http://localhost:8000/docs`
2. 健康检查: `http://localhost:8000/health`
3. 测试代码解释器:
```bash
curl -X POST "http://localhost:8000/code_interpreter" \
  -H "Content-Type: application/json" \
  -d '{"code": "print(\"Hello World\")"}'
```

## API文档

启动服务后访问: `http://localhost:8000/docs`

FastAPI自动生成的交互式API文档，支持在线测试所有接口。

### 核心接口列表

| 端点 | 方法 | 功能描述 |
|------|------|----------|
| `/health` | GET | 健康检查 |
| `/code_interpreter` | POST | 执行Python代码 |
| `/search` | POST | 深度网页搜索 |
| `/file_tool/read` | POST | 读取文件 |
| `/file_tool/write` | POST | 写入文件 |
| `/file_tool/parse` | POST | 解析文件 |
| `/data_analysis` | POST | 数据分析 |
| `/report_generator` | POST | 生成报告 |

## 开发指南

### 代码规范

- 遵循PEP 8 Python代码风格指南
- 使用Type Hints标注类型
- 所有公共函数必须添加Docstring
- 使用Black格式化代码
- 单元测试覆盖率要求 > 80%

### 添加新工具

1. 在 `genie_tool/tool/` 下创建新工具目录:
```bash
mkdir genie_tool/tool/new_tool
touch genie_tool/tool/new_tool/__init__.py
touch genie_tool/tool/new_tool/executor.py
```

2. 实现工具逻辑:
```python
# genie_tool/tool/new_tool/executor.py
from typing import Any, Dict

class NewToolExecutor:
    """新工具执行器"""

    def execute(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """
        执行新工具

        Args:
            params: 工具参数

        Returns:
            执行结果字典
        """
        # 实现工具逻辑
        result = self._do_work(params)

        return {
            "success": True,
            "result": result,
            "error": None
        }

    def _do_work(self, params: Dict[str, Any]) -> Any:
        """内部工作逻辑"""
        pass
```

3. 创建API端点:
```python
# genie_tool/api/new_tool.py
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from ..tool.new_tool.executor import NewToolExecutor

router = APIRouter()
executor = NewToolExecutor()

class NewToolRequest(BaseModel):
    param1: str
    param2: int

@router.post("/new_tool")
async def execute_new_tool(request: NewToolRequest):
    """新工具API端点"""
    try:
        result = executor.execute(request.dict())
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

4. 注册路由:
```python
# server.py
from genie_tool.api import new_tool

app.include_router(new_tool.router)
```

### 运行测试

```bash
# 使用uv运行测试
uv run pytest

# 使用pytest运行测试
pytest

# 运行特定测试文件
pytest tests/test_code_interpreter.py

# 查看测试覆盖率
pytest --cov=genie_tool tests/
```

### 调试技巧

**启用详细日志**:
```env
LOG_LEVEL=DEBUG
```

**使用FastAPI调试模式**:
```python
# server.py
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=True, log_level="debug")
```

**使用pdb调试**:
```python
import pdb
pdb.set_trace()  # 设置断点
```

## 生产部署

### 使用Docker部署

1. 构建Docker镜像:
```bash
docker build -t genie-tool:1.0 .
```

2. 运行容器:
```bash
docker run -d \
  --name genie-tool \
  -p 8000:8000 \
  -e LLM_API_KEY=your_key \
  -e DB_PASSWORD=your_password \
  genie-tool:1.0
```

### 使用systemd管理服务

1. 创建服务文件:
```ini
# /etc/systemd/system/genie-tool.service
[Unit]
Description=Genie Tool Service
After=network.target

[Service]
Type=simple
User=genie
WorkingDirectory=/opt/genie-tool
Environment="PATH=/opt/genie-tool/.venv/bin"
ExecStart=/opt/genie-tool/.venv/bin/python server.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

2. 启动服务:
```bash
sudo systemctl daemon-reload
sudo systemctl enable genie-tool
sudo systemctl start genie-tool
sudo systemctl status genie-tool
```

### Nginx反向代理

```nginx
server {
    listen 80;
    server_name tools.example.com;

    location / {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置（代码执行可能较长）
        proxy_read_timeout 300s;
        proxy_connect_timeout 10s;
    }
}
```

## 常见问题

### Q1: uv sync 失败

**A**:
1. 确保Python版本 >= 3.11
2. 更新uv到最新版本: `pip install --upgrade uv`
3. 删除`.venv`目录后重试
4. 使用国内镜像源: `uv sync --index-url https://pypi.tuna.tsinghua.edu.cn/simple`

### Q2: 代码执行超时

**A**:
1. 增加超时时间: `.env`中修改`CODE_TIMEOUT`
2. 优化代码逻辑，减少计算量
3. 检查是否有死循环或阻塞操作

### Q3: 数据库连接失败

**A**:
1. 检查MySQL服务是否启动
2. 验证`.env`中的数据库配置
3. 确认数据库用户权限
4. 检查防火墙设置

### Q4: 搜索结果为空

**A**:
1. 检查网络连接
2. 验证搜索引擎是否可访问
3. 调整搜索关键词
4. 查看日志中的详细错误信息

### Q5: 文件解析失败

**A**:
1. 确认文件格式是否支持
2. 检查文件是否损坏
3. 验证文件路径是否正确
4. 查看文件大小是否超过限制

### Q6: LLM调用失败

**A**:
1. 检查API密钥是否有效
2. 验证LLM_BASE_URL配置
3. 确认账户余额充足
4. 查看LiteLLM日志

## 性能优化

### 并发处理
- FastAPI原生支持异步并发
- 使用异步数据库驱动（如aiomysql）
- 合理配置Uvicorn的worker数量

### 缓存策略
- 搜索结果缓存（Redis）
- 文件解析结果缓存
- LLM响应缓存

### 资源限制
- 限制代码执行的内存和CPU
- 控制并发请求数量
- 实现请求队列机制

## 技术支持

**内部技术支持**:
- 联系人: 华创证券科技研发中心
- 技术文档: 见项目Wiki
- Issue追踪: 内部JIRA系统

**相关文档**:
- [后端服务文档](../genie-backend/README.md)
- [前端应用文档](../ui/README.md)
- [数据库设计文档](../database_schema.md)
- [API接口文档](http://localhost:8000/docs)
