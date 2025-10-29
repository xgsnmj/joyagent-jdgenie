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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
