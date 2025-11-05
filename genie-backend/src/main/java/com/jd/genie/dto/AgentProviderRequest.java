package com.jd.genie.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * 智能体服务商创建/更新请求
 * 用于接收前端创建或更新智能体配置的请求
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Data
public class AgentProviderRequest {

    /**
     * 平台类型：default/coze/ronghui
     */
    @NotBlank(message = "平台类型不能为空")
    @Pattern(regexp = "^(default|coze|ronghui)$", message = "平台类型只能是default/coze/ronghui")
    private String providerType;

    /**
     * 应用名称（最多10个字）
     */
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 10, message = "应用名称不能超过10个字")
    private String providerName;

    /**
     * API请求地址
     */
    @Size(max = 500, message = "API地址不能超过500个字符")
    private String apiEndpoint;

    /**
     * API密钥
     */
    @Size(max = 500, message = "API密钥不能超过500个字符")
    private String apiKey;

    /**
     * Coze平台的Bot ID
     * 仅coze类型必填（在业务层验证）
     */
    @Size(max = 100, message = "Bot ID不能超过100个字符")
    private String botId;

    /**
     * 是否为该用户的默认智能体
     */
    private Boolean isDefault;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 额外配置（JSON格式）
     */
    private String extraConfig;

    /**
     * 智能体简介（最多500字符）
     * 用于智能体社区展示
     */
    @Size(max = 500, message = "智能体简介不能超过500个字符")
    private String description;

    /**
     * 智能体图标URL
     * 用于智能体社区卡片展示
     */
    private String icon;

    /**
     * 分类标签
     * 教育、新零售、消费、金融等
     */
    private String category;
}
