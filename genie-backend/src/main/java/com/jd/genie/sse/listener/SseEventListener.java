package com.jd.genie.sse.listener;

import com.jd.genie.sse.ManagedSseEmitter;
import com.jd.genie.sse.SseEventType;

/**
 * SSE事件监听器接口
 * 定义SSE事件的统一监听机制
 *
 * 使用场景：
 * - 实现自定义业务逻辑（如日志记录、统计分析）
 * - 集成第三方服务（如监控系统、告警平台）
 * - 扩展框架功能（如消息队列发布、事件溯源）
 *
 * 实现要求：
 * 1. 线程安全：监听器可能在多个线程中被调用
 * 2. 异常处理：不应抛出未捕获异常，避免影响主流程
 * 3. 性能考虑：避免阻塞操作，建议异步处理
 *
 * 使用示例：
 * <pre>
 * // 实现监听器
 * public class MyListener implements SseEventListener {
 *     &#64;Override
 *     public void onEvent(SseEventType eventType, ManagedSseEmitter emitter, Object eventData) {
 *         if (eventType == SseEventType.AFTER_SEND) {
 *             SseMessage message = (SseMessage) eventData;
 *             log.info("消息已发送: {}", message.getSummary());
 *         }
 *     }
 * }
 *
 * // 注册监听器
 * emitter.addEventListener(new MyListener());
 * </pre>
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@FunctionalInterface
public interface SseEventListener {

    /**
     * 处理SSE事件
     * 当SSE连接中发生特定事件时被调用
     *
     * @param eventType 事件类型，不为null
     * @param emitter   触发事件的SseEmitter实例，不为null
     * @param eventData 事件附加数据，可能为null，具体类型取决于事件类型：
     *                  <ul>
     *                  <li>CONNECTED: null</li>
     *                  <li>BEFORE_SEND: SseMessage（待发送的消息）</li>
     *                  <li>AFTER_SEND: SseMessage（已发送的消息）</li>
     *                  <li>SEND_FAILED: SseMessage（发送失败的消息）</li>
     *                  <li>TIMEOUT: null</li>
     *                  <li>ERROR: Throwable（异常对象）</li>
     *                  <li>COMPLETION: null</li>
     *                  <li>DISCONNECTED: String（断开原因，如"timeout", "error", "completed"）</li>
     *                  <li>STATE_CHANGE: SseConnectionState（新状态）</li>
     *                  </ul>
     *
     * @throws Exception 实现者可以抛出异常，但建议内部捕获处理，避免影响SSE主流程
     */
    void onEvent(SseEventType eventType, ManagedSseEmitter emitter, Object eventData) throws Exception;

    /**
     * 判断是否对特定事件类型感兴趣
     * 默认实现为关注所有事件，子类可以覆盖此方法进行过滤
     *
     * @param eventType 事件类型
     * @return true表示关注此事件，false表示忽略
     */
    default boolean isInterestedIn(SseEventType eventType) {
        return true;
    }

    /**
     * 获取监听器名称
     * 用于日志记录和调试
     *
     * @return 监听器名称，默认返回类名
     */
    default String getListenerName() {
        return this.getClass().getSimpleName();
    }
}
