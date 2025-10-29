package com.jd.genie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 *
 * <p>用于存储会话中的每条消息记录，包括用户提问和AI回复。
 * 支持附件文件的JSON格式存储，使用逻辑删除管理消息生命周期。</p>
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     * 关联chat_session表的session_id字段
     * 用于将消息归属到具体的会话
     */
    private String sessionId;

    /**
     * 消息角色
     * 标识消息的发送者类型：
     * - user: 用户发送的消息（提问）
     * - assistant: AI助手的回复消息
     * - system: 系统消息（如提示、警告等）
     */
    private String role;

    /**
     * 消息内容
     * 存储实际的消息文本内容
     * 对于用户消息，是用户的提问
     * 对于助手消息，是AI的回复
     */
    private String content;

    /**
     * 附件文件信息（JSON格式）
     * 存储消息相关的文件信息，如上传的数据文件、图片等
     * JSON数组格式示例：
     * [
     *   {
     *     "fileName": "data.csv",
     *     "fileUrl": "https://...",
     *     "fileSize": 1024,
     *     "fileType": "text/csv"
     *   }
     * ]
     */
    private String files;

    /**
     * 创建时间
     * 记录消息的发送/生成时间
     * 用于消息排序和时间线展示
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除标识
     * 0-未删除：消息正常存在
     * 1-已删除：消息已被删除（但数据仍保留在数据库中）
     * 使用MyBatis-Plus的@TableLogic注解实现逻辑删除
     */
    @TableLogic
    private Integer yn;
}
