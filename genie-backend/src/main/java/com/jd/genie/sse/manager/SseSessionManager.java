package com.jd.genie.sse.manager;

import com.jd.genie.sse.ManagedSseEmitter;
import com.jd.genie.sse.SseConnectionState;
import com.jd.genie.sse.hook.SseMessageHook;
import com.jd.genie.sse.listener.MessagePersistenceListener;
import com.jd.genie.sse.listener.SseEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE会话管理器
 * 统一管理所有活跃的SSE连接
 *
 * 核心功能：
 * 1. 会话注册：创建和注册新的ManagedSseEmitter
 * 2. 会话查询：根据sessionId或requestId查找连接
 * 3. 会话移除：清理已关闭的连接
 * 4. 全局监听器/钩子：为所有连接注册公共组件
 * 5. 统计信息：连接数、消息数等
 *
 * 线程安全：
 * - 使用ConcurrentHashMap保证并发安全
 * - 所有公共方法都是线程安全的
 *
 * 使用场景：
 * - Controller层创建SSE连接时注册
 * - 监控系统查询当前活跃连接
 * - 定时任务清理超时连接
 * - 全局配置监听器和钩子
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
@Component
public class SseSessionManager {

    @Autowired(required = false)
    private MessagePersistenceListener messagePersistenceListener;

    /**
     * 会话存储
     * Key: sessionId, Value: ManagedSseEmitter
     */
    private final ConcurrentHashMap<String, ManagedSseEmitter> sessions = new ConcurrentHashMap<>();

    /**
     * 请求ID到会话ID的映射
     * Key: requestId, Value: sessionId
     * 用于根据requestId快速查找会话
     */
    private final ConcurrentHashMap<String, String> requestToSession = new ConcurrentHashMap<>();

    /**
     * 全局事件监听器
     * 新创建的ManagedSseEmitter会自动注册这些监听器
     */
    private final List<SseEventListener> globalListeners = new ArrayList<>();

    /**
     * 全局消息钩子
     * 新创建的ManagedSseEmitter会自动注册这些钩子
     */
    private final List<SseMessageHook> globalHooks = new ArrayList<>();

    /**
     * 初始化方法
     * 注册全局监听器
     */
    @PostConstruct
    public void init() {
        // 注册全局消息持久化监听器
        if (messagePersistenceListener != null) {
            registerGlobalListener(messagePersistenceListener);
            log.info("✅ 已注册全局消息持久化监听器");
        } else {
            log.warn("⚠️ MessagePersistenceListener未注入，消息持久化功能未启用");
        }
    }

    /**
     * 创建并注册SSE连接
     *
     * @param sessionId 会话ID
     * @param requestId 请求ID
     * @param timeout   超时时间（毫秒），null表示不超时
     * @return 创建的ManagedSseEmitter实例
     */
    public ManagedSseEmitter createSession(String sessionId, String requestId, Long timeout) {
        if (sessionId == null || requestId == null) {
            throw new IllegalArgumentException("sessionId and requestId cannot be null");
        }

        // 检查是否已存在
        if (sessions.containsKey(sessionId)) {
            log.warn("会话已存在，将移除旧连接: sessionId={}", sessionId);
            removeSession(sessionId);
        }

        // 创建新连接
        ManagedSseEmitter emitter = new ManagedSseEmitter(timeout, sessionId, requestId);

        // 注册全局监听器
        for (SseEventListener listener : globalListeners) {
            emitter.addEventListener(listener);
        }

        // 注册全局钩子
        for (SseMessageHook hook : globalHooks) {
            emitter.addMessageHook(hook);
        }

        // 自动清理：连接关闭时从管理器中移除
        emitter.addEventListener((eventType, em, data) -> {
            if (eventType == com.jd.genie.sse.SseEventType.COMPLETION) {
                removeSession(sessionId);
            }
        });

        // 存储会话
        sessions.put(sessionId, emitter);
        requestToSession.put(requestId, sessionId);

        log.info("SSE会话已创建: sessionId={}, requestId={}, 当前活跃连接数={}", sessionId, requestId, sessions.size());
        return emitter;
    }

