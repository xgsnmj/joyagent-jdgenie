package com.jd.genie.sse;

import com.jd.genie.sse.hook.SseMessageHook;
import com.jd.genie.sse.listener.SseEventListener;
import com.jd.genie.util.SseEmitterUTF8;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 托管的SSE发射器
 * 在Spring SseEmitter基础上增加事件监听、消息钩子、状态管理等功能
 *
 * 核心功能：
 * 1. 事件驱动：支持9种事件类型的监听
 * 2. 消息钩子：在发送前后拦截和处理消息
 * 3. 状态管理：追踪连接的完整生命周期
 * 4. 线程安全：使用CopyOnWriteArrayList和volatile保证并发安全
 *
 * 生命周期：
 * CREATED → ACTIVE → SENDING → ACTIVE → (TIMEOUT/ERROR/COMPLETED) → CLOSED
 *
 * 使用示例：
 * <pre>
 * ManagedSseEmitter emitter = new ManagedSseEmitter(300000L, sessionId, requestId);
 *
 * // 注册监听器
 * emitter.addEventListener((eventType, em, data) -> {
 *     log.info("事件: {}", eventType);
 * });
 *
 * // 注册钩子
 * emitter.addMessageHook(new DatabaseMessageHook());
 *
 * // 发送消息（自动触发钩子和事件）
 * emitter.send("Hello World", "message");
 * </pre>
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
public class ManagedSseEmitter extends SseEmitterUTF8 {

    /**
     * 会话ID
     * 关联到chat_session表
     */
    @Getter
    private final String sessionId;

    /**
     * 请求ID
     * 用于日志追踪
     */
    @Getter
    private final String requestId;

    /**
     * 当前连接状态
     * 使用volatile保证可见性
     */
    @Getter
    private volatile SseConnectionState state;

    /**
     * 事件监听器列表
     * 使用CopyOnWriteArrayList保证线程安全
     */
    private final CopyOnWriteArrayList<SseEventListener> eventListeners;

    /**
     * 消息钩子列表
     * 按优先级排序（升序）
     */
    private final List<SseMessageHook> messageHooks;

    /**
     * 消息序号生成器
     * 保证同一会话内消息有序
     */
    private final AtomicInteger messageSequence;

    /**
     * 创建时间
     */
    @Getter
    private final LocalDateTime createTime;

    /**
     * 消息缓存列表
     * 存储等待持久化的SSE消息
     */
    private final List<SseMessage> messageCache;

    /**
     * 消息缓存大小限制
     * 超过此值时强制保存，避免内存溢出
     */
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 构造函数
     *
     * @param timeout   超时时间（毫秒），null表示不超时
     * @param sessionId 会话ID
     * @param requestId 请求ID
     */
    public ManagedSseEmitter(Long timeout, String sessionId, String requestId) {
        super(timeout);
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.state = SseConnectionState.CREATED;
        this.eventListeners = new CopyOnWriteArrayList<>();
        this.messageHooks = new ArrayList<>();
        this.messageSequence = new AtomicInteger(0);
        this.createTime = LocalDateTime.now();
        this.messageCache = new ArrayList<>();

        // 注册原生SseEmitter的回调
        registerNativeCallbacks();

        // 触发CONNECTED事件（Spring SseEmitter没有onOpen回调，手动触发）
        setState(SseConnectionState.ACTIVE);
        fireEvent(SseEventType.CONNECTED, null);

        log.debug("ManagedSseEmitter创建成功: sessionId={}, requestId={}", sessionId, requestId);
    }

