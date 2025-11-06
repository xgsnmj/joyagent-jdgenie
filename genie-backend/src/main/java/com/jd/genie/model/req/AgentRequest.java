package com.jd.genie.model.req;

import com.jd.genie.model.dto.FileInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assistant请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    private String requestId;
    private String sessionId; // 会话ID，用于会话历史跟踪
    private Long agentProviderId; // 智能体配置ID，用于指定使用的智能体平台
    private String erp;
    private String query;
    private Integer agentType;
    private String basePrompt;//基础提示词
    private String sopPrompt;//标准步骤提示词
    private Boolean isStream;//是否流式输出
    private List<Message> messages;
    private String outputStyle; // 交付物产出格式：html(网页模式）， docs(文档模式）， table(表格模式）

    /**
     * 历史对话消息列表（用于多轮对话记忆）
     * 按时间正序排列，最早的消息在前
     */
    private List<HistoryMessage> historyMessages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
        private String commandCode;
        private List<FileInformation> uploadFile;
        private List<FileInformation> files;

    }

    /**
     * 历史消息DTO
     * 用于多轮对话记忆
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HistoryMessage {
        /**
         * 消息角色（user/assistant）
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 创建时间
         */
        private String createTime;
    }
}
