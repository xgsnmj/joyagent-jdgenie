package com.jd.genie.sse.hook;

import com.jd.genie.sse.SseMessage;

/**
 * SSE消息钩子接口
 * 在消息发送的关键节点进行拦截和处理
 *
 * 与SseEventListener的区别：
 * - Listener: 观察者模式，监听所有事件，不能修改流程
 * - Hook: 钩子模式，拦截特定操作，可以修改数据或中断流程
 *
 * 使用场景：
 * - 消息预处理：发送前修改消息内容、添加字段等
 * - 消息验证：检查消息格式、大小限制
 * - 消息持久化：保存消息到数据库、消息队列
 * - 消息统计：记录发送成功率、耗时等
 *
 * 执行顺序：
 * 1. beforeSend() - 发送前，可以修改message或中断发送
 * 2. SseEmitter.send() - 实际发送
 * 3. afterSend() - 发送后，更新message状态
 *
 * 使用示例：
 * <pre>
 * public class LoggingHook implements SseMessageHook {
 *     &#64;Override
 *     public boolean beforeSend(SseMessage message) {
 *         log.info("准备发送: {}", message.getSummary());
 *         // 检查消息大小
 *         if (message.getDataSize() > 1024 * 1024) {
 *             log.warn("消息过大，拒绝发送");
 *             return false; // 中断发送
 *         }
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public void afterSend(SseMessage message) {
 *         if (message.isSuccess()) {
 *             log.info("发送成功: {}", message.getSummary());
 *         } else {
 *             log.error("发送失败: {}", message.getFailureReason());
 *         }
 *     }
 * }
 * </pre>
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
public interface SseMessageHook {

    /**
     * 消息发送前钩子
     * 在SseEmitter.send()之前调用
     *
     * 可以执行的操作：
     * - 修改消息内容（message对象可变）
     * - 验证消息格式
     * - 记录日志
     * - 决定是否发送
     *
     * @param message 待发送的消息对象，可以修改其内容
     * @return true表示继续发送，false表示中断发送
     * @throws Exception 处理异常，异常将导致发送中断
     */
    boolean beforeSend(SseMessage message) throws Exception;

    /**
     * 消息发送后钩子
     * 在SseEmitter.send()之后调用（无论成功或失败）
     *
     * 可以执行的操作：
     * - 更新消息状态
     * - 持久化到数据库
     * - 发送到消息队列
     * - 统计分析
     *
     * 注意：
     * - message.isSuccess()表示发送结果
     * - message.getFailureReason()包含失败原因
     *
     * @param message 已发送的消息对象，包含发送结果
     * @throws Exception 处理异常，不会影响主流程，但会记录错误日志
     */
    void afterSend(SseMessage message) throws Exception;

    /**
     * 获取钩子名称
     * 用于日志记录和调试
     *
     * @return 钩子名称，默认返回类名
     */
    default String getHookName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取执行优先级
     * 数值越小优先级越高，越早执行
     * 默认优先级为0
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 0;
    }
}