    /**
     * 注册Spring SseEmitter的原生回调
     * 将原生事件桥接到自定义事件系统
     */
    private void registerNativeCallbacks() {
        // 超时回调
        super.onTimeout(() -> {
            log.info("SSE连接超时: sessionId={}, requestId={}", sessionId, requestId);
            setState(SseConnectionState.TIMEOUT);
            fireEvent(SseEventType.TIMEOUT, null);
            fireEvent(SseEventType.DISCONNECTED, "timeout");
        });

        // 错误回调
        super.onError((err) -> {
            log.error("SSE连接错误: sessionId={}, requestId={}, error={}", sessionId, requestId, err.getMessage());
            setState(SseConnectionState.ERROR);
            fireEvent(SseEventType.ERROR, err);
            fireEvent(SseEventType.DISCONNECTED, "error");
        });

        // 完成回调
        super.onCompletion(() -> {
            log.info("SSE连接完成: sessionId={}, requestId={}", sessionId, requestId);
            fireEvent(SseEventType.COMPLETION, null);
            setState(SseConnectionState.CLOSED);
        });
    }



    /**
     * 重载父类send方法（核心方法）
     * 发送SSE事件构建器，经过完整的钩子和事件流程
     *
     * @param builder SSE事件构建器
     * @throws IOException 发送失败时抛出
     */
    @Override
    public void send(SseEventBuilder builder) throws IOException {
        // 检查状态
        if (state.isTerminal()) {
            log.warn("连接已关闭，无法发送消息: sessionId={}, state={}", sessionId, state);
            return;
        }

        // 从builder中提取信息构建SseMessage
        SseMessage message = buildMessageFromEventBuilder(builder);

        // 触发BEFORE_SEND事件
        fireEvent(SseEventType.BEFORE_SEND, message);

        // 执行beforeSend钩子
        boolean shouldSend = executeBeforeSendHooks(message);
        if (!shouldSend) {
            log.info("消息被钩子拦截，取消发送: sessionId={}, messageId={}", sessionId, message.getId());
            message.setSuccess(false);
            message.setFailureReason("Blocked by hook");
            fireEvent(SseEventType.SEND_FAILED, message);
            return;
        }

        // 切换到SENDING状态
        SseConnectionState previousState = state;
        setState(SseConnectionState.SENDING);

        try {
            // 实际发送
            super.send(builder);

            // 发送成功
            message.setSuccess(true);
            message.setSendTime(LocalDateTime.now());
            log.debug("消息发送成功: sessionId={}, messageId={}, size={}B",
                    sessionId, message.getId(), message.getDataSize());

            // 触发AFTER_SEND事件
            fireEvent(SseEventType.AFTER_SEND, message);

        } catch (IOException e) {
            // 发送失败
            message.setSuccess(false);
            message.setSendTime(LocalDateTime.now());
            message.setFailureReason(e.getMessage());
            log.error("消息发送失败: sessionId={}, messageId={}, error={}",
                    sessionId, message.getId(), e.getMessage());

            // 触发SEND_FAILED事件
            fireEvent(SseEventType.SEND_FAILED, message);

            throw e;
        } finally {
            // 恢复之前的状态（ACTIVE）
            setState(previousState);

            // 执行afterSend钩子
            executeAfterSendHooks(message);
        }
    }

    /**
     * 重载父类send方法
     * 发送对象并指定媒体类型
     *
     * @param object    要发送的对象
     * @param mediaType 媒体类型
     * @throws IOException 发送失败时抛出
     */
    @Override
    public void send(Object object, MediaType mediaType) throws IOException {
        SseEmitter.SseEventBuilder builder = SseEmitter.event().data(object, mediaType);
        send(builder);
    }

    /**
     * 重载父类send方法
     * 发送对象，自动转换为JSON
     *
     * @param object 要发送的对象
     * @throws IOException 发送失败时抛出
     */
    @Override
    public void send(Object object) throws IOException {
        send(object, null);
    }

    /**
     * 发送消息（便捷方法）
     * 简化版本，内部调用send(SseEventBuilder)
     *
     * @param data      消息数据（JSON字符串或普通文本）
     * @param eventName SSE事件名称
     * @throws Exception 发送失败时抛出
     */
    public void send(String data, String eventName) throws Exception {
        SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .name(eventName)
                .data(data);
        send(eventBuilder); // 调用重载的方法，自动经过钩子和事件系统
    }

