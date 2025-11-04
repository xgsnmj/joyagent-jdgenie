package com.jd.genie.adapter;

import com.jd.genie.entity.ChatMessage;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 智能体适配器接口
 * 定义与不同智能体平台交互的统一接口
 * 实现适配器模式，支持多平台智能体对接
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
public interface AgentAdapter {

    /**
     * 聊天响应对象
     * 包含SSE发射器和外部会话ID
     */
    @Data
    class ChatResponse {
        /**
         * SSE发射器，用于流式返回响应
         */
        private SseEmitter emitter;

        /**
         * 外部平台返回的会话ID
         * 用于多轮对话时传递给外部平台
         */
        private String externalSessionId;
    }

    /**
     * 发送聊天请求到智能体平台
     *
     * @param sessionId 本地会话ID（字符串格式）
     * @param userMessage 用户消息
     * @param history 历史消息列表（支持多轮对话）
     * @param apiEndpoint API端点地址
     * @param apiKey API密钥
     * @param botId Bot ID（Coze平台必填，其他平台可为null）
     * @param externalSessionId 外部会话ID（多轮对话时传入，首次对话为null）
     * @return 聊天响应（包含SSE发射器和外部会话ID）
     */
    ChatResponse sendChatRequest(String sessionId,
                                 String userMessage,
                                 List<ChatMessage> history,
                                 String apiEndpoint,
                                 String apiKey,
                                 String botId,
                                 String externalSessionId);

    /**
     * 终止对话
     * 用户手动终止或发生错误时调用
     *
     * @param sessionId 会话ID（字符串格式）
     */
    void terminateChat(String sessionId);

    /**
     * 获取支持的平台类型
     *
     * @return 平台类型标识（default/coze/ronghui）
     */
    String getProviderType();

    /**
     * 格式化消息为标准格式
     * 用于将平台特定格式转换为统一的ChatMessage格式
     *
     * @param rawResponse 原始响应
     * @param messageFormat 消息格式标识
     * @return 标准格式消息
     */
    ChatMessage formatMessage(String rawResponse, String messageFormat);
}
