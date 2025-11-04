package com.jd.genie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 *
 * <p>用于存储用户的聊天会话信息，每个会话代表一次完整的对话历史。
 * 支持多种Agent类型和输出样式配置，使用逻辑删除管理会话生命周期。</p>
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
@TableName("chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话记录ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一标识符
     * 用于前后端交互时标识具体的会话
     * 通常使用UUID等全局唯一ID生成策略
     */
    private String sessionId;

    /**
     * 用户ID
     * 关联sys_user表，标识该会话所属的用户
     */
    private Long userId;

    /**
     * 关联的智能体服务商ID
     * 关联agent_provider表，标识该会话使用的智能体配置
     * 用于支持多智能体平台对接
     */
    private Long agentProviderId;

    /**
     * 外部智能体平台返回的会话ID
     * 用于多轮对话时传递给外部平台（如Coze、融汇）
     * 实现上下文连续性
     */
    private String externalSessionId;

    /**
     * 会话标题
     * 用于在会话列表中显示，方便用户识别和管理
     * 可以是用户自定义标题或根据首条消息自动生成
     */
    private String title;

    /**
     * Agent类型
     * 标识使用的AI代理类型，例如：
     * - data_agent: 数据分析代理
     * - code_agent: 代码生成代理
     * - general: 通用对话代理
     */
    private String agentType;

    /**
     * 输出样式
     * 控制AI回复的格式和风格，例如：
     * - markdown: Markdown格式
     * - plain: 纯文本
     * - structured: 结构化数据
     */
    private String outputStyle;

    /**
     * 创建时间
     * 记录会话的创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录会话的最后更新时间（如添加新消息、修改标题等）
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     * 0-未删除：会话正常存在
     * 1-已删除：会话已被用户删除（但数据仍保留在数据库中）
     * 使用MyBatis-Plus的@TableLogic注解实现逻辑删除
     */
    @TableLogic
    private Integer yn;
}
