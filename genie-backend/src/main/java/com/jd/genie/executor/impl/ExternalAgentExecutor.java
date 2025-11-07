package com.jd.genie.executor.impl;

import com.jd.genie.adapter.AgentAdapter;
import com.jd.genie.adapter.AgentAdapterFactory;
import com.jd.genie.context.SessionContext;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.entity.ChatMessage;
import com.jd.genie.entity.ChatSession;
import com.jd.genie.executor.AgentExecutor;
import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.util.ConversationDataCollector;
import com.jd.genie.util.InterceptableSseEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 外部平台执行器
 * 处理Coze、融汇等外部智能体平台的执行逻辑
 *
 * 执行流程：
 * 1. 准备会话（创建或获取）
 * 2. 保存用户消息
 * 3. 获取历史消息（用于多轮对话）
 * 4. 调用外部平台适配器
 * 5. 保存外部会话ID（首次对话）
 *
 * 注意事项：
 * - 外部平台的AI回复由适配器内部保存，不需要在此保存
 * - SSE流由适配器直接返回，不需要在此complete
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class ExternalAgentExecutor implements AgentExecutor {

    @Autowired
    private IChatHistoryService chatHistoryService;

    @Autowired
    private AgentAdapterFactory adapterFactory;

    @Override
    public void execute(SessionContext context) {
        log.info("[外部执行器] 开始执行 - requestId: {}, platformType: {}, sessionId: {}",
                context.getRequestId(),
                context.getPlatformType(),
                context.getSessionId());

        // 验证智能体配置
        AgentProvider provider = context.getAgentProvider();
        if (provider == null) {
            throw new IllegalStateException("外部平台执行器缺少智能体配置");
        }

        // 1. 准备会话
        prepareSession(context);

        // 2. 保存用户消息
        saveUserMessage(context);

        // 3. 获取历史消息
        List<ChatMessage> history = getHistoryMessages(context);

        // 4. 调用外部平台
        callExternalPlatform(context, history, provider);
    }

    @Override
    public String getPlatformType() {
        return "external"; // 通用外部平台执行器
    }

    /**
     * 准备会话
     * 如果用户已登录，创建或获取会话
     */
    private void prepareSession(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[外部执行器] 跳过会话准备 - 未登录或无sessionId");
            return;
        }

        try {
            String title = context.getOriginalQuery().length() > 50
                    ? context.getOriginalQuery().substring(0, 50) + "..."
                    : context.getOriginalQuery();

            String agentType = context.getRequest().getAgentType() != null
                    ? String.valueOf(context.getRequest().getAgentType())
                    : "default";

            chatHistoryService.createOrGetSession(
                    context.getSessionId(),
                    context.getUserId(),
                    title,
                    agentType,
                    context.getRequest().getOutputStyle(),
                    context.getAgentProvider().getId()
            );

            log.debug("[外部执行器] 会话已准备 - sessionId: {}", context.getSessionId());

        } catch (Exception e) {
            log.error("[外部执行器] 准备会话失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[外部执行器] 跳过用户消息保存 - 未登录或无sessionId");
            return;
        }

        try {
            chatHistoryService.saveMessage(
                    context.getSessionId(),
                    "user",
                    context.getOriginalQuery(),
                    null
            );

            log.debug("[外部执行器] 用户消息已保存 - sessionId: {}", context.getSessionId());

        } catch (Exception e) {
            log.error("[外部执行器] 保存用户消息失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 获取历史消息
     * 用于支持多轮对话
     *
     * @return 历史消息列表（ChatMessage格式）
     */
    private List<ChatMessage> getHistoryMessages(SessionContext context) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[外部执行器] 跳过历史加载 - 未登录或无sessionId");
            return new ArrayList<>();
        }

        try {
            // 获取历史消息（MessageVO格式）
            List<MessageVO> historyMessages = chatHistoryService.getSessionMessages(
                    context.getSessionId(),
                    context.getUserId()
            );

            // 转换为ChatMessage格式
            List<ChatMessage> history = historyMessages.stream()
                    .map(msg -> {
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.setRole(msg.getRole());
                        chatMsg.setContent(msg.getContent());
                        chatMsg.setCreateTime(msg.getCreateTime());
                        return chatMsg;
                    })
                    .collect(Collectors.toList());

            log.info("[外部执行器] 历史消息已获取 - sessionId: {}, count: {}",
                    context.getSessionId(), history.size());

            return history;

        } catch (Exception e) {
            log.error("[外部执行器] 获取历史消息失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 调用外部平台
     * 使用适配器发送请求，并处理响应
     * 使用拦截器模式收集SSE数据并保存到数据库
     */
    private void callExternalPlatform(
            SessionContext context,
            List<ChatMessage> history,
            AgentProvider provider) {

        log.info("[外部执行器] 调用外部平台 - platformType: {}, providerId: {}, providerName: {}",
                provider.getProviderType(),
                provider.getId(),
                provider.getProviderName());

        // 查询会话以获取externalSessionId
        ChatSession session = chatHistoryService.getSessionBySessionId(context.getSessionId());

        // 获取适配器
        AgentAdapter adapter = adapterFactory.getAdapter(provider.getProviderType());

        log.debug("[外部执行器] 适配器已获取 - adapterType: {}", adapter.getClass().getSimpleName());

        // 创建数据收集器
        ConversationDataCollector dataCollector = new ConversationDataCollector();

        // 获取目标emitter（来自context）
        SseEmitter targetEmitter = context.getEmitter();

        // 创建拦截器emitter，用于收集数据
//        InterceptableSseEmitter interceptorEmitter = new InterceptableSseEmitter(
//                30 * 60 * 1000L,
//                data -> {
//                    try {
//                        // 拦截到的数据：收集 + 转发
//                        String content = extractContentFromData(data);
//                        if (content != null && !content.isEmpty()) {
//                            dataCollector.collectRawContent(content);
//                        }
//
//                        // 转发给前端
//                        targetEmitter.send(data);
//
//                        log.debug("[外部执行器] SSE数据已拦截并转发 - sessionId: {}", context.getSessionId());
//                    } catch (Exception e) {
//                        log.error("[外部执行器] 拦截SSE数据失败 - sessionId: {}", context.getSessionId(), e);
//                    }
//                }
//        );
//
//        // 注册完成事件：保存AI回复
//        interceptorEmitter.onCompletion(() -> {
//            log.info("[外部执行器] SSE流完成，开始保存AI回复 - sessionId: {}", context.getSessionId());
//            saveAssistantReply(context, dataCollector);
//        });
//
//        // 注册错误事件：转发错误
//        interceptorEmitter.onError((ex) -> {
//            log.error("[外部执行器] SSE流错误 - sessionId: {}", context.getSessionId(), ex);
//            try {
//                targetEmitter.completeWithError(ex);
//            } catch (Exception e) {
//                log.error("[外部执行器] 转发错误失败", e);
//            }
//        });

        // 发送请求（使用拦截器emitter）
        AgentAdapter.ChatResponse response = adapter.sendChatRequest(
                context.getSessionId(),
                context.getOriginalQuery(),
                history,
                session != null ? session.getExternalSessionId() : null,
                targetEmitter,
                provider// 传入拦截器emitter
        );

        log.debug("[外部执行器] 请求已发送 - sessionId: {}", context.getSessionId());

        // 保存外部会话ID（首次对话时）
        if (session != null && session.getExternalSessionId() == null &&
                response.getExternalSessionId() != null) {

            chatHistoryService.updateExternalSessionId(
                    context.getSessionId(), response.getExternalSessionId());

            log.info("[外部执行器] 外部会话ID已保存 - sessionId: {}, externalSessionId: {}",
                    context.getSessionId(), response.getExternalSessionId());
        }

        log.info("[外部执行器] 执行完成 - requestId: {}, platformType: {}",
                context.getRequestId(), provider.getProviderType());
    }

    /**
     * 保存AI回复到数据库
     * 从ConversationDataCollector中提取收集的原始内容
     */
    private void saveAssistantReply(SessionContext context, ConversationDataCollector dataCollector) {
        if (!context.isLoggedIn() || context.getSessionId() == null) {
            log.debug("[外部执行器] 跳过保存AI回复 - 未登录或无sessionId");
            return;
        }

        try {
            // 提取完整的AI回复内容
            String fullContent = dataCollector.getRawContentAsString();

            if (fullContent == null || fullContent.isEmpty()) {
                log.warn("[外部执行器] AI回复内容为空，跳过保存 - sessionId: {}", context.getSessionId());
                return;
            }

            // 保存AI回复
            chatHistoryService.saveMessage(
                    context.getSessionId(),
                    "assistant",
                    fullContent,
                    null
            );

            log.info("[外部执行器] AI回复已保存 - sessionId: {}, contentLength: {}",
                    context.getSessionId(), fullContent.length());

        } catch (Exception e) {
            log.error("[外部执行器] 保存AI回复失败 - sessionId: {}, error: {}",
                    context.getSessionId(), e.getMessage(), e);
            // 不抛出异常，允许继续执行
        }
    }

    /**
     * 从SSE数据中提取内容
     * 支持多种SSE数据格式
     */
    private String extractContentFromData(Object data) {
        if (data == null) {
            return null;
        }

        String dataStr = data.toString();

        // 处理SseEmitter.event()格式
        if (dataStr.contains("data:")) {
            // 简单提取data:后的内容
            int dataIndex = dataStr.indexOf("data:");
            if (dataIndex != -1) {
                String content = dataStr.substring(dataIndex + 5).trim();
                // 移除可能的引号
                if (content.startsWith("\"") && content.endsWith("\"")) {
                    content = content.substring(1, content.length() - 1);
                }
                return content;
            }
        }

        // 直接返回字符串内容
        return dataStr;
    }
}
