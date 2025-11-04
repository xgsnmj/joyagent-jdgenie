package com.jd.genie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体服务商配置实体
 * 用于存储用户配置的智能体服务商信息，支持对接外部智能体平台
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Data
@TableName("agent_provider")
public class AgentProvider {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 平台类型：default/coze/ronghui
     * - default: 本地MultiAgent体系
     * - coze: Coze智能体平台
     * - ronghui: 融汇（阿里点金）智能体平台
     */
    private String providerType;

    /**
     * 应用名称（最多10个字）
     */
    private String providerName;

    /**
     * API请求地址
     * default类型为空
     */
    private String apiEndpoint;

    /**
     * API密钥（明文存储）
     * default类型为空
     */
    private String apiKey;

    /**
     * Coze平台的Bot ID
     * 仅coze类型必填，其他类型为空
     */
    private String botId;

    /**
     * 是否为该用户的默认智能体
     * 0-否 1-是
     * 每个用户只能有一个默认智能体（通过触发器保证）
     */
    private Boolean isDefault;

    /**
     * 状态
     * 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 额外配置（JSON格式）
     * 用于存储平台特有参数
     */
    private String extraConfig;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
