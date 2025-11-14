package com.jd.genie.service;

import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.model.dto.SessionMessagesResponse;
import com.jd.genie.model.dto.SessionVO;
import java.util.List;

/**
 * 会话历史服务接口
 * 提供会话历史相关的业务操作
 *
 * @author JD Genie
 * @since 1.0.0
 */
public interface IChatHistoryService {

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<SessionVO> getUserSessions(Long userId);

    /**
     * 根据会话ID获取会话详情
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 会话详情
     */
    SessionVO getSessionDetail(String sessionId, Long userId);

    /**
     * 删除会话（逻辑删除）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 是否删除成功
     */
    boolean deleteSession(String sessionId, Long userId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @param title     新标题
     * @return 是否更新成功
     */
    boolean updateSessionTitle(String sessionId, Long userId, String title);

    /**
     * 创建或获取会话
     * 如果会话不存在则创建，存在则返回现有会话
     *
     * @param sessionId        会话ID
     * @param userId           用户ID
     * @param title            会话标题（可选）
     * @param agentType        智能体类型
     * @param outputStyle      输出样式
     * @param agentProviderId  智能体配置ID（可选，null时使用用户默认智能体）
     * @return 会话信息
     */
    SessionVO createOrGetSession(String sessionId, Long userId, String title, String agentType, String outputStyle, Long agentProviderId);

    /**
     * 保存消息
     *
     * @param sessionId 会话ID
     * @param role      角色（user/assistant）
     * @param content   消息内容
     * @param files     附件文件信息（JSON格式，可选）
     */
    void saveMessage(String sessionId, String role, String content, String files);

    /**
     * 保存完整的消息数据
     * 包括思考过程、任务详情、计划信息等完整会话数据
     *
     * @param sessionId 会话ID
     * @param role      角色（user/assistant）
     * @param content   消息内容
     * @param files     附件文件信息（JSON格式，可选）
     * @param thought   思考过程（JSON格式，可选）
     * @param tasks     任务详情（JSON格式，可选）
     * @param plan      计划信息（JSON格式，可选）
     * @param metadata  其他元数据（JSON格式，可选）
     */
    void saveMessage(String sessionId, String role, String content, String files,
                     String thought, String tasks, String plan, String metadata);

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题（可选）
     * @return 新创建的会话信息
     */
    SessionVO createSession(Long userId, String title);

    /**
     * 获取会话的消息列表（含智能体信息）
     * 返回包含智能体ID和类型的完整响应，便于前端正确解析消息
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 会话消息响应对象（包含智能体信息和消息列表）
     */
    SessionMessagesResponse getSessionMessages(String sessionId, Long userId);

    /**
     * 异步生成会话标题
     * 使用AI根据用户问题和助手回复生成简洁标题（不超过50字）
     *
     * @param sessionId 会话ID
     * @param userQuery 用户问题
     * @param assistantReply AI回复
     */
    void generateSessionTitleAsync(String sessionId, String userQuery, String assistantReply);

    /**
     * 更新消息的metadata字段
     * 用于前端上报完整的multiAgent数据后更新到数据库
     *
     * @param messageId 消息ID
     * @param metadataJson metadata的JSON字符串
     */
    void updateMessageMetadata(Long messageId, String metadataJson);

    /**
     * 根据requestId查找assistant消息ID
     * 用于将前端上报的multiAgent数据关联到对应的消息记录
     *
     * @param sessionId 会话ID
     * @param requestId 请求ID
     * @return 消息ID，如果未找到则返回null
     */
    Long findAssistantMessageByRequestId(String sessionId, String requestId);

    /**
     * 验证会话是否属于指定用户
     * 用于权限校验，防止用户操作其他用户的会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return true表示该会话属于该用户，false表示不属于
     */
    boolean isSessionOwner(String sessionId, Long userId);

    /**
     * 根据消息ID获取消息详情
     * 用于读取消息的完整信息（包括metadata字段）
     *
     * @param messageId 消息ID
     * @return 消息实体，如果未找到则返回null
     */
    com.jd.genie.entity.ChatMessage getMessageById(Long messageId);

    /**
     * 根据sessionId获取会话实体
     * 用于获取完整的会话信息（包括externalSessionId）
     *
     * @param sessionId 会话ID
     * @return 会话实体，如果未找到则返回null
     */
    com.jd.genie.entity.ChatSession getSessionBySessionId(String sessionId);

    /**
     * 更新会话的外部会话ID
     * 用于保存外部智能体平台返回的会话ID
     *
     * @param sessionId 会话ID
     * @param externalSessionId 外部会话ID
     */
    void updateExternalSessionId(String sessionId, String externalSessionId);
}
