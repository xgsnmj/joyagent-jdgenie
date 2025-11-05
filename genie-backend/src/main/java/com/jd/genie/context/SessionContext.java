package com.jd.genie.context;

import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.model.req.AgentRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会话上下文对象
 * 封装单次会话请求的所有上下文信息
 *
 * 设计目的：
 * 1. 避免方法参数过多
 * 2. 便于在不同方法间传递上下文
 * 3. 提供统一的数据访问接口
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Data
@Builder
public class SessionContext {

    // ========== 基础信息 ==========

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 请求ID（用于日志追踪）
     */
    private String requestId;

    /**
     * 用户ID（可能为null，表示未登录用户）
     */
    private Long userId;

    /**
     * 原始请求对象
     */
    private AgentRequest request;

    // ========== Query相关 ==========

    /**
     * 原始用户查询（用于保存到数据库）
     */
    private String originalQuery;

    /**
     * 处理后的查询（拼接了输出样式提示词）
     */
    private String processedQuery;

    // ========== 智能体相关 ==========

    /**
     * 智能体配置（可能为null，表示使用系统默认）
     */
    private AgentProvider agentProvider;

    /**
     * 智能体执行上下文
     */
    private AgentContext agentContext;

    // ========== SSE相关 ==========

    /**
     * SSE发射器
     */
    private SseEmitter emitter;

    // ========== 便捷方法 ==========

    /**
     * 判断是否为已登录用户
     *
     * @return true表示已登录，false表示未登录
     */
    public boolean isLoggedIn() {
        return userId != null;
    }

    /**
     * 判断是否为外部平台
     *
     * @return true表示外部平台（Coze/融汇等），false表示默认平台
     */
    public boolean isExternalPlatform() {
        return agentProvider != null && !"default".equals(agentProvider.getProviderType());
    }

    /**
     * 判断是否为默认平台
     *
     * @return true表示默认平台，false表示外部平台
     */
    public boolean isDefaultPlatform() {
        return !isExternalPlatform();
    }

    /**
     * 获取平台类型
     *
     * @return 平台类型标识（default/coze/ronghui等）
     */
    public String getPlatformType() {
        if (agentProvider == null) {
            return "default";
        }
        return agentProvider.getProviderType();
    }
}
