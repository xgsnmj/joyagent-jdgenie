package com.jd.genie.sse.listener;

import com.jd.genie.sse.ManagedSseEmitter;
import com.jd.genie.sse.SseEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 平台终止器监听器
 * 监听SSE断开事件，自动调用外部智能体平台的中断接口
 *
 * 功能说明：
 * 当用户主动关闭浏览器、网络中断或连接超时时，前端SSE连接会断开。
 * 此时需要通知外部平台（Coze、融汇、通义等）停止生成，避免资源浪费。
 *
 * 支持的平台：
 * - Coze: 调用Cancel Chat API
 * - 融汇: 调用中断会话接口
 * - 通义点金: 调用停止生成接口
 * - Dify: 调用停止响应接口
 *
 * 触发时机：
 * - DISCONNECTED事件（由TIMEOUT/ERROR/COMPLETION触发）
 * - 断开原因包括：timeout（超时）、error（错误）、completed（正常完成）
 *
 * 使用方式：
 * <pre>
 * // 方式1：全局注册（推荐）
 * &#64;Autowired
 * private SseSessionManager sessionManager;
 * &#64;Autowired
 * private PlatformTerminator platformTerminator;
 *
 * &#64;PostConstruct
 * public void init() {
 *     sessionManager.registerGlobalListener(platformTerminator);
 * }
 *
 * // 方式2：单独注册
 * ManagedSseEmitter emitter = new ManagedSseEmitter(...);
 * emitter.addEventListener(platformTerminator);
 * </pre>
 *
 * 注意事项：
 * 1. 异步执行：避免阻塞SSE连接关闭流程
 * 2. 异常处理：平台API调用失败不应影响连接关闭
 * 3. 幂等性：确保重复调用不会造成问题
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
@Component
public class PlatformTerminator implements SseEventListener {

    // TODO: 注入依赖
    // @Autowired
    // private ChatSessionService chatSessionService;
    // @Autowired
    // private AgentAdapterFactory agentAdapterFactory;

    /**
     * 处理SSE事件
     * 仅关注DISCONNECTED事件
     *
     * @param eventType 事件类型
     * @param emitter   SSE发射器
     * @param eventData 事件数据（断开原因：timeout/error/completed）
     */
    @Override
    public void onEvent(SseEventType eventType, ManagedSseEmitter emitter, Object eventData) {
        // 仅处理断开事件
        if (eventType != SseEventType.DISCONNECTED) {
            return;
        }

        String sessionId = emitter.getSessionId();
        String requestId = emitter.getRequestId();
        String disconnectReason = eventData != null ? eventData.toString() : "unknown";

        log.info("检测到SSE断开: sessionId={}, requestId={}, reason={}",
                sessionId, requestId, disconnectReason);

        // 异步调用平台中断接口
        try {
            terminatePlatformSession(sessionId, requestId, disconnectReason);
        } catch (Exception e) {
            log.error("调用平台中断接口失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            // 不抛出异常，避免影响连接关闭流程
        }
    }

    /**
     * 调用外部平台中断接口
     *
     * @param sessionId       会话ID
     * @param requestId       请求ID
     * @param disconnectReason 断开原因
     */
    private void terminatePlatformSession(String sessionId, String requestId, String disconnectReason) {
        log.debug("准备终止外部平台会话: sessionId={}, reason={}", sessionId, disconnectReason);

        // TODO: 实现平台中断逻辑
        // 示例实现：
        //
        // 1. 查询会话信息
        // ChatSession session = chatSessionService.getSessionById(sessionId);
        // if (session == null) {
        //     log.warn("会话不存在，无法终止: sessionId={}", sessionId);
        //     return;
        // }
        //
        // 2. 查询智能体配置
        // List<SessionAgent> agents = sessionAgentService.getAgentsBySessionId(sessionId);
        // if (agents.isEmpty()) {
        //     log.debug("会话无关联智能体，无需终止: sessionId={}", sessionId);
        //     return;
        // }
        //
        // 3. 调用每个平台的中断接口
        // for (SessionAgent sessionAgent : agents) {
        //     try {
        //         AgentProvider provider = agentProviderService.getById(sessionAgent.getAgentId());
        //         if (provider == null) continue;
        //
        //         // 获取平台适配器
        //         AgentAdapter adapter = agentAdapterFactory.getAdapter(provider.getProviderType());
        //
        //         // 调用中断方法（需要在AgentAdapter接口中添加此方法）
        //         if (adapter instanceof InterruptibleAdapter) {
        //             InterruptibleAdapter interruptible = (InterruptibleAdapter) adapter;
        //             interruptible.interrupt(sessionId, provider);
        //             log.info("已调用平台中断接口: platform={}, sessionId={}", provider.getProviderType(), sessionId);
        //         }
        //     } catch (Exception e) {
        //         log.error("调用平台中断接口异常: agentId={}, error={}", sessionAgent.getAgentId(), e.getMessage());
        //     }
        // }

        // 临时实现：仅记录日志
        logTerminationAttempt(sessionId, requestId, disconnectReason);
    }

    /**
     * 临时实现：记录终止尝试
     * TODO: 替换为实际的平台API调用
     *
     * @param sessionId       会话ID
     * @param requestId       请求ID
     * @param disconnectReason 断开原因
     */
    private void logTerminationAttempt(String sessionId, String requestId, String disconnectReason) {
        log.info("🛑 平台终止请求: sessionId={}, requestId={}, reason={}",
                sessionId, requestId, disconnectReason);

        // 根据断开原因记录不同的日志
        switch (disconnectReason) {
            case "timeout":
                log.warn("  ⏱️ 连接超时，建议检查平台响应时间");
                break;
            case "error":
                log.error("  ❌ 连接错误，可能是网络问题或客户端异常");
                break;
            case "completed":
                log.info("  ✅ 正常完成，无需额外处理");
                break;
            default:
                log.warn("  ⚠️ 未知断开原因: {}", disconnectReason);
        }
    }

    /**
     * 仅关注DISCONNECTED事件
     *
     * @param eventType 事件类型
     * @return true表示关注
     */
    @Override
    public boolean isInterestedIn(SseEventType eventType) {
        return eventType == SseEventType.DISCONNECTED;
    }

    @Override
    public String getListenerName() {
        return "PlatformTerminator";
    }
}
