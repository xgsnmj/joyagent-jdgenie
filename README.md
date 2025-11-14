# 华创证券智能体底座平台

简体中文 | [English Version](README_EN.md)

## 企业级智能体基础设施与服务平台

**华创证券科技研发中心** | 基于 JoyAgent-JDGenie 二次开发

---

## 项目概述

华创证券智能体底座平台是由华创证券科技研发中心基于开源项目 JoyAgent-JDGenie 进行二次开发的企业级智能体基础设施。平台提供完整的智能体调度、管理和服务能力，支持原生多智能体编排和外部智能体平台接入。

### 核心价值

- **统一的智能体基础设施**：为公司各业务线提供标准化的智能体服务
- **灵活的平台接入能力**：支持 Coze、通义点金、融汇等主流平台
- **强大的原生多智能体引擎**：自研的多智能体调度和编排能力
- **开箱即用的文档输出**：支持 HTML、PPT、Markdown 等多种格式

### 应用场景

- 投研报告自动生成
- 数据分析与可视化
- 智能问答与知识检索
- 业务流程自动化
- 代码辅助开发

---

## 核心功能

### 1. 原生多智能体调度引擎

- **多种工作模式**
  - PlanSolve 模式：规划解决复杂任务
  - ReAct 模式：推理-行动循环
  - Router 模式：智能路由分发（规划中）
  - Workflow 模式：工作流编排（规划中）

- **高性能执行引擎**
  - 高并发 DAG 执行
  - 多智能体协作
  - 流式输出支持
  - 上下文管理

- **丰富的工具集**
  - 深度搜索工具
  - 代码解释器
  - 数据分析工具
  - 报告生成工具
  - 文件处理工具
  - MCP 协议工具

### 2. 智能体社区

智能体社区是平台的核心特性之一，支持多平台智能体的统一管理和调度。

- **多平台接入支持**
  - 原生平台（Default）：本地多智能体体系
  - Coze 平台：字节跳动智能体平台
  - 通义点金：阿里云智能体服务
  - 融汇平台：企业级智能体平台
  - Dify 平台：开源 LLM 应用平台（规划中）

- **社区功能**
  - 智能体卡片展示
  - 分类和搜索
  - 使用统计
  - 公开/私有管理
  - 一键部署使用

### 3. 数据分析能力（DataAgent）

针对企业结构化数据提供开箱即用的智能分析能力。

- **数据治理**：DGP 协议支持
- **智能问数**：自然语言查询数据库
- **智能诊断**：自动化数据分析和异常检测
- **可视化输出**：图表和报表自动生成

### 4. 文档自动生成

支持多种格式的文档自动生成和输出。

- **HTML 报告**：富文本格式，支持图表和样式
- **PPT 演示**：自动生成演示文稿
- **Markdown 文档**：纯文本格式，易于版本控制

---

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React前端     │    │  Spring Boot    │    │   AI工具服务    │
│   (端口:3000)   │◄──►│   后端API       │◄──►│   (端口:1601)   │
│                 │    │  (端口:8080)    │    │                 │
│ - 用户界面      │    │ - 智能体调度    │    │ - LLM调用       │
│ - 会话管理      │    │ - 认证授权      │    │ - 数据分析      │
│ - 智能体社区    │    │ - 平台适配      │    │ - 工具执行      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │   MySQL数据库   │              │
         └──────────────►│                │◄─────────────┘
                        │ - 用户管理      │
                        │ - 会话存储      │
                        │ - 智能体配置    │
                        └─────────────────┘

┌─────────────────┐
│   MCP客户端     │
│   (端口:8188)   │
│                 │
│ - MCP协议支持   │
│ - 外部工具集成  │
└─────────────────┘
```

### 技术栈

**后端服务（genie-backend）**
- Java 17 + Spring Boot 3.2.2
- MyBatis-Plus 3.5.14
- Spring Security + JWT
- MySQL 8.0

**前端应用（ui）**
- React 19 + TypeScript
- Vite 6.1.0
- Ant Design 5.26.3
- Zustand 5.0.3（状态管理）

**AI工具服务（genie-tool）**
- Python 3.11 + FastAPI
- LiteLLM 1.74.0+
- Pandas、NumPy、Matplotlib

**MCP客户端（genie-client）**
- Python 3.10-3.13
- FastAPI + MCP 1.9.4

---

## 快速开始

### 环境要求

```bash
node --version      # v18+
java -version       # 17+
python --version    # 3.11+
mvn -v             # 3.9.6+
mysql --version    # 8.0+
```

### 方式1: Docker 一键部署（推荐）

```bash
# 1. 克隆项目
git clone [项目地址]
cd joyagent-jdgenie

