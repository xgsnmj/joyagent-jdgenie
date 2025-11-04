package com.jd.genie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSE消息缓存实体
 * 用于临时存储SSE流消息，待流结束后统一持久化到chat_message表
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Data
@TableName("sse_message_cache")
public class SSEMessageCache {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID（关联chat_session.session_id）
     */
    private String sessionId;

    /**
     * 消息序号（从0开始递增）
     * 用于保证消息顺序
     */
    private Integer messageSequence;

    /**
     * SSE事件类型
     * 如：message、error、done
     */
    private String eventType;

    /**
     * 事件数据内容
     */
    private String eventData;

    /**
     * 原始数据（完整SSE消息）
     * 用于调试和问题排查
     */
    private String rawData;

    /**
     * 是否已持久化到chat_message表
     * 0-否 1-是
     */
    private Boolean isPersisted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
