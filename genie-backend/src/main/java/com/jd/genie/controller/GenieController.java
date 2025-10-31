package com.jd.genie.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.agent.printer.Printer;
import com.jd.genie.agent.printer.SSEPrinter;
import com.jd.genie.agent.tool.ToolCollection;
import com.jd.genie.agent.tool.common.*;
import com.jd.genie.agent.tool.mcp.McpTool;
import com.jd.genie.agent.util.DateUtil;
import com.jd.genie.agent.util.ThreadUtil;
import com.jd.genie.config.GenieConfig;
import com.jd.genie.util.ConversationDataCollector;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.model.req.GptQueryReq;
import com.jd.genie.service.AgentHandlerService;
import com.jd.genie.service.IGptProcessService;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.service.impl.AgentHandlerFactory;
import com.jd.genie.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/")
public class GenieController {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private static final long HEARTBEAT_INTERVAL = 10_000L; // 10秒心跳间隔
    @Autowired
    protected GenieConfig genieConfig;
    @Autowired
    private AgentHandlerFactory agentHandlerFactory;
    @Autowired
    private IGptProcessService gptProcessService;
    @Autowired
    private IChatHistoryService chatHistoryService;

    /**
     * 开启SSE心跳
     * @param emitter
     * @param requestId
     * @return
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
     * 注册SSE事件监听
     * 包括正常完成、超时、错误和用户主动中断
     *
     * @param emitter SSE发射器
     * @param requestId 请求ID
     * @param heartbeatFuture 心跳任务
     * @param agentContext Agent上下文
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    private void registerSSEMonitor(
            SseEmitter emitter,
            String requestId,
            ScheduledFuture<?> heartbeatFuture,
            AgentContext agentContext,
            String sessionId,
            Long userId
    ) {
        // 监听SSE正常完成事件
        emitter.onCompletion(() -> {
            log.info("{} SSE connection completed normally", requestId);
            heartbeatFuture.cancel(true);
            // 正常完成时消息已在handler.handle()后保存，无需重复保存
        });

        // 监听连接超时事件
        emitter.onTimeout(() -> {
            log.info("{} SSE connection timed out", requestId);
            heartbeatFuture.cancel(true);

            // 超时时保存已生成的内容
            savePartialResponse(sessionId, userId, agentContext, requestId, "timeout");

            emitter.complete();
        });

        // 监听连接错误事件（包括用户主动关闭）
        emitter.onError((ex) -> {
            log.info("{} SSE connection error: ", requestId, ex);
            heartbeatFuture.cancel(true);

            // 标记任务为已中断，通知处理线程停止
            if (agentContext != null) {
                agentContext.markInterrupted();
                log.info("{} 任务已标记为中断", requestId);
            }

            // 错误或中断时保存已生成的内容
            savePartialResponse(sessionId, userId, agentContext, requestId, "interrupted");

            emitter.completeWithError(ex);
        });
    }

    /**
     * 保存部分生成的回复（用于超时或中断场景）
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param agentContext Agent上下文
     * @param requestId 请求ID
     * @param reason 中断原因（timeout/interrupted）
     */
    private void savePartialResponse(
            String sessionId,
            Long userId,
            AgentContext agentContext,
            String requestId,
            String reason
    ) {
        if (userId == null || sessionId == null || agentContext == null) {
            return;
        }

        try {
            String partialResponse = agentContext.getAssistantResponse().toString();
            if (!partialResponse.isEmpty()) {
                // 添加中断标记
                String contentWithMark = partialResponse + "\n\n[生成已" +
                    ("timeout".equals(reason) ? "超时" : "中断") + "]";

                chatHistoryService.saveMessage(
                        sessionId,
                        "assistant",
                        contentWithMark,
                        null
                );
                log.info("保存部分回复: sessionId={}, reason={}, length={}",
                         sessionId, reason, partialResponse.length());
            } else {
                log.warn("部分回复为空，未保存: sessionId={}, reason={}", sessionId, reason);
            }
        } catch (Exception e) {
            log.error("保存部分回复失败: sessionId={}, reason={}, error={}",
                      sessionId, reason, e.getMessage(), e);
        }
    }