# 2. 配置后端服务
# 编辑 genie-backend/src/main/resources/application.yml
# 配置 LLM 的 base_url、apikey、model 等参数

# 3. 配置AI工具服务
# 编辑 genie-tool/.env_template
# 配置 OPENAI_API_KEY、OPENAI_BASE_URL、DEFAULT_MODEL 等参数

# 4. 构建Docker镜像
docker build -t hczq-agent:latest .

# 5. 启动服务
docker run -d -p 3000:3000 -p 8080:8080 -p 1601:1601 --name hczq-agent hczq-agent:latest

# 6. 访问应用
# 浏览器打开 http://localhost:3000
```

### 方式2: 手动部署

#### 1. 环境准备

```bash
# 安装Python依赖管理工具
pip install uv

# 配置Python环境
cd genie-tool
uv sync
source .venv/bin/activate  # Linux/Mac
# 或
.venv\Scripts\activate     # Windows
```

#### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE genie_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构
-- 使用 database_schema.md 中的SQL语句创建表
```

#### 3. 配置文件

**后端配置（application.yml）**
```yaml
llm:
  base_url: "https://api.deepseek.com"
  api_key: "your-api-key"
  model: "deepseek-chat"
  max_tokens: 8192

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/genie_db
    username: root
    password: your-password
```

**AI工具配置（.env）**
```bash
# 复制模板
cp .env_template .env

# 编辑配置
OPENAI_API_KEY=your-api-key
OPENAI_BASE_URL=https://api.deepseek.com
DEFAULT_MODEL=deepseek/deepseek-chat
SERPER_SEARCH_API_KEY=your-serper-key
```

#### 4. 启动服务

**方案A：一键启动脚本（推荐）**
```bash
# 检查环境和端口
sh check_dep_port.sh

# 启动所有服务
sh Genie_start.sh
```

**方案B：分别启动**
```bash
# 1. 启动后端服务
cd genie-backend
mvn spring-boot:run

# 2. 启动前端服务
cd ui
npm install
npm run dev

# 3. 启动AI工具服务
cd genie-tool
uv run python server.py

# 4. 启动MCP客户端（可选）
cd genie-client
uv run python main.py
```

#### 5. 访问应用

- 前端界面：http://localhost:3000
- 后端API：http://localhost:8080
- AI工具服务：http://localhost:1601
- MCP客户端：http://localhost:8188

---

## 配置指南

### LLM配置

支持 OpenAI API 兼容的所有模型提供商：

**DeepSeek 配置示例**
```yaml
llm:
  base_url: "https://api.deepseek.com"
  api_key: "sk-xxx"
  model: "deepseek-chat"
  max_tokens: 8192
```

**通义千问配置示例**
```yaml
llm:
  base_url: "https://dashscope.aliyuncs.com/compatible-mode/v1"
  api_key: "sk-xxx"
  model: "qwen-max"
  max_tokens: 8000
```

### 数据库配置

MySQL 8.0+ 数据库配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/genie_db?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: none
```

### 外部平台配置

在智能体社区中配置外部平台：

1. 登录系统后，点击侧边栏"智能体社区"
2. 点击"管理我的智能体"
3. 点击"新增智能体"
4. 选择平台类型并填写配置信息

**Coze平台配置**
- 平台类型：coze
- API地址：https://api.coze.cn
- API密钥：从Coze平台获取
- Bot ID：智能体ID

**通义点金配置**
- 平台类型：tongyi
- API地址：https://dianjin.aliyun.com
- API密钥：从阿里云获取
- 工作空间ID：从通义点金获取
- Bot ID：智能体ID

**融汇平台配置**
- 平台类型：ronghui
- API地址：https://qagent.rxhui.com
- API密钥：从融汇平台获取
- 租户ID、登录用户ID等：从融汇平台获取

---

## 使用说明

### 使用原生智能体

1. 登录系统
2. 点击"新建对话"
3. 选择原生智能体（Default）
4. 输入问题，系统自动调度多智能体完成任务

**支持的任务类型**
- 信息搜索和整理
- 数据分析和可视化
- 报告生成（HTML/PPT/Markdown）
- 代码编写和执行
- 文件处理

### 使用外部平台智能体

1. 在智能体社区配置外部平台智能体
2. 创建新对话时选择该智能体
3. 正常进行对话交互
4. 系统自动调用外部平台API

### 智能体社区使用

**浏览公开智能体**
- 点击侧边栏"智能体社区"
- 浏览所有公开智能体
- 支持按分类筛选和搜索

**管理我的智能体**
- 点击"管理我的智能体"
- 查看、编辑、删除自己创建的智能体
- 设置智能体公开/私有状态

---

## 二次开发指南

### 添加自定义智能体工具

实现 `BaseTool` 接口：

```java
public class CustomTool implements BaseTool {
    @Override
    public String getName() {
        return "custom_tool";
    }

