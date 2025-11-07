package com.jd.genie.adapter;

import com.jd.genie.entity.AgentProvider;
import com.jd.genie.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
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

    // 会话终止标记
    private final Map<String, Boolean> terminationFlags = new ConcurrentHashMap<>();



    @Override
    public void terminateChat(String sessionId) {
        log.info("终止Default会话: {}", sessionId);
        terminationFlags.put(sessionId, true);
    }

    @Override
    public ChatResponse sendChatRequest(String sessionId, String userMessage, List<ChatMessage> history, String externalSessionId, SseEmitter customEmitter, AgentProvider provider) {
        return null;
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

}
