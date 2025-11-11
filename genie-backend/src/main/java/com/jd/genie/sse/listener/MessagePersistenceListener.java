package com.jd.genie.sse.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.service.IChatHistoryService;
import com.jd.genie.sse.ManagedSseEmitter;
import com.jd.genie.sse.SseEventType;
import com.jd.genie.sse.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息持久化监听器
 * 负责将多条SSE消息合并并保存为一条ChatMessage记录
 *
 * 工作流程：
 * 1. AFTER_SEND事件：缓存成功发送的消息
 * 2. DISCONNECTED/COMPLETION事件：将缓存的消息合并为一条记录并保存
 * 3. CACHE_FULL事件：强制保存（内存保护）
 *
 * 数据结构：
 * - ChatMessage.content：合并所有SSE消息的文本内容
 * - ChatMessage.metadata：完整SSE消息列表的JSON字符串
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
@Component
public class MessagePersistenceListener implements SseEventListener {

    @Autowired
    private IChatHistoryService chatHistoryService;

    @Override
    public void onEvent(SseEventType eventType, ManagedSseEmitter emitter, Object eventData) {
        switch (eventType) {
            case AFTER_SEND:
                handleAfterSend(emitter, eventData);
                break;

            case DISCONNECTED:
            case COMPLETION:
                handleConnectionClose(emitter, eventType);
                break;

            case STATE_CHANGE:
                if ("CACHE_FULL".equals(eventData)) {
                    handleCacheFull(emitter);
                }
                break;

            default:
                // 其他事件不处理
                break;
        }
    }

    /**
     * 处理AFTER_SEND事件
     * 缓存成功发送的消息
     */
    private void handleAfterSend(ManagedSseEmitter emitter, Object eventData) {
        if (!(eventData instanceof SseMessage)) {
            return;
        }

        SseMessage message = (SseMessage) eventData;

        // 只缓存成功发送的消息
        if (!message.isSuccess()) {
            log.debug("跳过失败消息: sessionId={}, messageId={}",
                    emitter.getSessionId(), message.getId());
            return;
        }

        // 缓存消息（不过滤事件类型，所有消息都保存）
        emitter.cacheMessage(message);
        log.trace("消息已缓存: sessionId={}, messageId={}, cacheSize={}",
                emitter.getSessionId(), message.getId(), emitter.getCacheSize());
    }

    /**
     * 处理连接关闭事件
     * 合并并保存缓存的消息
     */
    private void handleConnectionClose(ManagedSseEmitter emitter, SseEventType eventType) {
        if (!emitter.hasCachedMessages()) {
            log.debug("无缓存消息，跳过保存: sessionId={}", emitter.getSessionId());
            return;
        }

        log.info("连接关闭，准备保存消息: sessionId={}, eventType={}, cacheSize={}",
                emitter.getSessionId(), eventType, emitter.getCacheSize());

        // 异步保存
        saveMessageAsync(emitter);
    }

    /**
     * 处理缓存满事件
     * 强制保存以避免内存溢出
     */
    private void handleCacheFull(ManagedSseEmitter emitter) {
        log.warn("缓存已满，强制保存消息: sessionId={}, cacheSize={}",
                emitter.getSessionId(), emitter.getCacheSize());

        // 异步保存
        saveMessageAsync(emitter);
    }

