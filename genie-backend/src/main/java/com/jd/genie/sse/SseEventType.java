package com.jd.genie.sse;

/**
 * SSE事件类型枚举
 * 定义所有SSE相关的事件类型，用于事件驱动架构
 *
 * 使用场景：
 * - 注册事件监听器时指定监听的事件类型
 * - 触发事件时标识事件类型
 * - 实现针对不同事件的差异化处理逻辑
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
public enum SseEventType {
    /**
     * 连接已建立
     * 触发时机: ManagedSseEmitter构造完成后
     * 说明: SseEmitter本身没有onOpen回调，此事件为手动触发
     */
    CONNECTED("connected", "连接已建立"),

    /**
     * 消息发送前
     * 触发时机: 调用send()方法，在实际发送之前
     * 用途: 可用于消息预处理、验证、拦截等
     */
    BEFORE_SEND("before_send", "消息发送前"),

    /**
     * 消息发送后
     * 触发时机: 消息成功发送到客户端后
     * 用途: 记录日志、保存到数据库、统计等
     */
    AFTER_SEND("after_send", "消息发送后"),

    /**
     * 消息发送失败
     * 触发时机: send()方法抛出异常时
     * 用途: 错误处理、重试逻辑、告警等
     */
    SEND_FAILED("send_failed", "消息发送失败"),

    /**
     * 连接超时
     * 触发时机: SseEmitter.onTimeout回调触发时
     * 说明: 超时时间在构造函数中设置，默认300秒
     */
    TIMEOUT("timeout", "连接超时"),

    /**
     * 发生错误
     * 触发时机: SseEmitter.onError回调触发时
     * 说明: 包括网络错误、客户端断开等异常情况
     */
    ERROR("error", "发生错误"),

    /**
     * 连接完成
     * 触发时机: SseEmitter.onCompletion回调触发时
     * 说明: 无论正常完成还是异常完成都会触发
     */
    COMPLETION("completion", "连接完成"),

    /**
     * 连接断开
     * 触发时机: 检测到连接断开时（onTimeout/onError/onCompletion时）
     * 用途: 清理资源、调用外部平台中断接口等
     */
    DISCONNECTED("disconnected", "连接断开"),

    /**
     * 状态变化
     * 触发时机: SseConnectionState发生变化时
     * 用途: 监控连接状态、实现状态机逻辑等
     */
    STATE_CHANGE("state_change", "状态变化");

    /**
     * 事件代码
     */
    private final String code;

    /**
     * 事件描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        事件代码
     * @param description 事件描述
     */
    SseEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取事件代码
     *
     * @return 事件代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取事件描述
     *
     * @return 事件描述
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + "(" + description + ")";
    }
}
