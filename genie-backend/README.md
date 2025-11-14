# 华创证券智能体底座 - 后端服务

## 项目简介

华创证券智能体底座后端服务是基于 Java 17 和 Spring Boot 3.2 构建的企业级智能体调度平台核心服务，负责智能体任务调度、会话管理、外部平台适配和数据持久化等核心功能。本服务支持原生多智能体协作调度，同时提供智能体社区功能，可接入 Coze、通义千问、融汇等第三方智能体平台。

**开发单位**: 华创证券科技研发中心
**项目定位**: 企业内部智能体基础设施平台

## 技术栈

### 核心框架
- **Java 17**: 采用最新LTS版本，支持现代Java特性
- **Spring Boot 3.2.2**: 企业级微服务框架
- **MyBatis-Plus 3.5.14**: 高效的ORM框架，简化数据库操作
- **Spring Security + JWT**: 提供安全的认证授权机制

### 数据存储
- **MySQL 8.0**: 主数据库，存储用户、会话、消息等数据
- **HikariCP**: 高性能数据库连接池

### 通信与集成
- **SSE (Server-Sent Events)**: 实现实时流式响应
- **OkHttp 4.9.3**: 高效的HTTP客户端，用于调用外部服务
- **FastJSON 1.2.83**: 高性能JSON处理库

### 工具与插件
- **Lombok**: 简化Java代码编写
- **Swagger/OpenAPI 3**: 自动生成API文档
- **Maven**: 项目构建和依赖管理

## 项目结构

```
genie-backend/
├── src/
│   ├── main/
│   │   ├── java/com/jd/genie/
│   │   │   ├── agent/                    # 智能体核心模块
│   │   │   │   ├── core/                 # 智能体调度器
│   │   │   │   │   ├── DefaultAgentExecutor.java    # 原生智能体执行器
│   │   │   │   │   └── ExternalAgentExecutor.java   # 外部平台执行器
│   │   │   │   ├── plansolve/            # PlanSolve智能体
│   │   │   │   │   ├── PlanningAgent.java           # 规划智能体
│   │   │   │   │   ├── ExecutorAgent.java           # 执行智能体
│   │   │   │   │   └── SummaryAgent.java            # 总结智能体
│   │   │   │   └── react/                # ReAct智能体
│   │   │   │       └── ReActAgent.java               # 推理-行动智能体
│   │   │   ├── adapter/                  # 外部平台适配器
│   │   │   │   ├── AgentAdapter.java                # 智能体适配器接口
│   │   │   │   ├── CozeAdapter.java                 # Coze平台适配器
│   │   │   │   ├── TongyiAdapter.java               # 通义千问适配器
│   │   │   │   └── RonghuiAdapter.java              # 融汇平台适配器
│   │   │   ├── config/                   # 配置类
│   │   │   │   ├── GenieConfig.java                 # 核心配置
│   │   │   │   ├── SecurityConfig.java              # 安全配置
│   │   │   │   └── CorsConfig.java                  # 跨域配置
│   │   │   ├── controller/               # 控制器层
│   │   │   │   ├── ChatController.java              # 聊天接口
│   │   │   │   ├── ChatHistoryController.java       # 会话历史接口
│   │   │   │   ├── AuthController.java              # 认证接口
│   │   │   │   ├── AgentProviderController.java     # 智能体配置接口
│   │   │   │   └── DataModelController.java         # 数据模型接口
│   │   │   ├── service/                  # 服务层
│   │   │   │   ├── IChatHistoryService.java         # 会话历史服务接口
│   │   │   │   ├── IAgentProviderService.java       # 智能体配置服务
│   │   │   │   ├── IUserService.java                # 用户服务
│   │   │   │   └── impl/                            # 服务实现
│   │   │   ├── entity/                   # 实体类
│   │   │   │   ├── ChatSession.java                 # 会话实体
│   │   │   │   ├── ChatMessage.java                 # 消息实体
│   │   │   │   ├── User.java                        # 用户实体
│   │   │   │   └── AgentProvider.java               # 智能体配置实体
│   │   │   ├── model/                    # 数据模型
│   │   │   │   ├── dto/                             # 数据传输对象
│   │   │   │   ├── req/                             # 请求对象
│   │   │   │   └── resp/                            # 响应对象
│   │   │   ├── mapper/                   # MyBatis Mapper
│   │   │   ├── handler/                  # 异常处理器
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── util/                     # 工具类
│   │   │       ├── JwtUtil.java                     # JWT工具
│   │   │       └── SSEPrinter.java                  # SSE流式输出
│   │   └── resources/
│   │       ├── application.yml            # 主配置文件
│   │       ├── application-dev.yml        # 开发环境配置
│   │       ├── application-prod.yml       # 生产环境配置
│   │       └── mapper/                    # MyBatis XML映射文件
│   └── test/                              # 测试代码
├── pom.xml                                # Maven配置文件
└── README.md
```