    /**
     * 异步保存消息到数据库（带重试机制）
     * 将多条SSE消息合并为一条ChatMessage记录
     */
    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void saveMessageAsync(ManagedSseEmitter emitter) {
        String sessionId = emitter.getSessionId();
        List<SseMessage> sseMessages = emitter.getAndClearMessageCache();

        if (sseMessages.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 合并内容
            String mergedContent = mergeContent(sseMessages);

            // 构建metadata（完整SSE消息列表JSON）
            String metadata = buildMetadata(sseMessages);

            // 调用IChatHistoryService保存消息
            chatHistoryService.saveMessage(
                    sessionId,
                    "assistant",  // AI角色
                    mergedContent,
                    null,         // files
                    null,         // thought
                    null,         // tasks
                    null,         // plan
                    metadata      // metadata（包含所有SSE消息的JSON）
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("保存AI回复成功: sessionId={}, sseCount={}, contentLength={}, duration={}ms",
                    sessionId, sseMessages.size(), mergedContent.length(), duration);

        } catch (Exception e) {
            log.error("保存AI回复异常: sessionId={}, sseCount={}, error={}",
                    sessionId, sseMessages.size(), e.getMessage(), e);
            // 抛出异常，触发@Retryable重试
            throw e;
        }
    }

    /**
     * 合并所有SSE消息的内容
     * 策略：拼接所有data字段，智能提取文本内容
     *
     * @param sseMessages SSE消息列表
     * @return 合并后的内容文本
     */
    private String mergeContent(List<SseMessage> sseMessages) {
        StringBuilder contentBuilder = new StringBuilder();

        for (SseMessage message : sseMessages) {
            String data = message.getData();
            if (data == null || data.isEmpty()) {
                continue;
            }

            // 尝试从JSON中提取content字段
            String extractedText = extractTextFromData(data);

            // 拼接内容
            if (!extractedText.isEmpty()) {
                contentBuilder.append(extractedText);
            }
        }

        return contentBuilder.toString();
    }

    /**
     * 从data字段提取文本内容
     * 支持纯文本和JSON格式
     *
     * @param data 原始数据
     * @return 提取的文本
     */
    private String extractTextFromData(String data) {
        // 尝试解析JSON
        if (data.trim().startsWith("{")) {
            try {
                JSONObject json = JSON.parseObject(data);

                // 尝试多种字段名
                if (json.containsKey("content")) {
                    return json.getString("content");
                }
                if (json.containsKey("text")) {
                    return json.getString("text");
                }
                if (json.containsKey("message")) {
                    return json.getString("message");
                }
                if (json.containsKey("delta")) {
                    // OpenAI流式格式：{"delta": {"content": "..."}}
                    JSONObject delta = json.getJSONObject("delta");
                    if (delta != null && delta.containsKey("content")) {
                        return delta.getString("content");
                    }
                }

            } catch (Exception e) {
                log.trace("JSON解析失败，使用原始文本: data={}, error={}",
                        data.substring(0, Math.min(50, data.length())), e.getMessage());
            }
        }

        // 返回原始数据（纯文本或解析失败的JSON）
        return data;
    }

    /**
     * 构建metadata JSON字符串
     * 包含完整的SSE消息列表和统计信息
     *
     * @param sseMessages SSE消息列表
     * @return metadata JSON字符串
     */
    private String buildMetadata(List<SseMessage> sseMessages) {
        Map<String, Object> metadata = new HashMap<>();

        // 转换SSE消息为简化结构
        List<Map<String, Object>> messageList = new ArrayList<>();
        long totalSize = 0;

        for (SseMessage msg : sseMessages) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("eventName", msg.getEventName());
            msgMap.put("data", msg.getData());
            msgMap.put("sequence", msg.getSequence());
            msgMap.put("sendTime", msg.getSendTime());
            msgMap.put("dataSize", msg.getDataSize());

            messageList.add(msgMap);
            totalSize += (msg.getDataSize() != null ? msg.getDataSize() : 0);
        }

        // 构建metadata结构
        metadata.put("messages", messageList);
        metadata.put("totalMessages", sseMessages.size());
        metadata.put("totalSize", totalSize);
        metadata.put("firstMessageTime", sseMessages.get(0).getSendTime());
        metadata.put("lastMessageTime", sseMessages.get(sseMessages.size() - 1).getSendTime());

        // 序列化为JSON字符串
        return JSON.toJSONString(metadata);
    }

    @Override
    public boolean isInterestedIn(SseEventType eventType) {
        // 关注AFTER_SEND、DISCONNECTED、COMPLETION、STATE_CHANGE事件
        return eventType == SseEventType.AFTER_SEND
                || eventType == SseEventType.DISCONNECTED
                || eventType == SseEventType.COMPLETION
                || eventType == SseEventType.STATE_CHANGE;
    }

    @Override
    public String getListenerName() {
        return "MessagePersistenceListener";
    }
}
