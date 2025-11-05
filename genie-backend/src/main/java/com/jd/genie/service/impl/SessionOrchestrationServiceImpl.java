package com.jd.genie.service.impl;

import com.jd.genie.context.SessionContext;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.executor.AgentExecutor;
import com.jd.genie.executor.AgentExecutorFactory;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.service.AgentProviderService;
import com.jd.genie.service.SessionContextBuilder;
import com.jd.genie.service.SessionOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会话编排服务实现
 *
 * 核心架构设计：
 * 1. 准备阶段：构建SessionContext，准备所有必需的上下文信息
 * 2. 选择阶段：确定使用哪个智能体配置
 * 3. 路由阶段：根据智能体类型路由到不同的执行器
 * 4. 执行阶段：调用执行器进行实际处理
 *
 * 职责边界：
 * - 负责：流程编排、智能体选择、执行器路由
 * - 不负责：具体执行逻辑（由执行器完成）
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Service
public class SessionOrchestrationServiceImpl implements SessionOrchestrationService {

    @Autowired
    private SessionContextBuilder contextBuilder;

    @Autowired
    private AgentProviderService agentProviderService;

    @Autowired
    private AgentExecutorFactory executorFactory;

    @Override
    public void orchestrate(AgentRequest request, Long userId, SseEmitter emitter) {
        log.info("[编排服务] 开始编排会话 - requestId: {}, userId: {}, sessionId: {}",
                request.getRequestId(), userId, request.getSessionId());

        try {
            // ========== 阶段1：构建会话上下文 ==========
            SessionContext context = contextBuilder.build(request, userId, emitter);
            log.debug("[编排服务] 会话上下文已构建 - sessionId: {}", context.getSessionId());

            // ========== 阶段2：选择智能体配置 ==========
            AgentProvider provider = selectAgentProvider(request.getAgentProviderId(), userId);
            context.setAgentProvider(provider);

            log.info("[编排服务] 智能体已选择 - 平台类型: {}, 配置ID: {}, 配置名称: {}",
                    context.getPlatformType(),
                    provider != null ? provider.getId() : null,
                    provider != null ? provider.getProviderName() : "系统默认");

            // ========== 阶段3：获取执行器 ==========
            AgentExecutor executor = executorFactory.getExecutor(context.getPlatformType());
            log.debug("[编排服务] 执行器已获取 - 类型: {}", executor.getClass().getSimpleName());

            // ========== 阶段4：执行 ==========
            executor.execute(context);

            log.info("[编排服务] 会话编排完成 - requestId: {}", request.getRequestId());

        } catch (Exception e) {
            log.error("[编排服务] 会话编排失败 - requestId: {}, error: {}",
                    request.getRequestId(), e.getMessage(), e);
            handleError(emitter, e);
        }
    }

    /**
     * 选择智能体配置
     *
     * 选择逻辑：
     * 1. 如果指定了agentProviderId，使用指定的智能体（需验证权限）
     * 2. 如果未指定，使用用户的默认智能体
     * 3. 如果用户未登录，返回null（使用系统默认）
     *
     * @param agentProviderId 指定的智能体配置ID（可能为null）
     * @param userId 用户ID（可能为null）
     * @return 智能体配置（可能为null）
     */
    private AgentProvider selectAgentProvider(Long agentProviderId, Long userId) {
        // 未登录用户使用系统默认
        if (userId == null) {
            log.debug("[编排服务] 未登录用户，使用系统默认智能体");
            return null;
        }

        // 如果指定了智能体ID，使用指定的智能体
        if (agentProviderId != null) {
            log.debug("[编排服务] 使用指定的智能体 - providerId: {}", agentProviderId);
            AgentProvider provider = agentProviderService.getById(agentProviderId);
            validateProvider(provider, agentProviderId, userId);
            return provider;
        }

        // 未指定，使用用户的默认智能体
        log.debug("[编排服务] 使用用户默认智能体 - userId: {}", userId);
        return agentProviderService.getUserDefaultProvider(userId);
    }

    /**
     * 验证智能体配置
     *
     * 验证规则：
     * 1. 智能体配置必须存在
     * 2. 用户必须有权限使用该智能体（即该智能体属于该用户）
     *
     * @param provider 智能体配置
     * @param providerId 配置ID
     * @param userId 用户ID
     * @throws IllegalArgumentException 如果智能体配置不存在
     * @throws SecurityException 如果用户无权限使用该智能体
     */
    private void validateProvider(AgentProvider provider, Long providerId, Long userId) {
        if (provider == null) {
            log.error("[编排服务] 智能体配置不存在 - providerId: {}", providerId);
            throw new IllegalArgumentException("智能体配置不存在: " + providerId);
        }

        if (!provider.getUserId().equals(userId)) {
            log.error("[编排服务] 用户无权使用该智能体 - providerId: {}, userId: {}, ownerUserId: {}",
                    providerId, userId, provider.getUserId());
            throw new SecurityException("无权使用该智能体配置");
        }

        log.debug("[编排服务] 智能体配置验证通过 - providerId: {}, name: {}",
                providerId, provider.getProviderName());
    }

    /**
     * 处理错误
     *
     * 错误处理策略：
     * 1. 记录详细的错误日志
     * 2. 通过SSE向前端发送错误消息
     * 3. 完成SSE连接（带错误状态）
     *
     * @param emitter SSE发射器
     * @param e 异常对象
     */
    private void handleError(SseEmitter emitter, Exception e) {
        try {
            // 发送错误消息给前端
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理请求失败: " + e.getMessage()));

            // 完成连接（带错误）
            emitter.completeWithError(e);

        } catch (Exception ex) {
            log.error("[编排服务] 发送错误消息失败", ex);
        }
    }
}
