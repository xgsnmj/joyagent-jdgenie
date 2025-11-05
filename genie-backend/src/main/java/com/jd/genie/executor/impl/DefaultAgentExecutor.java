package com.jd.genie.executor.impl;

import com.alibaba.fastjson.JSON;
import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.agent.enums.AgentType;
import com.jd.genie.agent.printer.Printer;
import com.jd.genie.agent.printer.SSEPrinter;
import com.jd.genie.agent.tool.ToolCollection;
import com.jd.genie.agent.util.ThreadUtil;
import com.jd.genie.config.GenieConfig;
import com.jd.genie.context.SessionContext;
import com.jd.genie.executor.AgentExecutor;
import com.jd.genie.handler.AgentResponseHandler;
import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.service.AgentHandlerService;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.service.ToolCollectionBuilder;
import com.jd.genie.service.impl.AgentHandlerFactory;
import com.jd.genie.util.ConversationDataCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认平台执行器
 * 处理系统默认智能体的执行逻辑
 *
 * 执行流程：
 * 1. 准备会话（创建或获取）
 * 2. 加载历史消息（支持多轮对话）
 * 3. 保存用户消息
 * 4. 异步执行智能体（构建工具、调用Handler）
 * 5. 保存AI回复
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class DefaultAgentExecutor implements AgentExecutor {

    @Autowired
    private GenieConfig genieConfig;

    @Autowired
    private IChatHistoryService chatHistoryService;

    @Autowired
    private AgentHandlerFactory agentHandlerFactory;

    @Autowired
    private ToolCollectionBuilder toolCollectionBuilder;

    @Autowired
    @Qualifier("reactAgentResponseHandler")
    private AgentResponseHandler agentResponseHandler;

    @Override
    public void execute(SessionContext context) {
        log.info("[默认执行器] 开始执行 - requestId: {}, sessionId: {}",
                context.getRequestId(), context.getSessionId());

        // 1. 准备会话
        prepareSession(context);

        // 2. 加载历史消息
        loadHistoryMessages(context);

        // 3. 保存用户消息
        saveUserMessage(context);

        // 4. 异步执行智能体
        executeAsync(context);
    }

    @Override
    public String getPlatformType() {
        return "default";
    }

    /**
     * 准备会话
     * 如果用户已登录，创建或获取会话
     */
    private void prepareSession(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[默认执行器] 跳过会话准备 - 未登录或无sessionId");
            return;
        }

        try {
            String title = context.getOriginalQuery().length() > 50
                    ? context.getOriginalQuery().substring(0, 50) + "..."
                    : context.getOriginalQuery();

            String agentType = context.getRequest().getAgentType() != null
                    ? String.valueOf(context.getRequest().getAgentType())
                    : "default";

            Long providerId = context.getAgentProvider() != null
                    ? context.getAgentProvider().getId()
                    : null;

            chatHistoryService.createOrGetSession(
                    context.getSessionId(),
                    context.getUserId(),
                    title,
                    agentType,
                    context.getRequest().getOutputStyle(),
                    providerId
            );

            log.debug("[默认执行器] 会话已准备 - sessionId: {}", context.getSessionId());

        } catch (Exception e) {
            log.error("[默认执行器] 准备会话失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 加载历史消息
     * 支持多轮对话，只保留最近N轮
     */
    private void loadHistoryMessages(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[默认执行器] 跳过历史加载 - 未登录或无sessionId");
            return;
        }

        try {
            // 获取历史消息
            List<MessageVO> historyMessages = chatHistoryService.getSessionMessages(
                    context.getSessionId(),
                    context.getUserId()
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
                    .collect(Collectors.toList());

            context.getRequest().setHistoryMessages(historyList);

            log.info("[默认执行器] 历史消息已加载 - sessionId: {}, count: {}",
                    context.getSessionId(), historyList.size());

        } catch (Exception e) {
            log.error("[默认执行器] 加载历史消息失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 加载失败不影响主流程
        }
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[默认执行器] 跳过用户消息保存 - 未登录或无sessionId");
            return;
        }

        try {
            chatHistoryService.saveMessage(
                    context.getSessionId(),
                    "user",
                    context.getOriginalQuery(),
                    null
            );

            log.debug("[默认执行器] 用户消息已保存 - sessionId: {}", context.getSessionId());

        } catch (Exception e) {
            log.error("[默认执行器] 保存用户消息失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 异步执行智能体
     * 使用ThreadUtil异步执行，避免阻塞HTTP线程
     */
    private void executeAsync(SessionContext context) {
        ThreadUtil.execute(() -> {
            try {
                log.info("[默认执行器] 异步任务开始 - requestId: {}", context.getRequestId());

                // 1. 创建Printer（集成AgentResponseHandler）
                Printer printer = new SSEPrinter(
                        context.getEmitter(),
                        context.getRequest(),
                        context.getRequest().getAgentType(),
                        context.getAgentContext(),
                        agentResponseHandler  // 传入Handler用于格式转换
                );
                context.getAgentContext().setPrinter(printer);

                // 2. 构建工具集合
                ToolCollection toolCollection = toolCollectionBuilder.build(
                        context.getAgentContext(),
                        context.getRequest()
                );
                context.getAgentContext().setToolCollection(toolCollection);

                log.debug("[默认执行器] 工具集合已构建 - requestId: {}, toolCount: {}",
                        context.getRequestId(), toolCollection.getToolMap().size());

                // 3. 获取Handler并执行
                AgentHandlerService handler = agentHandlerFactory.getHandler(
                        context.getAgentContext(),
                        context.getRequest()
                );

                log.info("[默认执行器] 开始执行Handler - requestId: {}, handlerType: {}",
                        context.getRequestId(), handler.getClass().getSimpleName());

                handler.handle(context.getAgentContext(), context.getRequest());

                log.info("[默认执行器] Handler执行完成 - requestId: {}", context.getRequestId());

                // 4. 保存AI回复
                saveAssistantReply(context);

                // 5. 完成SSE连接
                context.getEmitter().complete();

                log.info("[默认执行器] 异步任务完成 - requestId: {}", context.getRequestId());

            } catch (Exception e) {
                log.error("[默认执行器] 异步任务失败 - requestId: {}, error: {}",
                        context.getRequestId(), e.getMessage(), e);

                // 发送错误并完成连接
                try {
                    context.getEmitter().send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error")
                            .data("执行失败: " + e.getMessage()));
                    context.getEmitter().completeWithError(e);
                } catch (Exception ex) {
                    log.error("[默认执行器] 发送错误失败", ex);
                }
            }
        });
    }

    /**
     * 保存AI回复
     * 包含回复内容、文件信息、思考过程、任务详情等完整数据
     */
    private void saveAssistantReply(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[默认执行器] 跳过AI回复保存 - 未登录或无sessionId");
            return;
        }

        try {
            String assistantReply = context.getAgentContext().getAssistantResponse().toString();

            if (assistantReply.isEmpty()) {
                log.warn("[默认执行器] AI回复为空，未保存 - sessionId: {}", context.getSessionId());
                return;
            }

            // 提取文件信息
            String filesJson = extractFilesJson(context);

            // 从数据收集器获取所有 GptProcessResult 消息（JSON 格式）
            ConversationDataCollector collector = context.getAgentContext().getDataCollector();
            String allMessagesJson = collector != null ? collector.getAllMessagesJson() : null;

            // 保存消息（thought/tasks/plan 设为 null，所有数据在 metadata 中）
            chatHistoryService.saveMessage(
                    context.getSessionId(),
                    "assistant",
                    assistantReply,
                    filesJson,
                    null,  // thought = null
                    null,  // tasks = null
                    null,  // plan = null
                    allMessagesJson  // metadata = 所有 GptProcessResult
            );

            log.info("[默认执行器] AI回复已保存 - sessionId: {}, length: {}, hasFiles: {}, hasMetadata: {}",
                    context.getSessionId(),
                    assistantReply.length(),
                    filesJson != null,
                    allMessagesJson != null);

            // 异步生成会话标题
            chatHistoryService.generateSessionTitleAsync(
                    context.getSessionId(),
                    context.getOriginalQuery(),
                    assistantReply
            );

        } catch (Exception e) {
            log.error("[默认执行器] 保存AI回复失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 提取文件信息JSON
     * 过滤内部文件，只保留用户可见的文件
     */
    private String extractFilesJson(SessionContext context) {
        List<com.jd.genie.agent.dto.File> productFiles = context.getAgentContext().getProductFiles();

        if (productFiles == null || productFiles.isEmpty()) {
            return null;
        }

        try {
            // 过滤内部文件，只保留用户可见的文件
            List<com.jd.genie.agent.dto.File> visibleFiles = productFiles.stream()
                    .filter(file -> file.getIsInternalFile() == null || !file.getIsInternalFile())
                    .collect(Collectors.toList());

            if (visibleFiles.isEmpty()) {
                return null;
            }

            String filesJson = JSON.toJSONString(visibleFiles);

            log.debug("[默认执行器] 文件信息已提取 - sessionId: {}, fileCount: {}",
                    context.getSessionId(), visibleFiles.size());

            return filesJson;

        } catch (Exception e) {
            log.error("[默认执行器] 序列化文件信息失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            return null;
        }
    }
}
