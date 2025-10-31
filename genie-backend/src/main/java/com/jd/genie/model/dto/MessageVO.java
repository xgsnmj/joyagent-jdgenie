package com.jd.genie.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息信息响应VO
 * 用于返回给前端的消息信息
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息角色（user/assistant/system）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附件文件信息（JSON格式）
     */
    private String files;

    /**
     * 思考过程（AI的思维链）
     */
    private String thought;

    /**
     * 任务详情（JSON数组，包含所有任务执行过程）
     */
    private String tasks;

    /**
     * 计划信息（JSON对象，包含计划标题和步骤）
     */
    private String plan;

    /**
     * 其他元数据（JSON对象，存储额外信息）
     */
    private String metadata;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
