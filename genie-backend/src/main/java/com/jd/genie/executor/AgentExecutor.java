package com.jd.genie.executor;

import com.jd.genie.context.SessionContext;

/**
 * 智能体执行器接口
 * 定义不同平台智能体的执行规范
 *
 * 设计思想：
 * - 策略模式：不同平台实现不同的执行策略
 * - 单一职责：每个执行器只负责一种平台的执行逻辑
 * - 开闭原则：新增平台只需添加新的执行器实现
 *
 * 实现类：
 * - DefaultAgentExecutor：处理系统默认智能体
 * - ExternalAgentExecutor：处理外部平台智能体（Coze/融汇等）
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
public interface AgentExecutor {

    /**
     * 执行智能体
     *
     * 核心流程：
     * 1. 准备会话（创建或获取会话）
     * 2. 加载历史消息（如果需要）
     * 3. 保存用户消息
     * 4. 调用智能体API
     * 5. 保存AI回复
     *
     * @param context 会话上下文，包含请求、用户信息、智能体配置等
     */
    void execute(SessionContext context);

    /**
     * 获取支持的平台类型
     *
     * 用于执行器工厂进行路由选择
     *
     * @return 平台类型标识
     *         - "default"：系统默认智能体
     *         - "external"：外部平台智能体（通用）
     *         - "coze"：Coze平台（如需要单独实现）
     *         - "ronghui"：融汇平台（如需要单独实现）
     */
    String getPlatformType();
}
