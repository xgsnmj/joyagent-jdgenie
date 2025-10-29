# JD Genie 会话流程完整链路分析

## 📋 目录
1. [概述](#概述)
2. [前端发起会话](#前端发起会话)
3. [后端接收与路由](#后端接收与路由)
4. [提示词组合与Agent调度](#提示词组合与agent调度)
5. [MCP服务调用](#mcp服务调用)
6. [流式输出与前端渲染](#流式输出与前端渲染)
7. [完整流程图](#完整流程图)
8. [关键技术点](#关键技术点)

---

## 概述

JD Genie 是一个基于大语言模型的智能问答助手，支持多Agent协作、工具调用和流式输出。整个会话链路从前端用户输入开始，经过后端的Agent调度、提示词组装、MCP服务调用，最终以SSE流式方式返回结果给前端渲染。

### 核心技术栈
- **前端**: React + TypeScript + SSE (Server-Sent Events)
- **后端**: Spring Boot + MyBatis-Plus + SSE
- **AI层**: 大语言模型 + Agent调度引擎
- **工具层**: MCP (Model Context Protocol) 服务

---

## 前端发起会话

### 1.1 入口组件: ChatView

**文件位置**: `ui/src/components/ChatView/index.tsx`

#### 关键代码流程

```typescript
/**
 * ChatView组件 - 会话主界面
 * 负责用户输入、消息展示和SSE流式输出接收
 */
const ChatView: GenieType.FC<Props> = (props) => {
  const { inputInfo: inputInfoProp, product } = props;

  // 获取会话store中的setIsStreaming方法，用于同步流式输出状态
  const { setIsStreaming } = useSessionStore();

  /**
   * 发送消息主函数
   * @param inputInfo 用户输入信息（包含消息内容、深度思考标志、输出样式等）
   */
  const sendMessage = useMemoizedFn((inputInfo: CHAT.TInputInfo) => {
    const { message, deepThink, outputStyle } = inputInfo;
    const requestId = getUniqId(); // 生成唯一请求ID

    // 1. 构建当前聊天对象
    let currentChat = combineCurrentChat(inputInfo, sessionId, requestId);
    chatList.current = [...chatList.current, currentChat];

    // 2. 设置标题和加载状态
    if (!chatTitle) {
      setChatTitle(message!);
    }
    setLoading(true);
    setIsStreaming(true); // 通知侧边栏：正在输出中，禁用新建会话按钮

    // 3. 准备请求参数
    const params = {
      sessionId: sessionId,      // 会话ID
      requestId: requestId,      // 请求ID
      query: message,            // 用户问题
      deepThink: deepThink ? 1 : 0,  // 是否深度思考
      outputStyle,               // 输出样式（docs/html/ppt/table）
    };

    // 4. 定义SSE消息处理回调
    const handleMessage = (data: MESSAGE.Answer) => {
      const { finished, resultMap, packageType, status } = data;

      // 处理各种事件类型
      if (packageType !== "heartbeat") {
        requestAnimationFrame(() => {
          if (resultMap?.eventData) {
            // 合并增量数据到当前chat
            currentChat = combineData(resultMap.eventData || {}, currentChat);

            // 处理任务列表
            const taskData = handleTaskData(currentChat, deepThink, currentChat.multiAgent);
            setTaskList(taskData.taskList);

            // 如果完成，停止加载
            if (finished) {
              currentChat.loading = false;
              setLoading(false);
              setIsStreaming(false); // 通知侧边栏：输出完成，恢复新建会话按钮
            }

            // 更新UI
            const newChatList = [...chatList.current];
            newChatList.splice(newChatList.length - 1, 1, currentChat);
            chatList.current = newChatList;
          }
        });
        scrollToTop(chatRef.current!);
      }
    };

    // 5. 发起SSE请求
    querySSE({
      body: params,
      handleMessage,
      handleError,
      handleClose,
    });
  });
};
```

### 1.2 SSE请求工具: querySSE

**文件位置**: `ui/src/utils/querySSE.ts`

```typescript
/**
 * 创建服务器发送事件（SSE）连接
 * @param config SSE配置对象
 * @param url 可选的自定义URL，默认使用 /web/api/v1/gpt/queryAgentStreamIncr
 */
export default (config: SSEConfig, url: string = DEFAULT_SSE_URL): void => {
  const { body = null, handleMessage, handleError, handleClose } = config;

  fetchEventSource(url, {
    method: 'POST',
    credentials: 'include',
    headers: getSSEHeaders(), // 动态获取包含JWT token的请求头
    body: JSON.stringify(body),
    openWhenHidden: true,

    // SSE消息回调
    onmessage(event: EventSourceMessage) {
      if (event.data) {
        try {
          const parsedData = JSON.parse(event.data);
          handleMessage(parsedData); // 调用ChatView提供的handleMessage
        } catch (error) {
          console.error('Error parsing SSE message:', error);
          handleError(new Error('Failed to parse SSE message'));
        }
      }
    },

    onerror(error: Error) {
      console.error('SSE error:', error);
      handleError(error);
    },

    onclose() {
      console.log('SSE connection closed');
      handleClose();
    }
  });
};
```

#### 前端请求数据结构

```json
{
  "sessionId": "uuid-string",      // 会话唯一标识
  "requestId": "uuid-string",      // 请求唯一标识
  "query": "用户问题内容",
  "deepThink": 0,                  // 0-普通模式，1-深度思考模式
  "outputStyle": "docs"            // docs/html/ppt/table
}
```

---

## 后端接收与路由

### 2.1 Controller层入口

**文件位置**: `genie-backend/src/main/java/com/jd/genie/controller/GenieController.java`

```java
/**
 * Genie控制器
 * 负责接收前端请求，进行参数验证和路由分发
 */
@Slf4j
@RestController
@RequestMapping("/")
public class GenieController {

    @Autowired
    private IGptProcessService gptProcessService;

    @Autowired
    private IChatHistoryService chatHistoryService;

    /**
     * 处理Agent流式增量查询请求，返回SSE事件流
     *
     * 接口路径: /web/api/v1/gpt/queryAgentStreamIncr
     * 请求方式: POST
     * 返回类型: SSE (Server-Sent Events)
     *
     * @param params 查询请求参数对象，包含：
     *               - sessionId: 会话ID
     *               - requestId: 请求ID
     *               - query: 用户问题
     *               - deepThink: 是否深度思考
     *               - outputStyle: 输出样式
     * @return 返回SSE事件发射器，用于流式传输增量响应结果
     */
    @RequestMapping(
        value = "/web/api/v1/gpt/queryAgentStreamIncr",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter queryAgentStreamIncr(@RequestBody GptQueryReq params) {
        // 直接委托给GptProcessService处理
        return gptProcessService.queryMultiAgentIncrStream(params);
    }

    /**
     * 执行智能体调度 - 更底层的入口
     *
     * 接口路径: /AutoAgent
     * 这个接口提供更细粒度的控制，包括工具配置、提示词等
     *
     * @param request Agent请求对象
     * @param httpRequest HTTP请求对象，用于获取用户认证信息
     * @return SSE事件流
     */
    @PostMapping("/AutoAgent")
    public SseEmitter AutoAgent(
        @RequestBody AgentRequest request,
        HttpServletRequest httpRequest
    ) throws UnsupportedEncodingException {

        log.info("{} auto agent request: {}",
            request.getRequestId(), JSON.toJSONString(request));

        // 1. 获取当前用户ID（用于会话保存）
        Long userId = getUserIdFromRequest(httpRequest);

        // 2. 创建SSE发射器（超时时间1小时）
        Long AUTO_AGENT_SSE_TIMEOUT = 60 * 60 * 1000L;
        SseEmitter emitter = new SseEmitter(AUTO_AGENT_SSE_TIMEOUT);

        // 3. 启动SSE心跳（每10秒发送一次，保持连接）
        ScheduledFuture<?> heartbeatFuture = startHeartbeat(emitter, request.getRequestId());

        // 4. 监听SSE事件（完成、超时、错误）
        registerSSEMonitor(emitter, request.getRequestId(), heartbeatFuture);

        // 5. 拼接输出类型到query
        String originalQuery = request.getQuery(); // 保存原始query用于消息保存
        request.setQuery(handleOutputStyle(request));

        // 6. 保存会话和消息（如果用户已登录）
        if (userId != null && request.getSessionId() != null) {
            try {
                // 创建或获取会话
                chatHistoryService.createOrGetSession(
                    request.getSessionId(),
                    userId,
                    originalQuery.length() > 50 ?
                        originalQuery.substring(0, 50) + "..." : originalQuery,
                    request.getAgentType() != null ?
                        String.valueOf(request.getAgentType()) : "default",
                    request.getOutputStyle()
                );

                // 保存用户消息
                chatHistoryService.saveMessage(
                    request.getSessionId(),
                    "user",
                    originalQuery,
                    null
                );
                log.info("会话消息已保存: sessionId={}, userId={}",
                    request.getSessionId(), userId);
            } catch (Exception e) {
                log.error("保存会话消息失败: sessionId={}, error={}",
                    request.getSessionId(), e.getMessage(), e);
                // 继续执行，不因为保存失败而中断主流程
            }
        }

        // 7. 异步执行Agent调度引擎
        ThreadUtil.execute(() -> {
            try {
                // 7.1 创建SSE打印器（用于流式输出）
                Printer printer = new SSEPrinter(emitter, request, request.getAgentType());

                // 7.2 构建Agent上下文
                AgentContext agentContext = AgentContext.builder()
                    .requestId(request.getRequestId())
                    .sessionId(request.getRequestId())
                    .printer(printer)
                    .query(request.getQuery())
                    .task("")
                    .dateInfo(DateUtil.CurrentDateInfo())
                    .productFiles(new ArrayList<>())
                    .taskProductFiles(new ArrayList<>())
                    .sopPrompt(request.getSopPrompt())
                    .basePrompt(request.getBasePrompt())
                    .agentType(request.getAgentType())
                    .isStream(Objects.nonNull(request.getIsStream()) ?
                        request.getIsStream() : false)
                    .templateType("dataAgent".equals(request.getOutputStyle()) ?
                        "fix" : "empty")
                    .build();

                // 7.3 构建工具列表（包括MCP工具）
                agentContext.setToolCollection(buildToolCollection(agentContext, request));

                // 7.4 根据数据类型获取对应的处理器
                AgentHandlerService handler = agentHandlerFactory.getHandler(
                    agentContext, request
                );

                // 7.5 执行处理逻辑（核心业务）
                handler.handle(agentContext, request);

                // 7.6 关闭SSE连接
                emitter.complete();

            } catch (Exception e) {
                log.error("{} auto agent error", request.getRequestId(), e);
            }
        });

        return emitter;
    }

    /**
     * 根据输出样式拼接提示词
     *
     * html模式: query + "以HTML展示"
     * docs模式: query + "以Markdown展示"
     * ppt模式: query + "以PPT展示"
     * table模式: query + "以Excel表格展示"
     *
     * @param request Agent请求对象
     * @return 拼接后的query
     */
    private String handleOutputStyle(AgentRequest request) {
        String query = request.getQuery();
        Map<String, String> outputStyleMap = genieConfig.getOutputStylePrompts();
        if (!StringUtils.isEmpty(request.getOutputStyle())) {
            query += outputStyleMap.computeIfAbsent(request.getOutputStyle(), k -> "");
        }
        return query;
    }
}
```

### 2.2 SSE心跳机制

```java
/**
 * 开启SSE心跳
 * 每10秒发送一次心跳消息，保持连接活跃，防止超时断开
 *
 * @param emitter SSE发射器
 * @param requestId 请求ID（用于日志标识）
 * @return 心跳任务Future，可用于取消
 */
private ScheduledFuture<?> startHeartbeat(SseEmitter emitter, String requestId) {
    return executor.scheduleAtFixedRate(() -> {
        try {
            // 发送心跳消息
            log.info("{} send heartbeat", requestId);
            emitter.send("heartbeat");
        } catch (Exception e) {
            // 发送心跳失败，关闭连接
            log.error("{} heartbeat failed, closing connection", requestId, e);
            emitter.completeWithError(e);
        }
    }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
}

/**
 * 注册SSE事件监听器
 * 监听连接的完成、超时、错误事件，确保资源正确释放
 *
 * @param emitter SSE发射器
 * @param requestId 请求ID
 * @param heartbeatFuture 心跳任务Future
 */
private void registerSSEMonitor(
    SseEmitter emitter,
    String requestId,
    ScheduledFuture<?> heartbeatFuture
) {
    // 监听SSE正常完成事件
    emitter.onCompletion(() -> {
        log.info("{} SSE connection completed normally", requestId);
        heartbeatFuture.cancel(true);
    });

    // 监听连接超时事件
    emitter.onTimeout(() -> {
        log.info("{} SSE connection timed out", requestId);
        heartbeatFuture.cancel(true);
        emitter.complete();
    });

    // 监听连接错误事件
    emitter.onError((ex) -> {
        log.info("{} SSE connection error: ", requestId, ex);
        heartbeatFuture.cancel(true);
        emitter.completeWithError(ex);
    });
}
```

---

## 提示词组合与Agent调度

### 3.1 工具集构建

**文件位置**: `genie-backend/src/main/java/com/jd/genie/controller/GenieController.java`

```java
/**
 * 构建工具列表
 * 根据输出样式（outputStyle）和配置，动态组装可用工具
 *
 * 工具类型：
 * 1. 内置工具: FileTool, CodeInterpreterTool, ReportTool, DeepSearchTool, DataAnalysisTool
 * 2. MCP工具: 从MCP服务器动态获取的外部工具
 *
 * @param agentContext Agent上下文
 * @param request Agent请求对象
 * @return 工具集合
 */
private ToolCollection buildToolCollection(
    AgentContext agentContext,
    AgentRequest request
) {

    ToolCollection toolCollection = new ToolCollection();
    toolCollection.setAgentContext(agentContext);

    // ===== 根据输出样式选择工具 =====

    if ("dataAgent".equals(request.getOutputStyle())) {
        // DataAgent模式：专注数据分析

        // 1. 报告工具 - 生成HTML/Markdown报告
        ReportTool htmlTool = new ReportTool();
        htmlTool.setAgentContext(agentContext);
        toolCollection.addTool(htmlTool);

        // 2. 数据分析工具 - SQL查询、数据处理
        DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
        dataAnalysisTool.setAgentContext(agentContext);
        toolCollection.addTool(dataAnalysisTool);

    } else {
        // 通用Multi-Agent模式

        // 1. 文件工具 - 文件读写、上传下载
        FileTool fileTool = new FileTool();
        fileTool.setAgentContext(agentContext);
        toolCollection.addTool(fileTool);

        // 2. 从配置获取默认工具列表
        List<String> agentToolList = Arrays.asList(
            genieConfig.getMultiAgentToolListMap()
                .getOrDefault("default", "search,code,report")
                .split(",")
        );

        if (!agentToolList.isEmpty()) {
            // 代码解释器 - Python代码执行
            if (agentToolList.contains("code")) {
                CodeInterpreterTool codeTool = new CodeInterpreterTool();
                codeTool.setAgentContext(agentContext);
                toolCollection.addTool(codeTool);
            }

            // 报告工具 - 生成报告
            if (agentToolList.contains("report")) {
                ReportTool htmlTool = new ReportTool();
                htmlTool.setAgentContext(agentContext);
                toolCollection.addTool(htmlTool);
            }

            // 深度搜索工具 - 互联网搜索
            if (agentToolList.contains("search")) {
                DeepSearchTool deepSearchTool = new DeepSearchTool();
                deepSearchTool.setAgentContext(agentContext);
                toolCollection.addTool(deepSearchTool);
            }

            // 数据分析工具
            if (agentToolList.contains("data_analysis")) {
                DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
                dataAnalysisTool.setAgentContext(agentContext);
                toolCollection.addTool(dataAnalysisTool);
            }
        }
    }

    // ===== 加载MCP工具 =====

    try {
        McpTool mcpTool = new McpTool();
        mcpTool.setAgentContext(agentContext);

        // 遍历所有配置的MCP服务器
        for (String mcpServer : genieConfig.getMcpServerUrlArr()) {
            // 1. 调用MCP服务器的listTool接口，获取工具列表
            String listToolResult = mcpTool.listTool(mcpServer);
            if (listToolResult.isEmpty()) {
                log.error("{} mcp server {} invalid",
                    agentContext.getRequestId(), mcpServer);
                continue;
            }

            // 2. 解析工具列表
            JSONObject resp = JSON.parseObject(listToolResult);
            if (resp.getIntValue("code") != 200) {
                log.error("{} mcp serve {} code: {}, message: {}",
                    agentContext.getRequestId(), mcpServer,
                    resp.getIntValue("code"), resp.getString("message"));
                continue;
            }

            // 3. 获取工具数组
            JSONArray data = resp.getJSONArray("data");
            if (data.isEmpty()) {
                log.error("{} mcp serve {} code: {}, message: {}",
                    agentContext.getRequestId(), mcpServer,
                    resp.getIntValue("code"), resp.getString("message"));
                continue;
            }

            // 4. 注册每个MCP工具
            for (int i = 0; i < data.size(); i++) {
                JSONObject tool = data.getJSONObject(i);
                String method = tool.getString("name");           // 工具方法名
                String description = tool.getString("description"); // 工具描述
                String inputSchema = tool.getString("inputSchema"); // 输入参数schema

                // 添加到工具集合
                toolCollection.addMcpTool(method, description, inputSchema, mcpServer);
            }
        }
    } catch (Exception e) {
        log.error("{} add mcp tool failed", agentContext.getRequestId(), e);
    }

    return toolCollection;
}
```

### 3.2 Agent上下文结构

```java
/**
 * Agent上下文 - 包含执行所需的所有信息
 */
public class AgentContext {
    private String requestId;          // 请求唯一ID
    private String sessionId;          // 会话ID
    private Printer printer;           // 输出打印器（SSE）
    private String query;              // 用户问题（可能已拼接输出样式提示）
    private String task;               // 当前任务描述
    private String dateInfo;           // 当前日期时间信息
    private List<File> productFiles;   // 产物文件列表
    private List<File> taskProductFiles; // 任务产物文件列表
    private String sopPrompt;          // SOP标准提示词
    private String basePrompt;         // 基础提示词
    private String agentType;          // Agent类型
    private boolean isStream;          // 是否流式输出
    private String templateType;       // 模板类型（fix/empty）
    private ToolCollection toolCollection; // 工具集合
}
```

### 3.3 提示词组装策略

```
最终提示词 = basePrompt + sopPrompt + 日期信息 + 用户问题 + 输出样式提示

示例：
basePrompt: "你是一个智能助手..."
sopPrompt: "请遵循以下步骤..."
dateInfo: "当前时间: 2025-10-28 10:00:00"
query: "分析一下京东的最新财报"
outputStyle: "以Markdown格式输出报告"

→ 最终提示词: "你是一个智能助手...请遵循以下步骤...当前时间: 2025-10-28 10:00:00 分析一下京东的最新财报，以Markdown格式输出报告"
```

---

## MCP服务调用

### 4.1 MCP工具初始化

**文件位置**: `genie-backend/src/main/java/com/jd/genie/agent/tool/mcp/McpTool.java`

```java
/**
 * MCP工具 - Model Context Protocol工具
 *
 * MCP是一个标准化协议，允许AI Agent调用外部工具和服务
 * 通过HTTP接口与MCP服务器通信，实现工具的动态加载和调用
 */
public class McpTool extends Tool {

    /**
     * 列出MCP服务器提供的所有工具
     *
     * @param mcpServerUrl MCP服务器地址
     * @return 工具列表JSON字符串
     */
    public String listTool(String mcpServerUrl) {
        try {
            // 1. 构建请求URL
            String url = mcpServerUrl + "/mcp/list_tools";

            // 2. 发送HTTP GET请求
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            // 3. 返回响应body
            return response.body();

        } catch (Exception e) {
            log.error("list mcp tools failed: {}", mcpServerUrl, e);
            return "";
        }
    }

    /**
     * 调用MCP工具
     *
     * @param mcpServerUrl MCP服务器地址
     * @param method 工具方法名
     * @param arguments 工具参数（JSON格式）
     * @return 工具执行结果
     */
    public String callTool(String mcpServerUrl, String method, String arguments) {
        try {
            // 1. 构建请求URL
            String url = mcpServerUrl + "/mcp/call_tool";

            // 2. 构建请求body
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", method);
            requestBody.put("arguments", JSON.parseObject(arguments));

            // 3. 发送HTTP POST请求
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody.toJSONString()
                    ))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            // 4. 解析响应
            JSONObject result = JSON.parseObject(response.body());
            if (result.getIntValue("code") == 200) {
                return result.getString("data");
            } else {
                log.error("call mcp tool failed: {} - {}",
                    method, result.getString("message"));
                return "Error: " + result.getString("message");
            }

        } catch (Exception e) {
            log.error("call mcp tool error: {}", method, e);
            return "Error: " + e.getMessage();
        }
    }
}
```

### 4.2 MCP工具注册流程

```
1. 启动时 / 请求时
   ↓
2. 遍历配置的MCP服务器列表
   - http://localhost:8001
   - http://localhost:8002
   ↓
3. 调用每个服务器的 /mcp/list_tools 接口
   ↓
4. 解析返回的工具列表
   {
     "code": 200,
     "data": [
       {
         "name": "search_web",
         "description": "搜索互联网内容",
         "inputSchema": {
           "type": "object",
           "properties": {
             "query": {"type": "string"}
           },
           "required": ["query"]
         }
       },
       {
         "name": "fetch_weather",
         "description": "获取天气信息",
         "inputSchema": {...}
       }
     ]
   }
   ↓
5. 注册到ToolCollection
   toolCollection.addMcpTool(
     "search_web",
     "搜索互联网内容",
     "{...inputSchema...}",
     "http://localhost:8001"
   )
```

### 4.3 MCP工具调用流程

```
1. LLM决策需要调用工具
   Tool: search_web
   Arguments: {"query": "京东最新财报"}
   ↓
2. Agent引擎识别为MCP工具
   ↓
3. 查找工具对应的MCP服务器地址
   search_web → http://localhost:8001
   ↓
4. 调用MCP服务器
   POST http://localhost:8001/mcp/call_tool
   Body: {
     "name": "search_web",
     "arguments": {"query": "京东最新财报"}
   }
   ↓
5. 接收MCP服务器响应
   {
     "code": 200,
     "data": "京东2024Q3财报：营收XXX亿..."
   }
   ↓
6. 将结果返回给LLM
   Tool Result: "京东2024Q3财报：营收XXX亿..."
   ↓
7. LLM基于工具结果继续生成回答
```

---

## 流式输出与前端渲染

### 5.1 SSE打印器

**文件位置**: `genie-backend/src/main/java/com/jd/genie/agent/printer/SSEPrinter.java`

```java
/**
 * SSE打印器 - 负责将Agent执行过程实时输出给前端
 *
 * 支持多种事件类型：
 * - TASK: 任务更新
 * - THOUGHT: 思考过程
 * - RESPONSE: 最终回答
 * - FILE: 文件产物
 * - ERROR: 错误信息
 */
public class SSEPrinter implements Printer {

    private SseEmitter emitter;
    private AgentRequest request;
    private String agentType;

    /**
     * 输出任务信息
     *
     * @param task 任务对象
     */
    @Override
    public void printTask(Task task) {
        try {
            JSONObject event = new JSONObject();
            event.put("eventType", "TASK");
            event.put("data", task);

            // 通过SSE发送给前端
            emitter.send(SseEmitter.event()
                .data(event.toJSONString())
                .name("message"));

        } catch (IOException e) {
            log.error("Failed to send task event", e);
        }
    }

    /**
     * 输出思考过程（流式）
     *
     * @param thought 思考内容片段
     */
    @Override
    public void printThought(String thought) {
        try {
            JSONObject event = new JSONObject();
            event.put("eventType", "THOUGHT");
            event.put("data", thought);
            event.put("finished", false); // 未完成，还有更多内容

            emitter.send(SseEmitter.event()
                .data(event.toJSONString())
                .name("message"));

        } catch (IOException e) {
            log.error("Failed to send thought event", e);
        }
    }

    /**
     * 输出最终回答（流式）
     *
     * @param response 回答内容片段
     * @param finished 是否完成
     */
    @Override
    public void printResponse(String response, boolean finished) {
        try {
            JSONObject event = new JSONObject();
            event.put("eventType", "RESPONSE");
            event.put("data", response);
            event.put("finished", finished);

            emitter.send(SseEmitter.event()
                .data(event.toJSONString())
                .name("message"));

            // 如果完成，关闭SSE连接
            if (finished) {
                emitter.complete();
            }

        } catch (IOException e) {
            log.error("Failed to send response event", e);
        }
    }

    /**
     * 输出文件产物
     *
     * @param file 文件对象
     */
    @Override
    public void printFile(File file) {
        try {
            JSONObject event = new JSONObject();
            event.put("eventType", "FILE");
            event.put("data", file);

            emitter.send(SseEmitter.event()
                .data(event.toJSONString())
                .name("message"));

        } catch (IOException e) {
            log.error("Failed to send file event", e);
        }
    }
}
```

### 5.2 SSE事件格式

```json
// 任务事件
{
  "eventType": "TASK",
  "data": {
    "taskId": "task-001",
    "taskName": "搜索资料",
    "status": "running",
    "progress": 30
  }
}

// 思考过程事件
{
  "eventType": "THOUGHT",
  "data": "我需要先搜索京东的最新财报信息...",
  "finished": false
}

// 回答事件（增量）
{
  "eventType": "RESPONSE",
  "data": "根据搜索结果，京东2024年Q3财报显示...",
  "finished": false
}

// 回答事件（完成）
{
  "eventType": "RESPONSE",
  "data": "。",
  "finished": true
}

// 文件产物事件
{
  "eventType": "FILE",
  "data": {
    "fileName": "report.html",
    "fileUrl": "/files/xxx.html",
    "fileType": "html"
  }
}

// 错误事件
{
  "eventType": "ERROR",
  "data": "工具调用失败: Connection timeout"
}
```

### 5.3 前端渲染

```typescript
/**
 * 前端处理SSE消息
 */
const handleMessage = (data: MESSAGE.Answer) => {
  const { finished, resultMap, packageType, status } = data;

  // 跳过心跳消息
  if (packageType === "heartbeat") {
    return;
  }

  // 处理增量数据
  requestAnimationFrame(() => {
    if (resultMap?.eventData) {
      // 1. 合并增量数据到当前chat对象
      currentChat = combineData(resultMap.eventData || {}, currentChat);

      // 2. 解析任务列表
      const taskData = handleTaskData(
        currentChat,
        deepThink,
        currentChat.multiAgent
      );
      setTaskList(taskData.taskList);

      // 3. 更新计划
      updatePlan(taskData.plan!);

      // 4. 打开右侧ActionView（显示任务进度）
      openAction(taskData.taskList);

      // 5. 如果完成，停止加载
      if (finished) {
        currentChat.loading = false;
        setLoading(false);
        setIsStreaming(false);
      }

      // 6. 更新UI
      const newChatList = [...chatList.current];
      newChatList.splice(newChatList.length - 1, 1, currentChat);
      chatList.current = newChatList;
    }
  });

  // 7. 滚动到顶部
  scrollToTop(chatRef.current!);
};
```

---

## 完整流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端 (React)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 1. 用户输入问题
                              │    - 问题内容
                              │    - 输出样式（docs/html/ppt/table）
                              │    - 深度思考标志
                              ↓
                    ┌──────────────────┐
                    │   ChatView组件    │
                    │  sendMessage()   │
                    └──────────────────┘
                              │
                              │ 2. 调用querySSE发起SSE请求
                              ↓
                    ┌──────────────────┐
                    │   querySSE工具    │
                    │  fetchEventSource │
                    └──────────────────┘
                              │
                              │ 3. POST请求到后端
                              │    URL: /web/api/v1/gpt/queryAgentStreamIncr
                              │    Headers: Authorization: Bearer <token>
                              │    Body: {sessionId, requestId, query, ...}
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 4. Controller接收请求
                              ↓
                    ┌──────────────────┐
                    │ GenieController  │
                    │ queryAgentStream │
                    └──────────────────┘
                              │
                              │ 5. 委托给Service层
                              ↓
                    ┌──────────────────┐
                    │GptProcessService │
                    │queryMultiAgent   │
                    └──────────────────┘
                              │
                              │ 6. 创建SSE Emitter
                              │    启动心跳线程
                              │    注册事件监听
                              ↓
                    ┌──────────────────┐
                    │  异步线程执行     │
                    │  Agent调度引擎    │
                    └──────────────────┘
                              │
                              ├─ 7. 构建Agent上下文
                              │    - requestId, sessionId
                              │    - query (拼接输出样式提示)
                              │    - printer (SSE打印器)
                              │    - dateInfo, sopPrompt, basePrompt
                              │
                              ├─ 8. 构建工具集合
                              │    ┌───────────────────┐
                              │    │  内置工具          │
                              │    │  - FileTool       │
                              │    │  - CodeInterpreter│
                              │    │  - ReportTool     │
                              │    │  - DeepSearchTool │
                              │    │  - DataAnalysisTool│
                              │    └───────────────────┘
                              │    ┌───────────────────┐
                              │    │  MCP工具           │
                              │    │  9. 从MCP服务器    │
                              │    │     动态获取工具    │
                              │    └───────────────────┘
                              │              │
                              │              │ GET /mcp/list_tools
                              │              ↓
                              │    ┌───────────────────┐
                              │    │  MCP Server 1     │
                              │    │  - search_web     │
                              │    │  - fetch_weather  │
                              │    └───────────────────┘
                              │    ┌───────────────────┐
                              │    │  MCP Server 2     │
                              │    │  - translate      │
                              │    │  - summarize      │
                              │    └───────────────────┘
                              │
                              ├─ 10. 获取Agent处理器
                              │     根据agentType选择对应的Handler
                              │
                              ↓
                    ┌──────────────────┐
                    │ AgentHandler     │
                    │  handle()        │
                    └──────────────────┘
                              │
                              │ 11. 执行Agent逻辑
                              │
                              ├─ Step 1: 任务规划
                              │    Printer.printTask("开始分析...")
                              │    → SSE → 前端显示任务
                              │
                              ├─ Step 2: 思考过程
                              │    Printer.printThought("我需要...")
                              │    → SSE → 前端显示思考
                              │
                              ├─ Step 3: 调用LLM
                              │    发送提示词到大模型
                              │    接收流式响应
                              │
                              ├─ Step 4: 工具调用（如需要）
                              │    LLM返回: Tool Call
                              │    ├─ 内置工具
                              │    │   直接执行
                              │    └─ MCP工具
                              │        POST /mcp/call_tool
                              │        → MCP Server
                              │        ← 工具结果
                              │
                              ├─ Step 5: 流式输出回答
                              │    For each chunk:
                              │      Printer.printResponse(chunk, false)
                              │      → SSE → 前端增量渲染
                              │
                              ├─ Step 6: 文件产物（如有）
                              │    Printer.printFile(file)
                              │    → SSE → 前端下载链接
                              │
                              └─ Step 7: 完成
                                   Printer.printResponse("", true)
                                   emitter.complete()
                                   → SSE关闭 → 前端停止加载
                              │
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                          前端 (React)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 12. 接收SSE事件流
                              │
                              ├─ onmessage(event)
                              │    解析JSON: eventType, data, finished
                              │
                              ├─ handleMessage(data)
                              │    根据eventType更新UI
                              │    - TASK → 更新任务列表
                              │    - THOUGHT → 显示思考过程
                              │    - RESPONSE → 增量渲染回答
                              │    - FILE → 显示下载链接
                              │    - ERROR → 显示错误提示
                              │
                              ├─ combineData()
                              │    合并增量数据到currentChat
                              │
                              ├─ requestAnimationFrame()
                              │    使用浏览器动画帧优化渲染
                              │
                              └─ scrollToTop()
                                   自动滚动到最新内容
                              │
                              ↓
                    ┌──────────────────┐
                    │   用户看到结果     │
                    │  - 思考过程       │
                    │  - 任务进度       │
                    │  - 流式回答       │
                    │  - 文件下载       │
                    └──────────────────┘
```

---

## 关键技术点

### 7.1 SSE vs WebSocket

**为什么选择SSE？**

| 特性 | SSE | WebSocket |
|------|-----|-----------|
| 通信方向 | 单向（服务器→客户端） | 双向 |
| 协议 | HTTP | 独立协议 |
| 浏览器支持 | 原生支持 | 原生支持 |
| 断线重连 | 自动 | 需手动实现 |
| 适用场景 | 服务器推送、流式输出 | 实时聊天、游戏 |

**JD Genie使用SSE的原因：**
- ✅ 单向通信足够（服务器向客户端推送结果）
- ✅ 自动断线重连
- ✅ 实现简单，复杂度低
- ✅ 与HTTP良好集成，易于调试

### 7.2 流式输出优化

```typescript
/**
 * 使用requestAnimationFrame优化渲染
 *
 * 问题：SSE可能在极短时间内发送大量消息，导致页面卡顿
 * 解决：使用requestAnimationFrame批量更新，每帧最多渲染一次
 */
const handleMessage = (data: MESSAGE.Answer) => {
  if (packageType !== "heartbeat") {
    requestAnimationFrame(() => {
      // 在下一个动画帧更新UI
      // 即使收到100条消息，也只渲染60次/秒（浏览器刷新率）
      updateUI(data);
    });
  }
};
```

### 7.3 MCP协议优势

**Model Context Protocol (MCP)** 是一个标准化的工具调用协议：

1. **动态工具发现**
   - 无需硬编码工具列表
   - MCP服务器启动时自动注册
   - 支持热插拔

2. **统一接口**
   ```
   GET  /mcp/list_tools  → 列出所有工具
   POST /mcp/call_tool   → 调用指定工具
   ```

3. **跨语言支持**
   - MCP服务器可用Python、Node.js、Java等实现
   - 只要遵循MCP协议即可

4. **安全隔离**
   - 工具在独立进程运行
   - 避免直接执行不可信代码

### 7.4 心跳机制

```
为什么需要心跳？
- SSE连接可能因网络问题、代理超时而断开
- 心跳消息保持连接活跃
- 每10秒发送一次 "heartbeat"

前端处理：
if (packageType === "heartbeat") {
  return; // 忽略心跳消息，不更新UI
}
```

### 7.5 错误处理

```java
// 后端
try {
    // 执行Agent逻辑
    handler.handle(agentContext, request);
} catch (Exception e) {
    log.error("Agent执行失败", e);

    // 发送错误事件给前端
    JSONObject errorEvent = new JSONObject();
    errorEvent.put("eventType", "ERROR");
    errorEvent.put("data", e.getMessage());
    emitter.send(SseEmitter.event().data(errorEvent.toJSONString()));

    // 关闭连接
    emitter.completeWithError(e);
}
```

```typescript
// 前端
const handleError = (error: Error) => {
  // 显示错误提示
  message.error('会话出错：' + error.message);

  // 停止加载
  setLoading(false);
  setIsStreaming(false);
};
```

### 7.6 会话保存

```java
// 后端在Agent执行前保存会话
if (userId != null && sessionId != null) {
    // 1. 创建或获取会话
    chatHistoryService.createOrGetSession(
        sessionId, userId, title, agentType, outputStyle
    );

    // 2. 保存用户消息
    chatHistoryService.saveMessage(
        sessionId, "user", query, null
    );
}

// Agent执行完成后保存AI回答（在Handler中）
chatHistoryService.saveMessage(
    sessionId, "assistant", response, files
);
```

---

## 总结

JD Genie的会话链路是一个完整的端到端流式交互系统：

1. **前端**: React + SSE实现流式UI更新
2. **后端**: Spring Boot + SSE实现流式推送
3. **AI层**: Agent调度引擎 + 提示词组装
4. **工具层**: 内置工具 + MCP动态工具

**核心优势**:
- ✅ **流式体验**: 用户无需等待，实时看到思考和输出过程
- ✅ **可扩展性**: MCP协议支持动态加载外部工具
- ✅ **可靠性**: SSE自动重连 + 心跳机制
- ✅ **可观测性**: 详细的日志和事件流

---

## 附录

### A. 相关文件清单

**前端**:
- `ui/src/components/ChatView/index.tsx` - 会话主组件
- `ui/src/utils/querySSE.ts` - SSE请求工具
- `ui/src/utils/chat.ts` - 数据合并工具
- `ui/src/store/session.ts` - 会话状态管理

**后端**:
- `genie-backend/src/main/java/com/jd/genie/controller/GenieController.java` - 控制器
- `genie-backend/src/main/java/com/jd/genie/service/IGptProcessService.java` - 服务接口
- `genie-backend/src/main/java/com/jd/genie/service/impl/GptProcessServiceImpl.java` - 服务实现
- `genie-backend/src/main/java/com/jd/genie/agent/` - Agent引擎
- `genie-backend/src/main/java/com/jd/genie/agent/tool/mcp/McpTool.java` - MCP工具

### B. 配置项

```yaml
# application.yml
genie:
  # MCP服务器列表
  mcp-server-urls:
    - http://localhost:8001
    - http://localhost:8002

  # 输出样式提示词映射
  output-style-prompts:
    docs: "请以Markdown格式输出，包含标题、章节和代码块"
    html: "请以HTML格式输出，包含样式和交互效果"
    ppt: "请以PPT大纲格式输出，包含标题和要点"
    table: "请以表格格式输出，包含表头和数据行"

  # 多Agent工具列表
  multi-agent-tool-list:
    default: "search,code,report,data_analysis"
```

---

**文档版本**: 1.0.0
**最后更新**: 2025-10-28
**维护者**: JD Genie Team
