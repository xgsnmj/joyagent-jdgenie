package com.jd.genie.service;

import com.jd.genie.model.req.AgentRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会话编排服务接口
 * 负责会话请求的整体流程编排
 *
 * 核心职责：
 * 1. 构建会话上下文
 * 2. 选择智能体配置
 * 3. 路由到对应的执行器
 * 4. 协调整个会话处理流程
 *
 * 设计原则：
 * - 单一职责：只负责编排，不负责具体执行
 * - 开闭原则：新增平台不修改此服务
 * - 依赖倒置：依赖AgentExecutor接口，不依赖具体实现
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
public interface SessionOrchestrationService {

    /**
     * 编排会话请求
     * 这是整个会话处理的入口方法
     *
     * 主要流程：
     * 1. 构建会话上下文（SessionContext）
     * 2. 选择智能体配置（AgentProvider）
     * 3. 获取对应的执行器（AgentExecutor）
     * 4. 调用执行器执行
     *
     * @param request 请求对象，包含用户查询、会话ID、智能体配置等信息
     * @param userId 用户ID（可能为null，表示未登录用户）
     * @param emitter SSE发射器，用于流式返回响应
     */
    void orchestrate(AgentRequest request, Long userId, SseEmitter emitter);
}
