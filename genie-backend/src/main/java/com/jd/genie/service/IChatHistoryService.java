package com.jd.genie.service;

import com.jd.genie.model.dto.MessageVO;
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
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param title       会话标题（可选）
     * @param agentType   智能体类型
     * @param outputStyle 输出样式
     * @return 会话信息
     */
    SessionVO createOrGetSession(String sessionId, Long userId, String title, String agentType, String outputStyle);

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
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题（可选）
     * @return 新创建的会话信息
     */
    SessionVO createSession(Long userId, String title);

    /**
     * 获取会话的消息列表
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 消息列表
     */
    List<MessageVO> getSessionMessages(String sessionId, Long userId);
}
