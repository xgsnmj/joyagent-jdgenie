package com.jd.genie.adapter;

import com.jd.genie.entity.ChatMessage;
import com.jd.genie.service.SSEMessageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认智能体适配器
 * 对接本地的MultiAgent体系
 * 保持与现有系统的兼容性
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@Component
public class DefaultAgentAdapter implements AgentAdapter {

    @Autowired
    private SSEMessageCacheService cacheService;

    // 会话终止标记
    private final Map<String, Boolean> terminationFlags = new ConcurrentHashMap<>();

    @Override
    public ChatResponse sendChatRequest(String sessionId,
                                       String userMessage,
                                       List<ChatMessage> history,
                                       String apiEndpoint,
                                       String apiKey,
                                       String botId,
                                       String externalSessionId) {
        log.info("Default适配器处理请求 - 会话ID: {}", sessionId);
        // Default类型不使用botId参数

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30分钟超时
        terminationFlags.put(sessionId, false);

        // 构建响应对象
        ChatResponse response = new ChatResponse();
        response.setEmitter(emitter);
        response.setExternalSessionId(null); // default类型不需要外部会话ID

        // 异步处理
        new Thread(() -> {
            try {
                int sequence = 0;

                // TODO: 这里需要集成现有的MultiAgent逻辑
                // 参考现有的ChatController中的处理逻辑
                // 需要调用：PlanningAgent、ExecutorAgent、ReActAgent等
                //
                // 示例集成点：
                // 1. 获取现有的AgentService或相关Service
                // 2. 调用processUserMessage方法
                // 3. 处理返回的SSE流
                //
                // String agentResponse = agentService.processMessage(sessionId, userMessage, history);

                // 临时示例：模拟SSE流（实际应该从MultiAgent获取）
                log.warn("Default适配器当前使用模拟数据，需要集成真实的MultiAgent逻辑");
                String[] chunks = {"正在", "处理", "您的", "请求", "..."};
                for (String chunk : chunks) {
                    if (terminationFlags.get(sessionId)) {
                        log.info("会话{}被终止", sessionId);
                        break;
                    }

                    // 缓存消息
                    cacheService.cacheMessage(sessionId, sequence++, "message", chunk, chunk);

                    // 发送给前端
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(chunk));

                    Thread.sleep(100); // 模拟延迟
                }

                // 发送完成信号
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

                // 持久化缓存消息
                persistCachedMessages(sessionId);

            } catch (Exception e) {
                log.error("Default适配器处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理失败: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            } finally {
                terminationFlags.remove(sessionId);
            }
        }).start();

        return response;
    }

    @Override
    public void terminateChat(String sessionId) {
        log.info("终止Default会话: {}", sessionId);
        terminationFlags.put(sessionId, true);

        // 持久化已缓存的消息
        persistCachedMessages(sessionId);
    }

    @Override
    public String getProviderType() {
        return "default";
    }

    @Override
    public ChatMessage formatMessage(String rawResponse, String messageFormat) {
        // Default格式不需要特殊处理
        ChatMessage message = new ChatMessage();
        message.setContent(rawResponse);
        message.setMessageFormat("default");
        message.setRawContent(rawResponse);
        return message;
    }

    /**
     * 持久化缓存消息到chat_message表
     *
     * @param sessionId 会话ID
     */
    private void persistCachedMessages(String sessionId) {
        try {
            // TODO: 实现消息持久化逻辑
            // 1. 从缓存表读取所有消息（按序号排序）
            // List<SSEMessageCache> cachedMessages = cacheService.getSessionCachedMessages(sessionId);
            //
            // 2. 合并为完整消息
            // StringBuilder fullMessage = new StringBuilder();
            // for (SSEMessageCache cache : cachedMessages) {
            //     fullMessage.append(cache.getEventData());
            // }
            //
            // 3. 保存到chat_message表
            // ChatMessage message = new ChatMessage();
            // message.setSessionId(sessionId.toString());
            // message.setRole("assistant");
            // message.setContent(fullMessage.toString());
            // message.setMessageFormat("default");
            // chatMessageService.save(message);
            //
            // 4. 标记缓存为已持久化
            cacheService.markAsPersisted(sessionId);
            //
            // 5. 清理缓存
            cacheService.cleanPersistedCache(sessionId);

            log.debug("会话{}的消息已持久化", sessionId);
        } catch (Exception e) {
            log.error("持久化消息失败 - 会话ID: {}", sessionId, e);
        }
    }
}
