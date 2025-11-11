package com.jd.genie.sse;

/**
 * SSE连接状态枚举
 * 定义SseEmitter的所有可能状态，用于状态管理和监控
 *
 * 状态转换流程：
 * CREATED → ACTIVE → SENDING → ACTIVE → (TIMEOUT/ERROR/COMPLETED) → CLOSED
 *
 * 使用场景：
 * - 追踪SSE连接的生命周期
 * - 实现基于状态的业务逻辑
 * - 监控和告警
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
public enum SseConnectionState {
    /**
     * 已创建，尚未连接
     * 说明: ManagedSseEmitter对象已创建，但客户端尚未连接
     * 持续时间: 通常很短暂
     */
    CREATED("created", "已创建"),

    /**
     * 已连接，活跃中
     * 说明: 客户端已连接，可以正常发送消息
     * 持续时间: 整个会话期间的主要状态
     */
    ACTIVE("active", "活跃中"),

    /**
     * 正在发送消息
     * 说明: send()方法执行中
     * 持续时间: 毫秒级，发送完成后立即恢复为ACTIVE
     */
    SENDING("sending", "发送中"),

    /**
     * 超时
     * 说明: 连接超过设定的超时时间未活动
     * 触发条件: onTimeout回调触发
     * 后续状态: CLOSED
     */
    TIMEOUT("timeout", "已超时"),

    /**
     * 发生错误
     * 说明: 连接过程中发生异常
     * 触发条件: onError回调触发
     * 常见原因: 网络中断、客户端断开、发送失败等
     * 后续状态: CLOSED
     */
    ERROR("error", "发生错误"),

    /**
     * 正常完成
     * 说明: complete()方法被调用，连接正常结束
     * 触发条件: 业务逻辑主动调用complete()
     * 后续状态: CLOSED
     */
    COMPLETED("completed", "已完成"),

    /**
     * 异常完成
     * 说明: completeWithError()方法被调用
     * 触发条件: 业务逻辑检测到错误并主动关闭
     * 后续状态: CLOSED
     */
    FAILED("failed", "失败"),

    /**
     * 已关闭
     * 说明: 连接已彻底关闭，资源已清理
     * 触发条件: onCompletion回调完成后
     * 特点: 终态，不会再转换到其他状态
     */
    CLOSED("closed", "已关闭");

    /**
     * 状态代码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        状态代码
     * @param description 状态描述
     */
    SseConnectionState(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态代码
     *
     * @return 状态代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为终态
     * 终态包括: TIMEOUT, ERROR, COMPLETED, FAILED, CLOSED
     *
     * @return true表示终态
     */
    public boolean isTerminal() {
        return this == TIMEOUT || this == ERROR ||
               this == COMPLETED || this == FAILED || this == CLOSED;
    }

    /**
     * 判断是否为活跃状态
     * 活跃状态包括: ACTIVE, SENDING
     *
     * @return true表示活跃
     */
    public boolean isActive() {
        return this == ACTIVE || this == SENDING;
    }

    @Override
    public String toString() {
        return code + "(" + description + ")";
    }
}
