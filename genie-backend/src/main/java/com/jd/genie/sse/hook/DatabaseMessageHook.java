package com.jd.genie.sse.hook;

import com.jd.genie.sse.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据库消息钩子
 * 自动将所有SSE消息保存到数据库
 *
 * 功能：
 * 1. 消息审计：记录所有发送给客户端的消息
 * 2. 问题排查：根据sessionId查询历史消息
 * 3. 数据分析：统计消息发送成功率、大小分布等
 * 4. 消息重放：支持会话历史回放功能
 *
 * 使用方式：
 * <pre>
 * // 方式1：全局注册（推荐）
 * &#64;Autowired
 * private SseSessionManager sessionManager;
 * &#64;Autowired
 * private DatabaseMessageHook databaseHook;
 *
 * &#64;PostConstruct
 * public void init() {
 *     sessionManager.registerGlobalHook(databaseHook);
 * }
 *
 * // 方式2：单独注册
 * ManagedSseEmitter emitter = new ManagedSseEmitter(...);
 * emitter.addMessageHook(databaseHook);
 * </pre>
 *
 * 性能考虑：
 * - 异步保存：建议使用@Async或消息队列避免阻塞发送流程
 * - 批量保存：可以收集一批消息后批量写入
 * - 索引优化：在sessionId、sendTime字段上建立索引
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
@Component
public class DatabaseMessageHook implements SseMessageHook {

    // TODO: 注入Repository
    // @Autowired
    // private SseMessageRepository sseMessageRepository;

    /**
     * 发送前钩子
     * 此钩子不做处理，仅记录日志
     *
     * @param message 待发送的消息
     * @return true表示继续发送
     */
    @Override
    public boolean beforeSend(SseMessage message) {
        log.trace("准备发送消息: sessionId={}, messageId={}, size={}B",
                message.getSessionId(), message.getId(), message.getDataSize());
        // 不拦截任何消息
        return true;
    }

    /**
     * 发送后钩子
     * 将消息保存到数据库
     *
     * @param message 已发送的消息（包含发送结果）
     */
    @Override
    public void afterSend(SseMessage message) {
        try {
            // TODO: 实现数据库保存逻辑
            // 示例实现：
            // SseMessageEntity entity = convertToEntity(message);
            // sseMessageRepository.save(entity);

            if (message.isSuccess()) {
                log.debug("消息发送成功并记录: sessionId={}, messageId={}, sequence={}, size={}B",
                        message.getSessionId(), message.getId(), message.getSequence(), message.getDataSize());
            } else {
                log.warn("消息发送失败并记录: sessionId={}, messageId={}, reason={}",
                        message.getSessionId(), message.getId(), message.getFailureReason());
            }

            // 临时实现：仅记录日志
            logMessageToConsole(message);

        } catch (Exception e) {
            log.error("保存SSE消息到数据库失败: sessionId={}, messageId={}, error={}",
                    message.getSessionId(), message.getId(), e.getMessage(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 临时实现：将消息记录到控制台
     * TODO: 替换为数据库保存
     *
     * @param message 消息对象
     */
    private void logMessageToConsole(SseMessage message) {
        log.info("📨 SSE消息记录: {}", message.getSummary());
        log.debug("  ├─ 事件名称: {}", message.getEventName());
        log.debug("  ├─ 消息类型: {}", message.getMessageType());
        log.debug("  ├─ 发送时间: {}", message.getSendTime());
        log.debug("  ├─ 发送耗时: {}ms", message.getSendDuration());
        log.debug("  ├─ 发送结果: {}", message.isSuccess() ? "✅ 成功" : "❌ 失败");
        if (!message.isSuccess()) {
            log.debug("  └─ 失败原因: {}", message.getFailureReason());
        }
    }

    /**
     * TODO: 转换为数据库实体
     * 将SseMessage转换为MyBatis-Plus实体类
     *
     * @param message SSE消息对象
     * @return 数据库实体对象
     */
    // private SseMessageEntity convertToEntity(SseMessage message) {
    //     return SseMessageEntity.builder()
    //             .id(message.getId())
    //             .sessionId(message.getSessionId())
    //             .eventName(message.getEventName())
    //             .data(message.getData())
    //             .messageType(message.getMessageType())
    //             .sendTime(message.getSendTime())
    //             .success(message.isSuccess())
    //             .failureReason(message.getFailureReason())
    //             .sequence(message.getSequence())
    //             .dataSize(message.getDataSize())
    //             .createTime(message.getCreateTime())
    //             .build();
    // }

    @Override
    public String getHookName() {
        return "DatabaseMessageHook";
    }

    @Override
    public int getPriority() {
        // 优先级100，确保在业务钩子之后执行
        return 100;
    }
}