## 核心模块说明

### 1. 智能体调度器 (agent/core/)

**原生智能体执行器 (DefaultAgentExecutor)**
- 负责调度PlanSolve和ReAct等原生智能体
- 支持流式SSE输出，实时返回思考过程和执行结果
- 集成工具调用能力（代码解释器、深度搜索、文件操作、数据分析等）

**外部平台执行器 (ExternalAgentExecutor)**
- 适配Coze、通义千问、融汇等第三方智能体平台
- 统一API接口，屏蔽不同平台差异
- 支持外部会话ID管理，实现多轮对话上下文连续性

### 2. 智能体实现 (agent/plansolve/ & agent/react/)

**PlanSolve智能体**
- **PlanningAgent**: 任务规划智能体，将用户问题拆解为可执行的子任务
- **ExecutorAgent**: 任务执行智能体，调用工具完成具体任务
- **SummaryAgent**: 结果总结智能体，汇总任务执行结果生成最终答案

**ReAct智能体**
- 基于推理-行动（Reasoning-Acting）循环模式
- 支持动态思考和工具调用
- 适用于复杂多步骤推理任务

### 3. 外部平台适配器 (adapter/)

**设计模式**: 策略模式 + 工厂模式
- **AgentAdapter**: 适配器接口，定义统一的调用规范
- **CozeAdapter**: 字节跳动Coze平台适配器
- **TongyiAdapter**: 阿里通义千问平台适配器
- **RonghuiAdapter**: 融汇智能平台适配器

**功能特性**:
- 会话创建与管理
- 流式/非流式响应处理
- 错误处理与重试机制
- 外部会话ID映射

### 4. 会话管理 (service/IChatHistoryService)

**核心功能**:
- 会话CRUD操作（创建、查询、删除、更新标题）
- 消息存储与检索（支持完整对话历史）
- 会话归属权限校验
- 外部会话ID管理
- 异步标题生成（基于AI总结）

**数据结构**:
- 支持保存完整的多智能体执行数据（思考过程、任务详情、计划信息等）
- metadata字段存储前端渲染所需的完整结构化数据

### 5. 智能体社区 (service/IAgentProviderService)

**智能体配置管理**:
- 支持配置多个外部智能体平台
- 记录平台类型、API密钥、Bot ID等信息
- 用户可选择默认智能体或会话级智能体

**平台类型**:
- `default`: 原生智能体（PlanSolve、ReAct）
- `coze`: Coze平台智能体
- `ronghui`: 融汇平台智能体
- `tongyi`: 通义千问平台智能体
- `dify`: Dify平台智能体（预留）

### 6. 认证授权 (config/SecurityConfig & util/JwtUtil)

**认证机制**: JWT (JSON Web Token)
- 用户登录后颁发Token
- 请求头携带 `Authorization: Bearer {token}`
- Token自动续期机制

**权限控制**:
- 基于用户ID的会话归属验证
- 管理员角色支持（预留）
- API接口级权限控制

## 配置说明

### 核心配置文件 (application.yml)

```yaml
server:
  port: 8080  # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/genie_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}  # 建议使用环境变量
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: none  # 生产环境必须设为none

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.jd.genie.entity
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

genie:
  # LLM配置
  llm:
    api-key: ${LLM_API_KEY}
    base-url: https://api.deepseek.com/v1
    model: deepseek-chat

  # 工具服务配置
  tools:
    code-interpreter-url: http://127.0.0.1:8000  # 代码解释器服务
    deep-search-url: http://127.0.0.1:8000       # 深度搜索服务
    mcp-client-url: http://127.0.0.1:8188        # MCP客户端服务

  # JWT配置
  jwt:
    secret: ${JWT_SECRET}
    expiration: 604800000  # 7天，单位毫秒
```

