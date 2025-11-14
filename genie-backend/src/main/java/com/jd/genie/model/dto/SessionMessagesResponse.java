package com.jd.genie.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 会话消息响应DTO
 * 包含会话的智能体信息和消息列表
 * 用于前端正确解析不同智能体类型的消息格式
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessagesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 智能体配置ID
     * 用于标识该会话使用的智能体平台配置
     */
    private Long agentProviderId;

    /**
     * 智能体工作类型
     * 例如：plansolve（规划解决模式）、react（React模式）
     * 标识智能体的工作模式
     */
    private String agentType;

    /**
     * 智能体平台类型
     * 例如：default/coze/ronghui/tongyi/dify
     * 标识该会话使用的智能体平台（从agent_provider表的provider_type字段获取）
     * 前端根据此字段选择对应的平台适配器
     */
    private String agentProviderType;

    /**
     * 外部会话ID
     * 外部智能体平台返回的会话标识（用于多轮对话）
     */
    private String externalSessionId;

    /**
     * 消息列表
     * 按时间正序排列的会话消息
     */
    private List<MessageVO> messages;
}