    @Override
    public String getDescription() {
        return "自定义工具描述";
    }

    @Override
    public Map<String, Object> toParams() {
        // 返回工具参数定义（JSON Schema格式）
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "input", Map.of(
                    "type", "string",
                    "description", "输入参数"
                )
            ),
            "required", List.of("input")
        );
    }

    @Override
    public Object execute(Object input) {
        // 工具执行逻辑
        return "执行结果";
    }
}
```

在 `ToolCollectionBuilder` 中注册工具：

```java
CustomTool customTool = new CustomTool();
toolCollection.addTool(customTool);
```

### 添加新的平台适配器

实现 `AgentAdapter` 接口：

```java
@Service("customAdapter")
public class CustomAgentAdapter implements AgentAdapter {

    @Override
    public String getProviderType() {
        return "custom";
    }

    @Override
    public SseEmitter chat(AgentProvider provider,
                          String userMessage,
                          List<ChatMessage> history,
                          SSEPrinter printer) {
        // 实现平台对接逻辑
        // 1. 构建请求参数
        // 2. 调用外部API
        // 3. 处理流式响应
        // 4. 通过printer输出结果
        return printer.getEmitter();
    }
}
```

### 添加 MCP 工具

在 `application.yml` 中配置 MCP 服务器：

```yaml
mcp_server_url: "http://ip1:port1/sse,http://ip2:port2/sse"
```

启动 MCP 服务器后，系统自动发现和加载工具。

---

## FAQ

### Q1: 如何切换使用的LLM模型？

A: 修改 `genie-backend/src/main/resources/application.yml` 中的 `llm` 配置，重启后端服务即可。

### Q2: 如何添加新的外部智能体平台？

A: 需要实现 `AgentAdapter` 接口并在 Spring 容器中注册。详见"二次开发指南"。

### Q3: 智能体社区的智能体如何管理？

A: 通过前端"智能体社区"->"管理我的智能体"进行管理。可以创建、编辑、删除和设置公开状态。

### Q4: 历史会话支持哪些智能体平台？

A: 支持所有已配置的平台。历史会话会记录使用的平台类型（agentProviderType）和工作模式（agentType）。

### Q5: 如何启用 DataAgent 数据分析能力？

A: 需要配置数据模型（chat_model_info 和 chat_model_schema 表），详见 `database_schema.md`。

### Q6: 支持哪些文档输出格式？

A: 目前支持 HTML、Markdown 和 PPT 三种格式。通过报告生成工具（ReportTool）实现。

### Q7: 如何配置搜索工具？

A: 需要在 `.env` 文件中配置 `SERPER_SEARCH_API_KEY`。可从 https://serper.dev 获取 API Key。

### Q8: 前端和后端如何联调？

A: 前端配置代理（vite.config.ts），后端配置 CORS。开发环境已默认配置好。

---

## 技术支持

如有问题或建议，请联系：

**华创证券科技研发中心**
- 技术支持：[内部技术支持邮箱]
- 问题反馈：[内部问题反馈系统]

---

## 版本历史

### v1.0.0 (2025-01)
- 初始版本发布
- 基于 JoyAgent-JDGenie v0.1.0 二次开发
- 支持原生多智能体调度
- 支持智能体社区和外部平台接入
- 支持 Coze、通义点金、融汇平台

---

## 致谢

本项目基于开源项目 [JoyAgent-JDGenie](https://github.com/jd-opensource/joyagent-jdgenie) 进行二次开发，感谢原项目团队的贡献。

---

**华创证券科技研发中心**
© 2025 华创证券. All rights reserved.