### 环境变量配置

**开发环境** (Windows):
```cmd
set DB_PASSWORD=your_db_password
set LLM_API_KEY=your_llm_api_key
set JWT_SECRET=your_jwt_secret
```

**生产环境** (Linux):
```bash
export DB_PASSWORD=your_db_password
export LLM_API_KEY=your_llm_api_key
export JWT_SECRET=your_jwt_secret
```

### 外部平台配置

在 `agent_provider` 表中配置外部智能体平台：

```sql
INSERT INTO agent_provider (provider_name, provider_type, api_key, base_url, bot_id, description)
VALUES
  ('Coze智能体', 'coze', 'your_coze_api_key', 'https://api.coze.cn/v3', 'your_bot_id', 'Coze平台智能体'),
  ('通义千问', 'tongyi', 'your_tongyi_api_key', 'https://dashscope.aliyuncs.com/api/v1', 'your_agent_id', '阿里通义千问智能体'),
  ('融汇智能', 'ronghui', 'your_ronghui_api_key', 'https://api.ronghui.ai', 'your_agent_id', '融汇平台智能体');
```

## API文档

启动服务后访问: `http://localhost:8080/swagger-ui.html`

### 主要接口

**聊天接口**:
- `POST /api/chat/stream` - 流式对话（SSE）
- `POST /api/chat` - 普通对话

**会话管理**:
- `GET /api/chat/sessions` - 获取会话列表（支持分页）
- `POST /api/chat/sessions` - 创建新会话
- `GET /api/chat/sessions/{sessionId}/messages` - 获取会话消息（含智能体信息）
- `DELETE /api/chat/sessions/{sessionId}` - 删除会话
- `PUT /api/chat/session/{sessionId}/title` - 更新会话标题

**认证接口**:
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/user` - 获取当前用户信息

**智能体配置**:
- `GET /api/agent-providers` - 获取可用智能体列表
- `POST /api/agent-providers` - 添加智能体配置
- `PUT /api/agent-providers/{id}` - 更新智能体配置

**数据模型配置**:
- `GET /api/data-models` - 获取数据模型列表
- `POST /api/data-models` - 创建数据模型

## 安装和运行

### 前置要求

- **JDK**: 17 或更高版本
- **Maven**: 3.9.6 或更高版本
- **MySQL**: 8.0 或更高版本
- **Node.js**: 18+ (用于前端开发)

### 数据库初始化

1. 创建数据库:
```sql
CREATE DATABASE genie_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入初始化脚本:
```bash
mysql -u root -p genie_db < database_schema.sql
```

### 本地开发

1. 克隆项目:
```bash
git clone [内部Git仓库地址]
cd joyagent-jdgenie/genie-backend
```

2. 配置环境变量:
```bash
# Windows
copy .env.example .env
# 编辑 .env 文件填入实际配置

# Linux/Mac
cp .env.example .env
# 编辑 .env 文件填入实际配置
```

3. 使用Maven编译:
```bash
mvn clean compile -s "E:\apache-maven-3.9.6\conf\settings1.xml"
```

4. 启动开发服务器:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev -s "E:\apache-maven-3.9.6\conf\settings1.xml"
```

5. 验证服务:
```bash
# 访问健康检查端点
curl http://localhost:8080/actuator/health

# 访问Swagger文档
# 浏览器打开 http://localhost:8080/swagger-ui.html
```

### 生产部署

1. 打包应用:
```bash
mvn clean package -DskipTests -Pprod -s "E:\apache-maven-3.9.6\conf\settings1.xml"
```

2. 运行JAR包:
```bash
java -jar target/genie-backend-1.0.0.jar --spring.profiles.active=prod
```

3. 使用systemd管理服务 (Linux):
```bash
# 创建服务文件 /etc/systemd/system/genie-backend.service
sudo systemctl start genie-backend
sudo systemctl enable genie-backend
```

## 开发指南

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码，避免冗余的getter/setter
- 所有公共方法必须添加JavaDoc注释
- 单元测试覆盖率要求 > 70%

### 添加新的外部平台适配器

1. 实现 `AgentAdapter` 接口:
```java
@Component
public class NewPlatformAdapter implements AgentAdapter {
    @Override
    public boolean supports(String providerType) {
        return "new_platform".equals(providerType);
    }

