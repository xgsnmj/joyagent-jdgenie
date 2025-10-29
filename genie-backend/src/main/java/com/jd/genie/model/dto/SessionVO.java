package com.jd.genie.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话信息响应VO
 * 用于返回给前端的会话信息
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class SessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话记录ID
     */
    private Long id;

    /**
     * 会话唯一标识符
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * Agent类型
     */
    private String agentType;

    /**
     * 输出样式
     */
    private String outputStyle;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 消息数量
     */
    private Integer messageCount;
}
