package com.jd.genie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jd.genie.entity.ChatMessage;
import com.jd.genie.entity.ChatSession;
import com.jd.genie.mapper.ChatSessionMapper;
import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.model.dto.SessionVO;
import com.jd.genie.service.IChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话历史服务实现类
 * 实现会话历史相关的业务逻辑
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl implements IChatHistoryService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private com.jd.genie.mapper.ChatMessageMapper chatMessageMapper;

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    @Override
    public List<SessionVO> getUserSessions(Long userId) {
        // 查询用户的所有会话，按更新时间倒序排列
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);

        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);

        // 转换为VO并计算消息数量
        return sessions.stream()
                .map(session -> {
                    SessionVO vo = convertToVO(session);
                    // 计算该会话的消息数量
                    LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
                    msgWrapper.eq(ChatMessage::getSessionId, session.getSessionId());
                    Long count = chatMessageMapper.selectCount(msgWrapper);
                    vo.setMessageCount(count.intValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据会话ID获取会话详情
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 会话详情
     */
    @Override
    public SessionVO getSessionDetail(String sessionId, Long userId) {
        // 查询会话
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId);

        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session == null) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        return convertToVO(session);
    }

    /**
     * 删除会话（逻辑删除）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 是否删除成功
     */
    @Override
    public boolean deleteSession(String sessionId, Long userId) {
        // 先查询会话是否存在且属于该用户
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId);

        ChatSession session = chatSessionMapper.selectOne(queryWrapper);
        if (session == null) {
            throw new RuntimeException("会话不存在或无权删除");
        }

        // 执行逻辑删除
        // MyBatis-Plus会自动将yn字段设置为1（已删除）
        int result = chatSessionMapper.deleteById(session.getId());
        return result > 0;
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @param title     新标题
     * @return 是否更新成功
     */
    @Override
    public boolean updateSessionTitle(String sessionId, Long userId, String title) {
        // 先查询会话是否存在且属于该用户
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId);

        ChatSession session = chatSessionMapper.selectOne(queryWrapper);
        if (session == null) {
            throw new RuntimeException("会话不存在或无权修改");
        }

        // 更新标题
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .set(ChatSession::getTitle, title)
                .set(ChatSession::getUpdateTime, LocalDateTime.now());

        int result = chatSessionMapper.update(null, updateWrapper);
        return result > 0;
    }

    /**
     * 创建或获取会话
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param title       会话标题（可选）
     * @param agentType   智能体类型
     * @param outputStyle 输出样式
     * @return 会话信息
     */
    @Override
    public SessionVO createOrGetSession(String sessionId, Long userId, String title, String agentType, String outputStyle) {
        // 先查询会话是否存在
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);

        ChatSession session = chatSessionMapper.selectOne(wrapper);

        if (session == null) {
            // 会话不存在，创建新会话
            session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setTitle(title != null && !title.isEmpty() ? title : "新对话");
            session.setAgentType(agentType);
            session.setOutputStyle(outputStyle);
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());

            chatSessionMapper.insert(session);
            log.info("创建新会话: sessionId={}, userId={}, title={}", sessionId, userId, title);
        } else {
            // 会话存在，更新最后活跃时间
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
            log.debug("更新会话活跃时间: sessionId={}", sessionId);
        }

        return convertToVO(session);
    }

    /**
     * 保存消息
     *
     * @param sessionId 会话ID
     * @param role      角色（user/assistant）
     * @param content   消息内容
     * @param files     附件文件信息（JSON格式，可选）
     */
    @Override
    public void saveMessage(String sessionId, String role, String content, String files) {
        com.jd.genie.entity.ChatMessage message = new com.jd.genie.entity.ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setFiles(files);
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);
        log.debug("保存消息: sessionId={}, role={}, contentLength={}", sessionId, role, content.length());
    }

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题（可选）
     * @return 新创建的会话信息
     */
    @Override
    public SessionVO createSession(Long userId, String title) {
        // 生成新的会话ID
        String sessionId = UUID.randomUUID().toString();

        // 创建会话实体
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        // 保存到数据库
        chatSessionMapper.insert(session);
        log.info("创建新会话: sessionId={}, userId={}, title={}", sessionId, userId, title);

        // 转换为VO并返回
        SessionVO vo = convertToVO(session);
        vo.setMessageCount(0); // 新会话消息数为0
        return vo;
    }

    /**
     * 获取会话的消息列表
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 消息列表
     */
    @Override
    public List<MessageVO> getSessionMessages(String sessionId, Long userId) {
        // 先验证会话是否存在且属于该用户
        LambdaQueryWrapper<ChatSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId);

        ChatSession session = chatSessionMapper.selectOne(sessionWrapper);
        if (session == null) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        // 查询该会话的所有消息，按时间正序排列
        LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);

        List<ChatMessage> messages = chatMessageMapper.selectList(messageWrapper);

        // 转换为VO
        return messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
    }

    /**
     * 将ChatSession实体转换为SessionVO
     *
     * @param session 会话实体
     * @return 会话VO
     */
    private SessionVO convertToVO(ChatSession session) {
        SessionVO vo = new SessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    /**
     * 将ChatMessage实体转换为MessageVO
     *
     * @param message 消息实体
     * @return 消息VO
     */
    private MessageVO convertToMessageVO(ChatMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
