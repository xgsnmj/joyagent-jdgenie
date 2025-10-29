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
     * 注册SSE事件
     * @param emitter
     * @param requestId
     * @param heartbeatFuture
     */
    private void registerSSEMonitor(SseEmitter emitter, String requestId, ScheduledFuture<?> heartbeatFuture) {
        // 监听SSE异常事件
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
        Long userId = getUserIdFromRequest(httpRequest);

        Long AUTO_AGENT_SSE_TIMEOUT = 60 * 60 * 1000L;

        SseEmitter emitter = new SseEmitter(AUTO_AGENT_SSE_TIMEOUT);
        // SSE心跳
        ScheduledFuture<?> heartbeatFuture = startHeartbeat(emitter, request.getRequestId());
        // 监听SSE事件
        registerSSEMonitor(emitter, request.getRequestId(), heartbeatFuture);
        // 拼接输出类型
        String originalQuery = request.getQuery(); // 保存原始query用于消息保存
        request.setQuery(handleOutputStyle(request));

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
                Printer printer = new SSEPrinter(emitter, request, request.getAgentType());
                AgentContext agentContext = AgentContext.builder()
                        .requestId(request.getRequestId())
                        .sessionId(request.getSessionId())
                        .printer(printer)
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
                        .build();
                // 构建工具列表
                agentContext.setToolCollection(buildToolCollection(agentContext, request));
                // 根据数据类型获取对应的处理器
                AgentHandlerService handler = agentHandlerFactory.getHandler(agentContext, request);
                // 执行处理逻辑
                handler.handle(agentContext, request);
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
     * @param params 查询请求参数对象，包含GPT查询所需信息
     * @return 返回SSE事件发射器，用于流式传输增量响应结果
     */
    @RequestMapping(value = "/web/api/v1/gpt/queryAgentStreamIncr", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryAgentStreamIncr(@RequestBody GptQueryReq params) {
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
    