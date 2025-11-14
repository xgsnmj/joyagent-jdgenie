package com.jd.genie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.entity.ChatMessage;
import com.jd.genie.entity.ChatSession;
import com.jd.genie.mapper.ChatSessionMapper;
import com.jd.genie.model.dto.MessageVO;
import com.jd.genie.model.dto.SessionMessagesResponse;
import com.jd.genie.model.dto.SessionVO;
import com.jd.genie.service.AgentProviderService;
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

    @Autowired
    private AgentProviderService agentProviderService;

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
    public SessionVO createOrGetSession(String sessionId, Long userId, String title, String agentType, String outputStyle, Long agentProviderId) {
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

            // 设置智能体配置ID（如果未指定，则使用用户的默认智能体）
            session.setAgentProviderId(agentProviderId);

            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());

            chatSessionMapper.insert(session);
            log.info("创建新会话: sessionId={}, userId={}, title={}, agentProviderId={}",
                    sessionId, userId, title, agentProviderId);
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
    @Override
    public void saveMessage(String sessionId, String role, String content, String files,
                            String thought, String tasks, String plan, String metadata) {
        com.jd.genie.entity.ChatMessage message = new com.jd.genie.entity.ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setFiles(files);
        message.setThought(thought);
        message.setTasks(tasks);
        message.setPlan(plan);
        message.setMetadata(metadata);
        message.setCreateTime(LocalDateTime.now());

        chatMessageMapper.insert(message);
        log.info("保存完整消息: sessionId={}, role={}, contentLength={}, hasThought={}, hasTasks={}, hasPlan={}, hasMetadata={}",
                sessionId, role, content != null ? content.length() : 0,
                thought != null, tasks != null, plan != null, metadata != null);
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
     * 获取会话的消息列表（含智能体信息）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（用于权限校验）
     * @return 会话消息响应对象
     */
    @Override
    public SessionMessagesResponse getSessionMessages(String sessionId, Long userId) {
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
        List<MessageVO> messageVOs = messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());

        // 构建完整响应，包含智能体信息
        SessionMessagesResponse.SessionMessagesResponseBuilder builder = SessionMessagesResponse.builder()
                .sessionId(session.getSessionId())
                .agentProviderId(session.getAgentProviderId())
                .agentType(session.getAgentType())
                .externalSessionId(session.getExternalSessionId())
                .messages(messageVOs);

        // 如果存在智能体配置ID，查询平台类型
        if (session.getAgentProviderId() != null) {
            AgentProvider provider = agentProviderService.getById(session.getAgentProviderId());
            if (provider != null) {
                builder.agentProviderType(provider.getProviderType());
            }
        }

        return builder.build();
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

    /**
     * 异步生成会话标题
     * 使用AI根据用户问题和助手回复生成简洁标题（不超过50字）
     */
    @Override
    public void generateSessionTitleAsync(String sessionId, String userQuery, String assistantReply) {
        // 使用线程池异步执行，不阻塞主流程
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 查询会话，检查是否需要生成标题
                LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ChatSession::getSessionId, sessionId);
                ChatSession session = chatSessionMapper.selectOne(wrapper);

                if (session == null) {
                    log.warn("生成标题失败：会话不存在 sessionId={}", sessionId);
                    return;
                }

                // 只有标题是默认值或用query前缀时才生成
                if (session.getTitle() == null || session.getTitle().equals("新对话") ||
                    (userQuery.length() > 20 && session.getTitle().startsWith(userQuery.substring(0, Math.min(20, userQuery.length()))))) {

                    log.info("开始为会话生成标题: sessionId={}", sessionId);

                    // 简化版：基于规则生成标题（提取关键词）
                    // TODO: 后续可替换为LLM生成
                    String generatedTitle = generateTitleByRule(userQuery, assistantReply);

                    // 更新数据库
                    LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(ChatSession::getSessionId, sessionId)
                        .set(ChatSession::getTitle, generatedTitle)
                        .set(ChatSession::getUpdateTime, LocalDateTime.now());

                    chatSessionMapper.update(null, updateWrapper);
                    log.info("会话标题生成成功: sessionId={}, title={}", sessionId, generatedTitle);
                } else {
                    log.debug("会话已有自定义标题，跳过生成: sessionId={}, title={}", sessionId, session.getTitle());
                }

            } catch (Exception e) {
                // 标题生成失败不影响主流程，只记录日志
                log.error("生成会话标题失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            }
        });
    }

    /**
     * 基于规则生成标题（简化版）
     * 提取用户问题的前50个字符作为标题
     * TODO: 后续可替换为LLM智能生成
     */
    private String generateTitleByRule(String userQuery, String assistantReply) {
        if (userQuery == null || userQuery.isEmpty()) {
            return "新对话";
        }

        // 清理标题（去除换行、多余空格等）
        String title = userQuery.trim()
            .replaceAll("\\n+", " ")          // 换行替换为空格
            .replaceAll("\\s+", " ")          // 多个空格合并为一个
            .replaceAll("^[\"']|[\"']$", ""); // 去除首尾引号

        // 限制长度为50字符
        if (title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }

        return title;
    }

    /**
     * 更新消息的metadata字段
     * 用于前端上报完整的multiAgent数据后更新到数据库
     *
     * @param messageId 消息ID
     * @param metadataJson metadata的JSON字符串
     */
    @Override
    public void updateMessageMetadata(Long messageId, String metadataJson) {
        ChatMessage message = new ChatMessage();
        message.setId(messageId);
        message.setMetadata(metadataJson);

        int result = chatMessageMapper.updateById(message);
        log.info("更新消息metadata: messageId={}, size={}, result={}",
                 messageId, metadataJson != null ? metadataJson.length() : 0, result);
    }

    /**
     * 根据requestId查找assistant消息ID
     * 用于将前端上报的multiAgent数据关联到对应的消息记录
     *
     * @param sessionId 会话ID
     * @param requestId 请求ID
     * @return 消息ID，如果未找到则返回null
     */
    @Override
    public Long findAssistantMessageByRequestId(String sessionId, String requestId) {
        // 查询session下最近的10条assistant消息（性能优化，避免全表扫描）
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .eq(ChatMessage::getRole, "assistant")
               .orderByDesc(ChatMessage::getCreateTime)
               .last("LIMIT 10");

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);

        // 遍历查找匹配的requestId（从metadata中解析）
        for (ChatMessage msg : messages) {
            if (msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
                try {
                    com.alibaba.fastjson.JSONObject metadata =
                        com.alibaba.fastjson.JSON.parseObject(msg.getMetadata());
                    if (requestId.equals(metadata.getString("requestId"))) {
                        log.debug("找到匹配的消息: messageId={}, requestId={}", msg.getId(), requestId);
                        return msg.getId();
                    }
                } catch (Exception e) {
                    // 忽略JSON解析错误，继续查找
                    log.debug("解析metadata失败: messageId={}, error={}", msg.getId(), e.getMessage());
                }
            }
        }

        // Fallback策略：如果没有找到匹配的requestId，返回最新的assistant消息
        // 这样可以确保在metadata为空或解析失败时，仍然能够更新数据
        if (!messages.isEmpty()) {
            Long fallbackId = messages.get(0).getId();
            log.warn("未找到匹配requestId的消息，使用最新assistant消息: sessionId={}, requestId={}, fallbackMessageId={}",
                     sessionId, requestId, fallbackId);
            return fallbackId;
        }

        log.warn("未找到任何assistant消息: sessionId={}, requestId={}", sessionId, requestId);
        return null;
    }

    /**
     * 验证会话是否属于指定用户
     * 用于权限校验，防止用户操作其他用户的会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return true表示该会话属于该用户，false表示不属于
     */
    @Override
    public boolean isSessionOwner(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
               .eq(ChatSession::getUserId, userId)
               .eq(ChatSession::getYn, 0);  // 只查询未删除的会话

        Long count = chatSessionMapper.selectCount(wrapper);
        boolean isOwner = count > 0;

        log.debug("验证会话归属: sessionId={}, userId={}, isOwner={}", sessionId, userId, isOwner);
        return isOwner;
    }

    /**
     * 根据消息ID获取消息详情
     * 用于读取消息的完整信息（包括metadata字段）
     *
     * @param messageId 消息ID
     * @return 消息实体，如果未找到则返回null
     */
    @Override
    public ChatMessage getMessageById(Long messageId) {
        if (messageId == null) {
            log.warn("getMessageById: messageId为空");
            return null;
        }

        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            log.warn("getMessageById: 未找到消息 messageId={}", messageId);
        } else {
            log.debug("getMessageById: 成功获取消息 messageId={}, hasMetadata={}",
                     messageId, message.getMetadata() != null);
        }

        return message;
    }

    /**
     * 根据sessionId获取会话实体
     * 用于获取完整的会话信息（包括externalSessionId）
     *
     * @param sessionId 会话ID
     * @return 会话实体，如果未找到则返回null
     */
    @Override
    public ChatSession getSessionBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("getSessionBySessionId: sessionId为空");
            return null;
        }

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId);

        ChatSession session = chatSessionMapper.selectOne(wrapper);
        if (session == null) {
            log.warn("getSessionBySessionId: 未找到会话 sessionId={}", sessionId);
        } else {
            log.debug("getSessionBySessionId: 成功获取会话 sessionId={}, externalSessionId={}",
                     sessionId, session.getExternalSessionId());
        }

        return session;
    }

    /**
     * 更新会话的外部会话ID
     * 用于保存外部智能体平台返回的会话ID
     *
     * @param sessionId 会话ID
     * @param externalSessionId 外部会话ID
     */
    @Override
    public void updateExternalSessionId(String sessionId, String externalSessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("updateExternalSessionId: sessionId为空");
            return;
        }

        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .set(ChatSession::getExternalSessionId, externalSessionId)
                .set(ChatSession::getUpdateTime, LocalDateTime.now());

        int result = chatSessionMapper.update(null, updateWrapper);
        if (result > 0) {
            log.info("更新外部会话ID成功: sessionId={}, externalSessionId={}",
                    sessionId, externalSessionId);
        } else {
            log.warn("更新外部会话ID失败: sessionId={}, externalSessionId={}",
                    sessionId, externalSessionId);
        }
    }
}