    /**
     * 正常完成连接
     * 重写父类方法，增加状态管理
     */
    @Override
    public void complete() {
        if (state.isTerminal()) {
            log.warn("连接已处于终态，忽略complete调用: sessionId={}, state={}", sessionId, state);
            return;
        }

        log.info("主动完成SSE连接: sessionId={}", sessionId);
        setState(SseConnectionState.COMPLETED);
        fireEvent(SseEventType.DISCONNECTED, "completed");
        super.complete();
    }

    /**
     * 异常完成连接
     * 重写父类方法，增加状态管理
     */
    @Override
    public void completeWithError(Throwable ex) {
        if (state.isTerminal()) {
            log.warn("连接已处于终态，忽略completeWithError调用: sessionId={}, state={}", sessionId, state);
            return;
        }

        log.error("异常完成SSE连接: sessionId={}, error={}", sessionId, ex.getMessage());
        setState(SseConnectionState.FAILED);
        fireEvent(SseEventType.DISCONNECTED, "failed");
        super.completeWithError(ex);
    }

    /**
     * 添加事件监听器
     *
     * @param listener 监听器实例
     */
    public void addEventListener(SseEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        eventListeners.add(listener);
        log.debug("注册事件监听器: sessionId={}, listener={}", sessionId, listener.getListenerName());
    }

    /**
     * 移除事件监听器
     *
     * @param listener 监听器实例
     * @return true表示成功移除
     */
    public boolean removeEventListener(SseEventListener listener) {
        boolean removed = eventListeners.remove(listener);
        if (removed) {
            log.debug("移除事件监听器: sessionId={}, listener={}", sessionId, listener.getListenerName());
        }
        return removed;
    }

