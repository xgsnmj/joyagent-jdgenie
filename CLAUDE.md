# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目架构

JoyAgent-JDGenie 是一个企业级智能分析平台，采用微服务架构：

- **genie-backend**: Java 17 + Spring Boot 3.2 后端服务
- **ui**: React 19 + TypeScript + Vite 前端应用
- **genie-tool**: Python 3.11 + FastAPI AI工具服务
- **数据库**: MySQL 8.0 + InnoDB

## 核心架构

### 1. 技术栈概览

**后端技术栈:**
- Java 17 + Spring Boot 3.2.2
- MyBatis-Plus 3.5.14 (ORM框架)
- Spring Security + JWT (认证授权)
- MySQL 8.0 (主数据库)
- FastJSON 1.2.83 (JSON处理)
- OkHttp 4.9.3 (HTTP客户端)
- Maven (构建工具)

**前端技术栈:**
- React 19 + TypeScript
- Vite 6.1.0 (构建工具)
- Ant Design 5.26.3 (UI组件库)
- Tailwind CSS 4.1.11 (样式框架)
- Zustand 5.0.3 (状态管理)
- React Router 7.6.2 (路由管理)

**AI工具服务 (genie-tool):**
- Python 3.11 + FastAPI
- LiteLLM 1.74.0+ (LLM统一接口)
- Uvicorn 0.35.0 (ASGI服务器)
- Pandas, NumPy, Matplotlib (数据处理)
- OpenAI, DeepSeek API集成

**MCP客户端 (genie-client):**
- Python 3.10-3.13
- FastAPI + MCP 1.9.4 (Model Context Protocol)
- 独立的微服务架构

### 2. 系统架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React前端     │    │  Spring Boot    │    │   AI工具服务    │
│   (端口:3000)   │◄──►│   后端API       │◄──►│   (端口:1601)   │
│                 │    │  (端口:8080)   │    │                 │
│ - 用户界面      │    │ - 智能体调度    │    │ - LLM调用       │
│ - 会话管理      │    │ - 认证授权      │    │ - 数据分析      │
│ - 实时交互      │    │ - 数据持久化    │    │ - 文件处理      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │   MySQL数据库   │              │
         └──────────────►│   (genie_db)   │◄─────────────┘
                        │                 │
                        │ - 用户管理      │
                        │ - 会话存储      │
                        │ - 消息历史      │
                        └─────────────────┘