    @Override
    public String executeAgent(AgentProvider provider, String query,
                               String sessionId, String externalSessionId) {
        // 实现调用逻辑
    }
}
```

2. 在 `agent_provider` 表中添加配置:
```sql
INSERT INTO agent_provider (provider_name, provider_type, ...)
VALUES ('新平台', 'new_platform', ...);
```

### 添加新的工具

1. 在 `genie-tool` 服务中实现工具接口
2. 在 `agent/tools/` 目录下创建工具调用类
3. 在智能体配置中注册工具

### 调试技巧

**查看SQL执行日志**:
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**查看HTTP请求日志**:
```yaml
logging:
  level:
    com.jd.genie: DEBUG
    okhttp3: DEBUG
```

**使用IDEA断点调试**:
- 在 `DefaultAgentExecutor.execute()` 设置断点观察智能体调度流程
- 在 `ChatController.streamChat()` 观察SSE流式输出

## 数据库设计

详细的数据库设计文档请参考根目录的 `database_schema.md` 文件。

### 核心表说明

- **sys_user**: 用户管理表，支持JWT认证
- **chat_session**: 会话管理表，记录智能体类型和外部会话ID
- **chat_message**: 消息表，存储完整对话历史和元数据
- **agent_provider**: 智能体配置表，支持多平台接入
- **chat_model_info**: 数据模型配置表，用于DataAgent
- **sales_data**: 示例数据表，用于数据分析演示

## 常见问题

### Q1: 启动时提示 "Failed to configure a DataSource"

**A**: 检查以下几点:
1. MySQL服务是否已启动
2. `application.yml` 中的数据库连接配置是否正确
3. 数据库 `genie_db` 是否已创建
4. 环境变量 `DB_PASSWORD` 是否已设置

### Q2: LLM调用失败

**A**: 检查以下几点:
1. 环境变量 `LLM_API_KEY` 是否已设置
2. `genie.llm.base-url` 配置是否正确
3. 网络是否可以访问LLM服务
4. API密钥是否有效且有足够余额

### Q3: 外部智能体调用失败

**A**: 检查以下几点:
1. `agent_provider` 表中的配置是否正确
2. API密钥和Bot ID是否有效
3. 对应的Adapter是否已实现
4. 查看日志中的详细错误信息

### Q4: SSE流式输出中断

**A**: 可能原因:
1. 前端超时设置过短，延长EventSource的timeout
2. 网络代理或负载均衡器截断了长连接
3. 后端抛出异常，检查日志

### Q5: JWT Token验证失败

**A**: 检查以下几点:
1. 前端是否正确在请求头中携带Token
2. Token格式是否为 `Bearer {token}`
3. Token是否已过期
4. `JWT_SECRET` 是否配置正确

### Q6: 如何切换智能体类型？

**A**: 在调用 `/api/chat/stream` 接口时，传递不同的参数:
- `agentType`: 工作类型（plansolve/react）
- `agentProviderId`: 智能体配置ID（null为原生智能体，指定ID为外部平台）

示例:
```json
{
  "query": "帮我分析销售数据",
  "sessionId": "session-xxx",
  "agentType": "plansolve",
  "agentProviderId": 5
}
```

### Q7: Maven编译慢或依赖下载失败

**A**:
1. 使用指定的Maven配置文件: `-s "E:\apache-maven-3.9.6\conf\settings1.xml"`
2. 配置国内镜像源（阿里云Maven镜像）
3. 检查网络代理设置

## 技术支持

**内部技术支持**:
- 联系人: 华创证券科技研发中心
- 技术文档: 见项目Wiki
- Issue追踪: 内部JIRA系统

**相关文档**:
- [数据库设计文档](../database_schema.md)
- [API接口文档](http://localhost:8080/swagger-ui.html)
- [前端项目文档](../ui/README.md)
- [AI工具服务文档](../genie-tool/README.md)