    /**
     * 执行智能体调度
     * @param request Agent请求对象
     * @param httpRequest HTTP请求对象，用于获取用户认证信息
     * @return SSE事件流
     * @throws UnsupportedEncodingException 编码异常
     */
    @PostMapping("/AutoAgent")
    public SseEmitter AutoAgent(@RequestBody AgentRequest request, HttpServletRequest httpRequest) throws UnsupportedEncodingException {

        log.info("{} auto agent request: {}", request.getRequestId(), JSON.toJSONString(request));

        // 获取当前用户ID（用于会话保存）
        // 优先从JWT token获取，如果不存在则从request.erp字段获取（内部调用场景）
        Long tempUserId = getUserIdFromRequest(httpRequest);
        if (tempUserId == null && request.getErp() != null) {
            try {
                tempUserId = Long.parseLong(request.getErp());
                log.info("{} 从request.erp获取userId: {}", request.getRequestId(), tempUserId);
            } catch (NumberFormatException e) {
                log.debug("{} request.erp不是有效的userId: {}", request.getRequestId(), request.getErp());
            }
        }
        // 创建final变量供lambda表达式使用
        final Long userId = tempUserId;

        Long AUTO_AGENT_SSE_TIMEOUT = 60 * 60 * 1000L;

        SseEmitter emitter = new SseEmitter(AUTO_AGENT_SSE_TIMEOUT);

        // 提前创建AgentContext（用于SSE事件监听中的部分回复保存）
        // 注意：此时只初始化基础字段，printer和toolCollection在ThreadUtil.execute内部设置
        AgentContext agentContext = AgentContext.builder()
                .requestId(request.getRequestId())
                .sessionId(request.getSessionId())
                .printer(null)  // 稍后在ThreadUtil.execute内部设置
                .query(request.getQuery())
                .task("")
                .dateInfo(DateUtil.CurrentDateInfo())
                .productFiles(new ArrayList<>())
                .taskProductFiles(new ArrayList<>())
                .sopPrompt(request.getSopPrompt())
                .basePrompt(request.getBasePrompt())
                .agentType(request.getAgentType())
                .isStream(Objects.nonNull(request.getIsStream()) ? request.getIsStream() : false)
                .templateType("dataAgent".equals(request.getOutputStyle()) ? "fix" : "empty")
                .assistantResponse(new StringBuilder())  // 显式初始化，用于累积AI回复
                .build();

        // 初始化会话数据收集器（用于收集完整的会话数据）
        agentContext.setDataCollector(new ConversationDataCollector());

        // SSE心跳
        ScheduledFuture<?> heartbeatFuture = startHeartbeat(emitter, request.getRequestId());
        // 监听SSE事件（传入agentContext以支持中断时保存部分回复）
        registerSSEMonitor(emitter, request.getRequestId(), heartbeatFuture, agentContext, request.getSessionId(), userId);

        // 拼接输出类型
        String originalQuery = request.getQuery(); // 保存原始query用于消息保存
        request.setQuery(handleOutputStyle(request));
        // 更新agentContext中的query（因为handleOutputStyle修改了request.getQuery()）
        agentContext.setQuery(request.getQuery());

        // 创建或获取会话并保存用户消息（如果用户已登录）
        if (userId != null && request.getSessionId() != null) {
            try {
                // 创建或获取会话
                chatHistoryService.createOrGetSession(
                        request.getSessionId(),
                        userId,
                        originalQuery.length() > 50 ? originalQuery.substring(0, 50) + "..." : originalQuery,
                        request.getAgentType() != null ? String.valueOf(request.getAgentType()) : "default",
                        request.getOutputStyle()
                );

                // 加载历史对话消息（用于多轮对话记忆）
                try {
                    List<com.jd.genie.model.dto.MessageVO> historyMessages = chatHistoryService.getSessionMessages(
                            request.getSessionId(),
                            userId
                    );

                    // 只保留最近N轮对话
                    int historyRounds = genieConfig.getConversationHistoryRounds();
                    int maxMessages = historyRounds * 2;  // 每轮包含user和assistant两条消息

                    if (historyMessages.size() > maxMessages) {
                        historyMessages = historyMessages.subList(
                            historyMessages.size() - maxMessages,
                            historyMessages.size()
                        );
                    }

                    // 转换为AgentRequest.HistoryMessage格式
                    List<AgentRequest.HistoryMessage> historyList = historyMessages.stream()
                        .map(msg -> AgentRequest.HistoryMessage.builder()
                            .role(msg.getRole())
                            .content(msg.getContent())
                            .createTime(msg.getCreateTime().toString())
                            .build())
                        .collect(java.util.stream.Collectors.toList());

                    request.setHistoryMessages(historyList);
                    log.info("加载历史对话: sessionId={}, historyCount={}",
                             request.getSessionId(), historyList.size());

                } catch (Exception e) {
                    log.error("加载历史对话失败: sessionId={}, error={}",
                              request.getSessionId(), e.getMessage(), e);
                    // 加载失败不影响主流程，继续执行
                }

                // 保存用户消息
                chatHistoryService.saveMessage(
                        request.getSessionId(),
                        "user",
                        originalQuery,
                        null
                );
                log.info("会话消息已保存: sessionId={}, userId={}", request.getSessionId(), userId);
            } catch (Exception e) {
                log.error("保存会话消息失败: sessionId={}, error={}", request.getSessionId(), e.getMessage(), e);
                // 继续执行，不因为保存失败而中断主流程
            }
        }

        // 执行调度引擎
        ThreadUtil.execute(() -> {
            try {
                // 创建SSEPrinter，传入agentContext
                Printer printer = new SSEPrinter(emitter, request, request.getAgentType(), agentContext);
                agentContext.setPrinter(printer);  // 设置printer

                // 构建工具列表
                agentContext.setToolCollection(buildToolCollection(agentContext, request));
                // 根据数据类型获取对应的处理器
                AgentHandlerService handler = agentHandlerFactory.getHandler(agentContext, request);
                // 执行处理逻辑
                handler.handle(agentContext, request);

                // 保存AI助手的回复消息
                if (userId != null && request.getSessionId() != null) {
                    try {
                        String assistantReply = agentContext.getAssistantResponse().toString();
                        if (!assistantReply.isEmpty()) {
                            // 提取文件信息并转换为JSON格式
                            String filesJson = null;
                            List<com.jd.genie.agent.dto.File> productFiles = agentContext.getProductFiles();
                            if (productFiles != null && !productFiles.isEmpty()) {
                                try {
                                    // 过滤内部文件，只保留用户可见的文件
                                    List<com.jd.genie.agent.dto.File> visibleFiles = productFiles.stream()
                                            .filter(file -> file.getIsInternalFile() == null || !file.getIsInternalFile())
                                            .collect(java.util.stream.Collectors.toList());

                                    if (!visibleFiles.isEmpty()) {
                                        filesJson = JSON.toJSONString(visibleFiles);
                                        log.info("AI回复包含文件: sessionId={}, fileCount={}",
                                                 request.getSessionId(), visibleFiles.size());
                                    }
                                } catch (Exception e) {
                                    log.error("序列化文件信息失败: sessionId={}, error={}",
                                              request.getSessionId(), e.getMessage(), e);
                                }
                            }

                            // 从数据收集器获取完整的会话数据
                            ConversationDataCollector collector = agentContext.getDataCollector();
                            String thoughtJson = collector != null ? collector.getThoughtJson() : null;
                            String tasksJson = collector != null ? collector.getTasksJson() : null;
                            String planJson = collector != null ? collector.getPlanJson() : null;
                            String metadataJson = collector != null ? collector.getMetadataJson() : null;

                            // 使用新的saveMessage方法保存完整数据
                            chatHistoryService.saveMessage(
                                    request.getSessionId(),
                                    "assistant",
                                    assistantReply,
                                    filesJson,
                                    thoughtJson,
                                    tasksJson,
                                    planJson,
                                    metadataJson
                            );
                            log.info("AI回复已保存（含完整数据）: sessionId={}, replyLength={}, hasFiles={}, hasThought={}, hasTasks={}, hasPlan={}",
                                     request.getSessionId(),
                                     assistantReply.length(),
                                     filesJson != null,
                                     thoughtJson != null,
                                     tasksJson != null,
                                     planJson != null);

                            // 异步生成会话标题
                            chatHistoryService.generateSessionTitleAsync(
                                    request.getSessionId(),
                                    originalQuery,  // 原始用户问题
                                    assistantReply
                            );
                        } else {
                            log.warn("AI回复为空，未保存: sessionId={}", request.getSessionId());
                        }
                    } catch (Exception e) {
                        log.error("保存AI回复失败: sessionId={}, error={}",
                                  request.getSessionId(), e.getMessage(), e);
                        // 继续执行，不因为保存失败而影响主流程
                    }
                }

                // 关闭连接
                emitter.complete();

            } catch (Exception e) {
                log.error("{} auto agent error", request.getRequestId(), e);
            }
        });

        return emitter;
    }