┌─────────────────┐
│   MCP客户端     │
│   (端口:8188)   │
│                 │
│ - MCP协议处理   │
│ - 外部工具集成  │
│ - 服务发现      │
└─────────────────┘
```

## 核心模块详解

### 1. 智能体框架 (genie-backend)

**智能体类型:**
- **PlanningAgent**: 任务规划智能体，负责将复杂任务拆解为可执行的子任务
- **ExecutorAgent**: 任务执行智能体，使用工具完成具体任务
- **ReActAgent**: 反思执行智能体，基于ReAct模式进行思考和行动
- **SummaryAgent**: 结果总结智能体，对任务执行结果进行汇总

**核心工具类:**
- **DeepSearchTool**: 深度搜索工具，支持多源搜索
- **CodeInterpreterTool**: 代码解释器工具，支持Python代码执行
- **ReportTool**: 报告生成工具，支持HTML/Markdown/PPT格式
- **FileTool**: 文件处理工具，支持文件读写和管理
- **DataAnalysisTool**: 数据分析工具，集成数据库查询和分析
- **McpTool**: MCP协议工具，支持外部工具集成

**关键配置文件:**
- `E:\PycharmProjects\joyagent-jdgenie\genie-backend\src\main\resources\application.yml` - 主配置文件
- 包含LLM配置、数据库连接、智能体参数等

### 2. 前端应用 (ui)

**主要组件:**
- **ChatView**: 主聊天界面，支持实时对话流式显示
- **Dialogue**: 对话组件，支持多种内容格式渲染
- **ActionPanel**: 操作面板，展示文件、搜索结果、数据分析等
- **PlanView**: 计划视图，可视化显示任务执行计划
- **Sidebar**: 侧边栏，显示会话列表和用户信息

**状态管理:**
- **sessionStore**: 会话状态管理，使用Zustand
- **userStore**: 用户状态管理，包含登录状态和权限

**关键特性:**
- SSE (Server-Sent Events) 实时流式响应
- JWT认证集成
- 响应式设计，支持移动端
- 多种内容渲染器 (Markdown, HTML, 表格, 图表等)

### 3. AI工具服务 (genie-tool)

**核心API端点:**
- `/search` - 深度搜索服务
- `/code_interpreter` - 代码执行服务
- `/data_analysis` - 数据分析服务
- `/file_tool` - 文件处理服务
- `/report_tool` - 报告生成服务

**配置文件:**
- `E:\PycharmProjects\joyagent-jdgenie\genie-tool\.env_template` - 环境变量模板
- 需要配置API密钥、数据库连接等

### 4. 数据库设计

**核心表结构:**
- **sys_user**: 用户管理表，支持认证和权限控制
- **chat_session**: 会话管理表，存储用户会话信息
- **chat_message**: 消息表，存储完整的对话历史
- **chat_model_info**: 数据模型配置表，用于DataAgent
- **chat_model_schema**: 模型字段元数据表
- **sales_data**: 示例销售数据表
- **file_info**: 文件信息表，存储上传文件元数据

**详细设计文档:** `E:\PycharmProjects\joyagent-jdgenie\database_schema.md`

## 开发命令

### 环境要求检查
```bash
node --version      # 应显示 v18+
java -version       # 应显示 17+
python --version    # 应显示 3.11+
mvn -v             # 应显示 3.9.6+
mysql --version    # 应显示 8.0+
```

### 一键启动脚本 (推荐)
创建 `start-dev.bat`:
```batch
@echo off
echo 启动开发环境...
start "后端服务" cmd /k "cd genie-backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
timeout /t 10 /nobreak
start "前端服务" cmd /k "cd ui && npm run dev"
timeout /t 5 /nobreak
start "AI工具服务" cmd /k "cd genie-tool && uvicorn main:app --reload --host 0.0.0.0 --port 8000"
```

### 分步启动
```bash
# 1. 启动后端 (端口 8080)
cd genie-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 启动前端 (端口 5173)
cd ui
npm run dev

# 3. 启动AI工具服务 (端口 8000)
cd genie-tool
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Maven 构建和测试
```bash
# 使用指定配置文件编译
mvn clean compile -s "E:\apache-maven-3.9.6\conf\settings1.xml"

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

### 前端构建
```bash
cd ui
npm install          # 安装依赖
npm run build        # 生产构建
npm run preview      # 预览构建结果
npm run lint         # 代码检查
```

### Python 工具服务
```bash
cd genie-tool
# 安装依赖 (推荐使用 uv)
uv sync
# 或使用 pip
pip install -r requirements.txt