    /**
     * 添加消息钩子
     *
     * @param hook 钩子实例
     */
    public void addMessageHook(SseMessageHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Hook cannot be null");
        }
        messageHooks.add(hook);
        // 按优先级排序（升序）
        messageHooks.sort(Comparator.comparingInt(SseMessageHook::getPriority));
        log.debug("注册消息钩子: sessionId={}, hook={}, priority={}", sessionId, hook.getHookName(), hook.getPriority());
    }

    /**
     * 移除消息钩子
     *
     * @param hook 钩子实例
     * @return true表示成功移除
     */
    public boolean removeMessageHook(SseMessageHook hook) {
        boolean removed = messageHooks.remove(hook);
        if (removed) {
            log.debug("移除消息钩子: sessionId={}, hook={}", sessionId, hook.getHookName());
        }
        return removed;
    }

    /**
     * 触发事件
     *
     * @param eventType 事件类型
     * @param eventData 事件数据
     */
    private void fireEvent(SseEventType eventType, Object eventData) {
        log.trace("触发事件: sessionId={}, eventType={}, listeners={}", sessionId, eventType, eventListeners.size());

        for (SseEventListener listener : eventListeners) {
            // 检查监听器是否关注此事件
            if (!listener.isInterestedIn(eventType)) {
                continue;
            }

            try {
                listener.onEvent(eventType, this, eventData);
            } catch (Exception e) {
                log.error("事件监听器执行异常: sessionId={}, listener={}, eventType={}, error={}",
                        sessionId, listener.getListenerName(), eventType, e.getMessage(), e);
            }
        }
    }

    /**
     * 执行beforeSend钩子
     *
     * @param message 待发送的消息
     * @return true表示允许发送，false表示拦截
     */
    private boolean executeBeforeSendHooks(SseMessage message) {
        for (SseMessageHook hook : messageHooks) {
            try {
                boolean shouldContinue = hook.beforeSend(message);
                if (!shouldContinue) {
                    log.info("消息被钩子拦截: sessionId={}, hook={}, messageId={}",
                            sessionId, hook.getHookName(), message.getId());
                    return false;
                }
            } catch (Exception e) {
                log.error("beforeSend钩子执行异常: sessionId={}, hook={}, error={}",
                        sessionId, hook.getHookName(), e.getMessage(), e);
                // 钩子异常不中断流程
            }
        }
        return true;
    }

    /**
     * 执行afterSend钩子
     *
     * @param message 已发送的消息
     */
    private void executeAfterSendHooks(SseMessage message) {
        for (SseMessageHook hook : messageHooks) {
            try {
                hook.afterSend(message);
            } catch (Exception e) {
                log.error("afterSend钩子执行异常: sessionId={}, hook={}, error={}",
                        sessionId, hook.getHookName(), e.getMessage(), e);
                // 钩子异常不中断流程
            }
        }
    }

    /**
     * 从SseEventBuilder中提取信息构建SseMessage
     *
     * Spring SseEventBuilderImpl 实际结构（基于源码分析）：
     * - dataToSend: Set<DataWithMediaType> - 存储所有SSE内容
     *   - [0] "id:123\nevent:test\nretry:5000\n:comment\ndata:\n" (协议头)
     *   - [1] "实际内容" (纯数据)
     *   - [2] "\n" (终止符)
     *
     * 所有字段（id, event, retry, comment）都序列化在协议头字符串中
     *
     * @param builder SSE事件构建器
     * @return SseMessage对象
     */
    private SseMessage buildMessageFromEventBuilder(SseEventBuilder builder) {
        String eventId = null;
        String eventName = "message";  // 默认事件名
        String data = "[EventBuilder]";  // 默认数据占位符
        String comment = null;
        Long reconnectTime = null;
        Long dataSize = 0L;

        try {
            Class<?> builderClass = builder.getClass();

            // 1. 获取 dataToSend 字段（优先）或 data 字段（fallback）
            java.lang.reflect.Field dataField = findFieldInHierarchy(builderClass, "dataToSend");
            if (dataField == null) {
                dataField = findFieldInHierarchy(builderClass, "data");
            }

            if (dataField == null) {
                log.debug("未找到dataToSend或data字段，使用默认值");
                return createDefaultSseMessage(eventName, data, dataSize);
            }

            dataField.setAccessible(true);
            Object dataValue = dataField.get(builder);

            if (!(dataValue instanceof java.util.Set)) {
                log.debug("dataToSend字段不是Set类型: {}", dataValue == null ? "null" : dataValue.getClass());
                return createDefaultSseMessage(eventName, data, dataSize);
            }

            java.util.Set<?> dataSet = (java.util.Set<?>) dataValue;

            // 2. 遍历 dataSet，识别协议头和纯数据
            StringBuilder dataContent = new StringBuilder();
            String protocolHeader = null;

            for (Object item : dataSet) {
                if (item == null) {
                    continue;
                }

                // 提取 DataWithMediaType.data 字段
                try {
                    Class<?> itemClass = item.getClass();
                    java.lang.reflect.Field itemDataField = itemClass.getDeclaredField("data");
                    itemDataField.setAccessible(true);
                    Object itemData = itemDataField.get(item);

                    if (itemData == null) {
                        continue;
                    }

                    String dataStr = itemData.toString();

                    // 判断是否为协议头（包含SSE协议标记）
                    if (dataStr.contains("id:") || dataStr.contains("event:") ||
                        dataStr.contains("retry:") || dataStr.startsWith(":") ||
                        dataStr.contains("data:")) {

                        protocolHeader = dataStr;  // 保存协议头
                        log.trace("识别到SSE协议头: {}", dataStr.replace("\n", "\\n"));

                    } else if (!dataStr.trim().isEmpty()) {
                        // 纯数据内容
                        if (dataContent.length() > 0) {
                            dataContent.append("\n");
                        }
                        dataContent.append(dataStr);
                    }

                } catch (Exception e) {
                    log.trace("提取DataWithMediaType.data失败: {}", e.getMessage());
                }
            }

            // 3. 解析协议头，提取各个字段
            if (protocolHeader != null) {
                Map<String, String> fields = parseSseProtocolFields(protocolHeader);

                eventId = fields.get("id");

                if (fields.containsKey("event")) {
                    eventName = fields.get("event");
                }

                comment = fields.get("comment");

                if (fields.containsKey("retry")) {
                    try {
                        reconnectTime = Long.parseLong(fields.get("retry"));
                    } catch (NumberFormatException e) {
                        log.trace("retry字段解析失败: {}", fields.get("retry"));
                    }
                }

                log.trace("协议字段解析结果: id={}, event={}, retry={}, comment={}",
                         eventId, eventName, reconnectTime, comment);
            }

            // 4. 设置数据内容
            if (dataContent.length() > 0) {
                data = dataContent.toString();
                dataSize = (long) data.getBytes(StandardCharsets.UTF_8).length;
            }

            log.trace("从EventBuilder提取信息成功: sessionId={}, eventId={}, eventName={}, dataSize={}, comment={}, reconnectTime={}",
                    sessionId, eventId, eventName, dataSize, comment, reconnectTime);

        } catch (Exception e) {
            log.debug("从EventBuilder提取信息失败，使用默认值: sessionId={}, builderClass={}, error={}",
                    sessionId, builder.getClass().getName(), e.getMessage());

            // 开发环境打印详细的字段信息，帮助调试
            if (log.isDebugEnabled()) {
                logBuilderFields(builder);
            }
        }

        return createDefaultSseMessage(eventName, data, dataSize);
    }

    /**
     * 创建默认的SseMessage对象
     *
     * @param eventName 事件名
     * @param data 数据内容
     * @param dataSize 数据大小
     * @return SseMessage对象
     */
    private SseMessage createDefaultSseMessage(String eventName, String data, Long dataSize) {
        return SseMessage.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .eventName(eventName)
                .data(data)
                .messageType(eventName)
                .sequence(messageSequence.incrementAndGet())
                .dataSize(dataSize)
                .createTime(LocalDateTime.now())
                .success(false)
                .build();
    }

    /**
     * 在类继承层次中查找字段
     * 支持查找父类和接口中定义的字段
     *
     * @param clazz 起始类
     * @param fieldName 字段名
     * @return Field对象，未找到返回null
     */
    private java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        // 向上遍历类继承链
        while (currentClass != null && currentClass != Object.class) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
                return field;  // 找到字段，立即返回
            } catch (NoSuchFieldException e) {
                // 当前类没有，继续查找父类
                currentClass = currentClass.getSuperclass();
            }
        }

        return null;  // 未找到
    }

    /**
     * 解析 SSE 协议格式的字符串，提取各个字段
     *
     * 协议格式：
     * id:123\n
     * event:test\n
     * retry:5000\n
     * :comment text\n
     * data:\n
     *
     * @param protocolText SSE协议文本
     * @param fieldName 要提取的字段名（id, event, retry, comment）
     * @return 字段值，未找到返回 null
     */
    private String parseSseProtocolField(String protocolText, String fieldName) {
        if (protocolText == null || protocolText.isEmpty()) {
            return null;
        }

        // 按行分割
        String[] lines = protocolText.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // 处理 comment（特殊格式 ":comment text"）
            if ("comment".equals(fieldName) && line.startsWith(":")) {
                String value = line.substring(1).trim();
                return value.isEmpty() ? null : value;
            }

            // 处理其他字段（格式 "field:value"）
            if (line.startsWith(fieldName + ":")) {
                String value = line.substring(fieldName.length() + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }

        return null;
    }

    /**
     * 从 SSE 协议文本中提取所有字段
     *
     * @param protocolText SSE协议文本
     * @return Map包含所有解析的字段（id, event, retry, comment）
     */
    private Map<String, String> parseSseProtocolFields(String protocolText) {
        Map<String, String> fields = new HashMap<>();

        if (protocolText == null || protocolText.isEmpty()) {
            return fields;
        }

        // 提取 id
        String id = parseSseProtocolField(protocolText, "id");
        if (id != null) {
            fields.put("id", id);
        }

        // 提取 event（注意：协议中是 event，但 name() 方法生成的是 event:）
        String event = parseSseProtocolField(protocolText, "event");
        if (event != null) {
            fields.put("event", event);
        }

        // 提取 retry
        String retry = parseSseProtocolField(protocolText, "retry");
        if (retry != null) {
            fields.put("retry", retry);
        }

        // 提取 comment
        String comment = parseSseProtocolField(protocolText, "comment");
        if (comment != null) {
            fields.put("comment", comment);
        }

        return fields;
    }


    /**
     * 打印Builder的所有字段信息（调试用）
     * 仅在debug日志级别启用时调用
     *
     * @param builder EventBuilder实例
     */
    private void logBuilderFields(SseEventBuilder builder) {
        try {
            Class<?> clazz = builder.getClass();
            log.debug("=== SseEventBuilder字段分析 ===");
            log.debug("类名: {}", clazz.getName());

            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(builder);

                String valueStr;
                if (value == null) {
                    valueStr = "null";
                } else if (value instanceof java.util.Set) {
                    java.util.Set<?> set = (java.util.Set<?>) value;
                    valueStr = "Set[size=" + set.size() + "]";
                } else {
                    String str = value.toString();
                    valueStr = str.length() > 100 ? str.substring(0, 100) + "..." : str;
                }

                log.debug("  字段: {} | 类型: {} | 值: {}",
                        field.getName(),
                        field.getType().getSimpleName(),
                        valueStr);
            }
            log.debug("=== 字段分析结束 ===");
        } catch (Exception e) {
            log.debug("无法打印字段信息: {}", e.getMessage());
        }
    }

    /**
     * 设置连接状态
     * 状态变化时触发STATE_CHANGE事件
     *
     * @param newState 新状态
     */
    private void setState(SseConnectionState newState) {
        if (this.state == newState) {
            return; // 状态未变化，忽略
        }

        SseConnectionState oldState = this.state;
        this.state = newState;

        log.debug("SSE状态变化: sessionId={}, {} → {}", sessionId, oldState, newState);
        fireEvent(SseEventType.STATE_CHANGE, newState);
    }

    /**
     * 获取当前消息序号
     *
     * @return 当前序号
     */
    public int getCurrentSequence() {
        return messageSequence.get();
    }

    /**
     * 获取监听器数量
     *
     * @return 监听器数量
     */
    public int getListenerCount() {
        return eventListeners.size();
    }

    /**
     * 获取钩子数量
     *
     * @return 钩子数量
     */
    public int getHookCount() {
        return messageHooks.size();
    }

    /**
     * 缓存消息（供监听器调用）
     *
     * @param message 要缓存的消息
     */
    public void cacheMessage(SseMessage message) {
        if (message == null) {
            return;
        }
        synchronized (messageCache) {
            messageCache.add(message);

            // 内存保护：超过限制时触发强制保存事件
            if (messageCache.size() >= MAX_CACHE_SIZE) {
                log.warn("消息缓存达到上限，触发强制保存: sessionId={}, size={}",
                        sessionId, messageCache.size());
                // 触发STATE_CHANGE事件，传递特殊标识
                fireEvent(SseEventType.STATE_CHANGE, "CACHE_FULL");
            }
        }
    }

    /**
     * 获取并清空消息缓存（供监听器调用）
     *
     * @return 缓存的消息列表（副本）
     */
    public List<SseMessage> getAndClearMessageCache() {
        synchronized (messageCache) {
            List<SseMessage> messages = new ArrayList<>(messageCache);
            messageCache.clear();
            log.debug("清空消息缓存: sessionId={}, count={}", sessionId, messages.size());
            return messages;
        }
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存的消息数量
     */
    public int getCacheSize() {
        return messageCache.size();
    }

    /**
     * 判断是否有缓存的消息
     *
     * @return true表示有缓存
     */
    public boolean hasCachedMessages() {
        return !messageCache.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ManagedSseEmitter[sessionId=%s, requestId=%s, state=%s, listeners=%d, hooks=%d]",
                sessionId, requestId, state, eventListeners.size(), messageHooks.size());
    }
}
