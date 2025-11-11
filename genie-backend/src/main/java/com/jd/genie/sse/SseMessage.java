package com.jd.genie.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSE消息实体
 * 记录通过SseEmitter发送的每条消息的详细信息
 *
 * 用途：
 * - 审计：记录所有发送给客户端的消息
 * - 调试：追踪消息发送流程
 * - 统计：分析消息发送成功率、大小分布等
 * - 重放：支持消息历史回放功能
 *
 * 生命周期：
 * 1. 发送前创建（BEFORE_SEND事件）
 * 2. 发送后更新状态（AFTER_SEND/SEND_FAILED事件）
 * 3. 可选：持久化到数据库（通过DatabaseMessageHook）
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage {

    /**
     * 消息唯一标识
     * 格式：UUID或雪花ID
     */
    private String id;

    /**
     * 会话ID
     * 关联到chat_session表的session_id
     * 用于将消息归属到具体会话
     */
    private String sessionId;

    /**
     * SSE事件名称
     * 对应EventSource的event字段
     * 例如: "message", "error", "done"等
     */
    private String eventName;

    /**
     * 消息数据
     * JSON格式的字符串
     * 发送给客户端的实际内容
     */
    private String data;

    /**
     * 消息类型
     * 业务层面的消息分类
     * 例如: "text", "json", "plan", "tool_call"等
     */
    private String messageType;

    /**
     * 发送时间
     * 消息实际发送到客户端的时间戳
     */
    private LocalDateTime sendTime;

    /**
     * 发送是否成功
     * true: 成功发送到客户端
     * false: 发送失败（网络错误、客户端断开等）
     */
    private boolean success;

    /**
     * 失败原因
     * 当success=false时记录异常信息
     * 用于错误诊断和告警
     */
    private String failureReason;

    /**
     * 消息序号
     * 同一会话内的消息递增序号
     * 用于保证消息顺序、检测丢失等
     */
    private Integer sequence;

    /**
     * 数据大小（字节）
     * data字段的字节长度
     * 用于流量统计和性能分析
     */
    private Long dataSize;

    /**
     * 创建时间
     * 消息对象创建时间（通常在BEFORE_SEND事件）
     * 用于计算发送耗时
     */
    private LocalDateTime createTime;

    /**
     * 计算发送耗时（毫秒）
     *
     * @return 从创建到发送的耗时，如果未发送则返回null
     */
    public Long getSendDuration() {
        if (createTime == null || sendTime == null) {
            return null;
        }
        return java.time.Duration.between(createTime, sendTime).toMillis();
    }

    /**
     * 判断是否为错误消息
     *
     * @return true表示发送失败
     */
    public boolean isFailed() {
        return !success;
    }

    /**
     * 获取简短摘要
     * 用于日志输出
     *
     * @return 消息摘要字符串
     */
    public String getSummary() {
        return String.format("SseMessage[id=%s, session=%s, event=%s, success=%s, size=%dB]",
                id, sessionId, eventName, success, dataSize != null ? dataSize : 0);
    }
}