    /**
     * html模式： query+以 html展示
     * docs模式：query+以 markdown展示
     * table 模式: query+以 excel 展示
     */
    private String handleOutputStyle(AgentRequest request) {
        String query = request.getQuery();
        Map<String, String> outputStyleMap = genieConfig.getOutputStylePrompts();
        if (!StringUtils.isEmpty(request.getOutputStyle())) {
            query += outputStyleMap.computeIfAbsent(request.getOutputStyle(), k -> "");
        }
        return query;
    }


    /**
     * 构建工具列表
     *
     * @param agentContext
     * @param request
     * @return
     */
    private ToolCollection buildToolCollection(AgentContext agentContext, AgentRequest request) {

        ToolCollection toolCollection = new ToolCollection();
        toolCollection.setAgentContext(agentContext);

        // data agent
        if ("dataAgent".equals(request.getOutputStyle())) {
            ReportTool htmlTool = new ReportTool();
            htmlTool.setAgentContext(agentContext);
            toolCollection.addTool(htmlTool);

            DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
            dataAnalysisTool.setAgentContext(agentContext);
            toolCollection.addTool(dataAnalysisTool);
        } else {
            // file
            FileTool fileTool = new FileTool();
            fileTool.setAgentContext(agentContext);
            toolCollection.addTool(fileTool);
            // default tool
            List<String> agentToolList = Arrays.asList(genieConfig.getMultiAgentToolListMap()
                    .getOrDefault("default", "search,code,report").split(","));
            if (!agentToolList.isEmpty()) {
                if (agentToolList.contains("code")) {
                    CodeInterpreterTool codeTool = new CodeInterpreterTool();
                    codeTool.setAgentContext(agentContext);
                    toolCollection.addTool(codeTool);
                }
                if (agentToolList.contains("report")) {
                    ReportTool htmlTool = new ReportTool();
                    htmlTool.setAgentContext(agentContext);
                    toolCollection.addTool(htmlTool);
                }
                if (agentToolList.contains("search")) {
                    DeepSearchTool deepSearchTool = new DeepSearchTool();
                    deepSearchTool.setAgentContext(agentContext);
                    toolCollection.addTool(deepSearchTool);
                }
                if (agentToolList.contains("data_analysis")) {
                    DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
                    dataAnalysisTool.setAgentContext(agentContext);
                    toolCollection.addTool(dataAnalysisTool);
                }
            }
        }

        // mcp tool
        try {
            McpTool mcpTool = new McpTool();
            mcpTool.setAgentContext(agentContext);
            for (String mcpServer : genieConfig.getMcpServerUrlArr()) {
                String listToolResult = mcpTool.listTool(mcpServer);
                if (listToolResult.isEmpty()) {
                    log.error("{} mcp server {} invalid", agentContext.getRequestId(), mcpServer);
                    continue;
                }

                JSONObject resp = JSON.parseObject(listToolResult);
                if (resp.getIntValue("code") != 200) {
                    log.error("{} mcp serve {} code: {}, message: {}", agentContext.getRequestId(), mcpServer,
                            resp.getIntValue("code"), resp.getString("message"));
                    continue;
                }
                JSONArray data = resp.getJSONArray("data");
                if (data.isEmpty()) {
                    log.error("{} mcp serve {} code: {}, message: {}", agentContext.getRequestId(), mcpServer,
                            resp.getIntValue("code"), resp.getString("message"));
                    continue;
                }
                for (int i = 0; i < data.size(); i++) {
                    JSONObject tool = data.getJSONObject(i);
                    String method = tool.getString("name");
                    String description = tool.getString("description");
                    String inputSchema = tool.getString("inputSchema");
                    toolCollection.addMcpTool(method, description, inputSchema, mcpServer);
                }
            }
        } catch (Exception e) {
            log.error("{} add mcp tool failed", agentContext.getRequestId(), e);
        }

        return toolCollection;
    }

