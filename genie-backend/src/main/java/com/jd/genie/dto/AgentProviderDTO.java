package com.jd.genie.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体服务商DTO
 * 用于前端展示智能体配置信息
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Data
public class AgentProviderDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 平台类型：default/coze/ronghui
     */
    private String providerType;

    /**
     * 应用名称（最多10个字）
     */
    private String providerName;

    /**
     * API请求地址
     */
    private String apiEndpoint;

    /**
     * API密钥（脱敏后）
     * 前端展示时只显示前4位和后4位
     */
    private String apiKey;

    /**
     * Coze平台的Bot ID
     * 仅coze类型必填
     */
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
     * 额外配置
     */
    private String extraConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 获取脱敏后的API密钥
     * 只显示前4位和后4位，中间用****替代
     *
     * @return 脱敏后的API密钥
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