# 启动开发服务器
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 运行测试
pytest
```

## 核心模块架构

### 后端核心组件

#### 智能体框架 (`genie-backend/src/main/java/com/jd/genie/agent/`)
- **AgentContext**: 智能体运行环境，管理工具调用和状态
- **PlanningAgent**: 规划智能体，制定任务执行计划
- **ExecutorAgent**: 执行智能体，执行具体任务
- **ReActAgent**: ReAct模式智能体，支持推理-行动循环

#### 工具系统 (`genie-tool/genie_tool/tools/`)
- **search_tools**: 搜索工具，支持网页搜索和结果处理
- **code_interpreter**: 代码执行工具，支持Python代码沙箱运行
- **file_browser**: 文件浏览器工具，支持文件读写操作
- **data_analyzer**: 数据分析工具，支持CSV/Excel数据处理
- **report_generator**: 报告生成工具，支持Markdown和PDF生成

#### 数据模型
- **用户管理**: User, UserRole
- **会话系统**: Session, SessionAgent
- **消息存储**: ChatMessage (支持用户消息、AI响应、系统消息、工具调用)
- **文件管理**: FileInfo (支持会话文件关联)

### 前端架构

#### 主要组件 (`ui/src/components/`)
- **ChatView**: 聊天界面主组件，支持流式消息显示
- **Dialogue**: 对话气泡组件，支持Markdown渲染和代码高亮
- **Sidebar**: 侧边栏，会话历史管理和新会话创建
- **FileUploader**: 文件上传组件，支持多文件拖拽上传

#### 状态管理 (`ui/src/store/`)
- **session.ts**: 会话状态管理，使用 Zustand
- **agent.ts**: 智能体状态管理
- **chat.ts**: 聊天状态管理

#### API 层 (`ui/src/api/`)
- **chat.ts**: 聊天相关API，支持SSE流式响应
- **auth.ts**: 认证相关API
- **file.ts**: 文件操作API

## 关键配置

### 数据库配置
- **开发环境**: `application-dev.properties` 中的 MySQL 配置
- **连接池**: HikariCP，最大连接数20
- **JPA**: Spring Data JPA + Hibernate，显示SQL便于调试

### LLM 配置
- **DeepSeek**: 主要使用的LLM模型
- **配置位置**: `GenieConfig.java` 和环境变量
- **API密钥**: 通过环境变量配置

### 前端配置
- **Vite**: 开发服务器配置，代理后端API到 `/api`
- **Tailwind CSS**: 样式框架配置
- **TypeScript**: 严格模式，路径别名配置

## 开发模式

### SSE 流式响应
- **后端**: `SSEPrinter.java` 实现服务器推送事件
- **前端**: `querySSE.ts` 处理流式数据接收
- **用途**: AI响应实时显示，工具执行过程可视化

### 认证授权
- **JWT Token**: 用户认证机制
- **权限控制**: 基于角色的访问控制
- **会话管理**: Session机制管理用户会话状态

### 错误处理
- **统一异常处理**: `GlobalExceptionHandler.java`
- **前端错误边界**: React Error Boundary
- **网络错误**: Axios拦截器统一处理

## 数据库设计

核心表结构（详见 `database_schema.md`）：
- **users**: 用户基础信息
- **user_roles**: 用户角色关联
- **chat_sessions**: 聊天会话
- **session_agents**: 会话智能体关联
- **chat_messages**: 聊天消息（支持多种消息类型）
- **file_info**: 文件信息存储
- **session_files**: 会话文件关联

## 重要约定

### 代码风格
- **Java**: 遵循阿里巴巴Java开发规范
- **TypeScript**: 使用ESLint + Prettier
- **Python**: 遵循PEP 8，使用Black格式化

### 提交规范
- **feat**: 新功能
- **fix**: 修复bug
- **docs**: 文档更新
- **style**: 代码格式调整
- **refactor**: 代码重构
- **test**: 测试相关
- **chore**: 构建工具或辅助工具的变动

### 环境变量
敏感配置通过环境变量管理：
- `DB_PASSWORD`: 数据库密码
- `LLM_API_KEY`: LLM API密钥
- `JWT_SECRET`: JWT签名密钥

## 故障排除

### 常见问题
1. **端口冲突**: 确保8080、5173、8000端口未被占用
2. **数据库连接**: 检查MySQL服务是否启动，连接配置是否正确
3. **依赖安装**: Python依赖推荐使用uv，确保虚拟环境正确激活
4. **CORS问题**: 开发环境已配置CORS，生产环境需要配置反向代理

### 日志查看
- **后端日志**: `genie-backend/logs/application.log`
- **前端日志**: 浏览器开发者工具Console
- **AI工具日志**: 控制台输出，支持JSON格式化

## 性能优化

### 后端优化
- 数据库连接池调优
- JPA查询优化，避免N+1问题
- 异步处理长时间任务

### 前端优化
- 组件懒加载
- 虚拟滚动处理大量消息
- 图片和文件上传优化

### AI工具优化
- 工具调用结果缓存
- 并发工具执行
- 流式响应处理