    /**
     * 根据会话ID获取连接
     *
     * @param sessionId 会话ID
     * @return ManagedSseEmitter实例，不存在则返回null
     */
    public ManagedSseEmitter getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据请求ID获取连接
     *
     * @param requestId 请求ID
     * @return ManagedSseEmitter实例，不存在则返回null
     */
    public ManagedSseEmitter getSessionByRequestId(String requestId) {
        String sessionId = requestToSession.get(requestId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话ID
     * @return 被移除的ManagedSseEmitter，不存在则返回null
     */
    public ManagedSseEmitter removeSession(String sessionId) {
        ManagedSseEmitter removed = sessions.remove(sessionId);
        if (removed != null) {
            // 同时移除requestId映射
            requestToSession.remove(removed.getRequestId());
            log.info("SSE会话已移除: sessionId={}, 当前活跃连接数={}", sessionId, sessions.size());
        }
        return removed;
    }

    /**
     * 注册全局监听器
     * 对所有新创建的ManagedSseEmitter生效
     *
     * @param listener 监听器实例
     */
    public void registerGlobalListener(SseEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        globalListeners.add(listener);
        log.info("注册全局监听器: {}", listener.getListenerName());
    }

    /**
     * 注册全局钩子
     * 对所有新创建的ManagedSseEmitter生效
     *
     * @param hook 钩子实例
     */
    public void registerGlobalHook(SseMessageHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Hook cannot be null");
        }
        globalHooks.add(hook);
        globalHooks.sort(Comparator.comparingInt(SseMessageHook::getPriority));
        log.info("注册全局钩子: {}, priority={}", hook.getHookName(), hook.getPriority());
    }

    /**
     * 获取所有活跃会话ID
     *
     * @return 会话ID集合
     */
    public Set<String> getActiveSessionIds() {
        return new HashSet<>(sessions.keySet());
    }

    /**
     * 获取所有活跃连接
     *
     * @return ManagedSseEmitter列表
     */
    public List<ManagedSseEmitter> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 获取活跃连接数
     *
     * @return 连接数
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 清理所有已关闭的连接
     * 可用于定时任务
     *
     * @return 清理的连接数
     */
    public int cleanupClosedSessions() {
        int cleaned = 0;
        Iterator<Map.Entry<String, ManagedSseEmitter>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ManagedSseEmitter> entry = iterator.next();
            ManagedSseEmitter emitter = entry.getValue();
            if (emitter.getState().isTerminal()) {
                String sessionId = entry.getKey();
                iterator.remove();
                requestToSession.remove(emitter.getRequestId());
                cleaned++;
                log.debug("清理已关闭会话: sessionId={}, state={}", sessionId, emitter.getState());
            }
        }
        if (cleaned > 0) {
            log.info("清理完成: 移除{}个已关闭连接，剩余活跃连接数={}", cleaned, sessions.size());
        }
        return cleaned;
    }

    /**
     * 关闭所有连接
     * 用于应用关闭时清理资源
     */
    public void closeAllSessions() {
        log.info("关闭所有SSE连接，当前连接数={}", sessions.size());
        for (ManagedSseEmitter emitter : sessions.values()) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("关闭SSE连接失败: sessionId={}, error={}", emitter.getSessionId(), e.getMessage());
            }
        }
        sessions.clear();
        requestToSession.clear();
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息Map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", sessions.size());
        stats.put("globalListeners", globalListeners.size());
        stats.put("globalHooks", globalHooks.size());

        // 按状态统计
        Map<SseConnectionState, Long> stateCount = new HashMap<>();
        for (ManagedSseEmitter emitter : sessions.values()) {
            stateCount.merge(emitter.getState(), 1L, Long::sum);
        }
        stats.put("stateDistribution", stateCount);

        return stats;
    }
}
