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
     * 平台类型：default/coze/ronghui/tongyi/dify
     * - default: 本地MultiAgent体系
     * - coze: Coze智能体平台
     * - ronghui: 融汇智能体平台
     * - tongyi: 通义点金智能体平台
     * - dify: Dify开源LLM应用平台
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
     * 通义点金的工作空间ID
     * 仅tongyi类型需要，其他类型为空
     */
    private String workspaceId;

    /**
     * 租户ID
     * 仅ronghui类型使用，其他类型为空
     */
    private String tenantId;

    /**
     * 登录用户ID
     * 仅ronghui类型使用，其他类型为空
     */
    private String loginUserId;

    /**
     * 登录部门ID
     * 仅ronghui类型使用，其他类型为空
     */
    private String loginDeptId;

    /**
     * 登录用户名
     * 仅ronghui类型使用，其他类型为空
     */
    private String loginUsername;

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
     * 智能体简介（最多500字符）
     * 用于智能体社区展示
     */
    private String description;

    /**
     * 智能体图标URL
     * 用于智能体社区卡片展示
     */
    private String icon;

    /**
     * 创建者用户ID
     * 用于"我的智能体"筛选和权限控制
     */
    private Long creatorId;

    /**
     * 是否公开
     * 1-公开（所有用户可见） 0-私有（仅创建者可见）
     */
    private Boolean isPublic;

    /**
     * 使用次数统计
     * 每次用户选择该智能体发起对话时+1
     */
    private Integer usageCount;

    /**
     * 分类标签（部门）
     * 场外衍生品部、风险管理部、投资银行部、科技研发中心等
     */
    private String category;

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