    /**
     * 探活接口
     *
     * @return
     */
    @RequestMapping(value = "/web/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }


    /**
     * 处理Agent流式增量查询请求，返回SSE事件流
     * 支持JWT认证和会话历史保存
     *
     * @param params 查询请求参数对象，包含GPT查询所需信息
     * @param httpRequest HTTP请求对象，用于获取JWT token
     * @return 返回SSE事件发射器，用于流式传输增量响应结果
     */
    @RequestMapping(value = "/web/api/v1/gpt/queryAgentStreamIncr", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryAgentStreamIncr(@RequestBody GptQueryReq params, HttpServletRequest httpRequest) {
        // 获取当前用户ID（从JWT token中解析）
        Long userId = getUserIdFromRequest(httpRequest);

        // 打印认证信息用于调试
        log.info("{} queryAgentStreamIncr: userId={}, sessionId={}, query={}",
                params.getRequestId(), userId, params.getSessionId(), params.getQuery());

        // 只创建会话，不保存用户消息（用户消息将在AutoAgent中保存，避免重复）
        if (userId != null && params.getSessionId() != null && params.getQuery() != null) {
            try {
                // 创建或获取会话
                String sessionTitle = params.getQuery().length() > 50 ?
                        params.getQuery().substring(0, 50) + "..." : params.getQuery();

                chatHistoryService.createOrGetSession(
                        params.getSessionId(),
                        userId,
                        sessionTitle,
                        "default",  // agentType
                        params.getOutputStyle()
                );

                log.info("会话已创建/获取: sessionId={}, userId={}", params.getSessionId(), userId);
            } catch (Exception e) {
                log.error("创建会话失败: sessionId={}, userId={}, error={}",
                        params.getSessionId(), userId, e.getMessage(), e);
                // 继续执行，不因为保存失败而中断主流程
            }
        }

        // 将userId设置到params.user字段（用于后续流程）
        if (userId != null) {
            params.setUser(String.valueOf(userId));
        }

        // 调用原有的处理逻辑
        return gptProcessService.queryMultiAgentIncrStream(params);
    }

    /**
     * 从请求中提取用户ID
     * 从JWT Token中解析出当前登录用户的ID
     *
     * @param request HTTP请求对象
     * @return 用户ID，如果未登录或Token无效则返回null
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        // 提取Token（去除Bearer前缀）
        String token = authorization;
        if (authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        // 从Token中获取用户ID
        return JwtUtil.getUserIdFromToken(token);
    }

}
    