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
     * 执行智能体调度（重构后）
     *
     * 架构说明：
     * - Controller层：负责HTTP处理、SSE管理、用户认证
     * - Service层：负责业务编排、智能体选择、平台路由
     * - Executor层：负责具体执行逻辑
     *
     * @param request Agent请求对象
     * @param httpRequest HTTP请求对象，用于获取用户认证信息
     * @return SSE事件流
     * @throws UnsupportedEncodingException 编码异常
     */
//    @PostMapping("/AutoAgent")
//    public SseEmitter AutoAgent(@RequestBody AgentRequest request, HttpServletRequest httpRequest)
//            throws UnsupportedEncodingException {
//
//        log.info("{} auto agent request: {}", request.getRequestId(), JSON.toJSONString(request));
//
//        // 1. 获取用户ID
//        Long userId = extractUserId(httpRequest, request);
//
//        // 2. 创建SSE
//        Long AUTO_AGENT_SSE_TIMEOUT = 60 * 60 * 1000L;
//        SseEmitter emitter = new SseEmitter(AUTO_AGENT_SSE_TIMEOUT);
//
//        // 3. 启动SSE心跳
//        ScheduledFuture<?> heartbeatFuture = startHeartbeat(emitter, request.getRequestId());
//
//        // 4. 调用编排服务
//        sessionOrchestrationService.orchestrate(request, userId, emitter);
//
//        return emitter;
//    }

    /**
     * 提取用户ID
     * 优先从JWT token获取，其次从request.erp字段获取
     *
     * @param httpRequest HTTP请求
     * @param request Agent请求
     * @return 用户ID（可能为null）
     */
    private Long extractUserId(HttpServletRequest httpRequest, AgentRequest request) {
        Long userId = getUserIdFromRequest(httpRequest);
        if (userId == null && request.getErp() != null) {
            try {
                userId = Long.parseLong(request.getErp());
                log.info("{} 从request.erp获取userId: {}", request.getRequestId(), userId);
            } catch (NumberFormatException e) {
                log.debug("{} request.erp不是有效的userId: {}", request.getRequestId(), request.getErp());
            }
        }
        return userId;
    }

    // ========== 以下方法已废弃，保留用于兼容性 ==========

    /**
     * 处理输出样式
     * @deprecated 已迁移到SessionContextBuilder，保留用于兼容性
     */
//    @Deprecated
//    private String handleOutputStyle(AgentRequest request) {
//        String query = request.getQuery();
//        Map<String, String> outputStyleMap = genieConfig.getOutputStylePrompts();
//        if (!StringUtils.isEmpty(request.getOutputStyle())) {
//            query += outputStyleMap.computeIfAbsent(request.getOutputStyle(), k -> "");
//        }
//        return query;
//    }

    /**
     * 构建工具集合
     * @deprecated 已迁移到ToolCollectionBuilderImpl，保留用于兼容性
     */
//    @Deprecated
//    private ToolCollection buildToolCollection(AgentContext agentContext, AgentRequest request) {
//        ToolCollection toolCollection = new ToolCollection();
//        toolCollection.setAgentContext(agentContext);
//
//        // data agent
//        if ("dataAgent".equals(request.getOutputStyle())) {
//            ReportTool htmlTool = new ReportTool();
//            htmlTool.setAgentContext(agentContext);
//            toolCollection.addTool(htmlTool);
//
//            DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
//            dataAnalysisTool.setAgentContext(agentContext);
//            toolCollection.addTool(dataAnalysisTool);
//        } else {
//            // file
//            FileTool fileTool = new FileTool();
//            fileTool.setAgentContext(agentContext);
//            toolCollection.addTool(fileTool);
//            // default tool
//            List<String> agentToolList = Arrays.asList(genieConfig.getMultiAgentToolListMap()
//                    .getOrDefault("default", "search,code,report").split(","));
//            if (!agentToolList.isEmpty()) {
//                if (agentToolList.contains("code")) {
//                    CodeInterpreterTool codeTool = new CodeInterpreterTool();
//                    codeTool.setAgentContext(agentContext);
//                    toolCollection.addTool(codeTool);
//                }
//                if (agentToolList.contains("report")) {
//                    ReportTool htmlTool = new ReportTool();
//                    htmlTool.setAgentContext(agentContext);
//                    toolCollection.addTool(htmlTool);
//                }
//                if (agentToolList.contains("search")) {
//                    DeepSearchTool deepSearchTool = new DeepSearchTool();
//                    deepSearchTool.setAgentContext(agentContext);
//                    toolCollection.addTool(deepSearchTool);
//                }
//                if (agentToolList.contains("data_analysis")) {
//                    DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
//                    dataAnalysisTool.setAgentContext(agentContext);
//                    toolCollection.addTool(dataAnalysisTool);
//                }
//            }
//        }
//
//        // mcp tool
//        try {
//            McpTool mcpTool = new McpTool();
//            mcpTool.setAgentContext(agentContext);
//            for (String mcpServer : genieConfig.getMcpServerUrlArr()) {
//                String listToolResult = mcpTool.listTool(mcpServer);
//                if (listToolResult.isEmpty()) {
//                    log.error("{} mcp server {} invalid", agentContext.getRequestId(), mcpServer);
//                    continue;
//                }
//
//                JSONObject resp = JSON.parseObject(listToolResult);
//                if (resp.getIntValue("code") != 200) {
//                    log.error("{} mcp serve {} code: {}, message: {}", agentContext.getRequestId(), mcpServer,
//                            resp.getIntValue("code"), resp.getString("message"));
//                    continue;
//                }
//                JSONArray data = resp.getJSONArray("data");
//                if (data.isEmpty()) {
//                    log.error("{} mcp serve {} code: {}, message: {}", agentContext.getRequestId(), mcpServer,
//                            resp.getIntValue("code"), resp.getString("message"));
//                    continue;
//                }
//                for (int i = 0; i < data.size(); i++) {
//                    JSONObject tool = data.getJSONObject(i);
//                    String method = tool.getString("name");
//                    String description = tool.getString("description");
//                    String inputSchema = tool.getString("inputSchema");
//                    toolCollection.addMcpTool(method, description, inputSchema, mcpServer);
//                }
//            }
//        } catch (Exception e) {
//            log.error("{} add mcp tool failed", agentContext.getRequestId(), e);
//        }
//
//        return toolCollection;
//    }

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
    