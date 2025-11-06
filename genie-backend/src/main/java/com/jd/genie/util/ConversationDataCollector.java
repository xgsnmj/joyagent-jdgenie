package com.jd.genie.util;

import com.alibaba.fastjson.JSON;
import com.jd.genie.model.response.GptProcessResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话数据收集器（简化版）
 *
 * <p>直接收集 GptProcessResult 对象，不再进行数据提取和转换。
 * 所有收集的数据将被序列化为JSON格式，保存到数据库的 chat_message.metadata 字段。</p>
 *
 * <p>主要功能：
 * <ul>
 *   <li>收集所有 GptProcessResult 对象</li>
 *   <li>支持外部平台的原始内容收集</li>
 *   <li>将收集的数据序列化为JSON格式</li>
 * </ul>
 * </p>
 *
 * @author JD Genie
 * @since 3.0.0
 */
@Slf4j
@Data
public class ConversationDataCollector {

    /**
     * GptProcessResult 消息列表
     * 直接保存所有 GptProcessResult 对象，包含完整的 SSE 数据
     */
    private final List<GptProcessResult> messages = new ArrayList<>();

    /**
     * 外部平台的原始内容（用于 Coze/融汇等）
     * 外部智能体返回的是纯文本内容，不是结构化的 GptProcessResult
     */
    private final StringBuilder externalRawContent = new StringBuilder();

    /**
     * 收集消息数据（直接收集 GptProcessResult）
     *
     * @param result GptProcessResult 对象
     */
    public void collectMessage(GptProcessResult result) {
        if (result == null) {
            return;
        }

        try {
            messages.add(result);
            log.debug("收集 GptProcessResult - packageType: {}, finished: {}",
                    result.getPackageType(), result.isFinished());
        } catch (Exception e) {
            log.warn("收集 GptProcessResult 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取所有收集的消息（JSON 格式）
     * 用于保存到 chat_message.metadata 字段
     *
     * @return JSON 字符串，如果没有消息则返回 null
     */
    public String getAllMessagesJson() {
        if (messages.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("version", "3.0");  // 新版本格式标记
            result.put("format", "GptProcessResult");
            result.put("messageCount", messages.size());
            result.put("timestamp", System.currentTimeMillis());
            result.put("messages", messages);

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("序列化消息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 收集原始内容（用于外部智能体）
     * 外部智能体返回的是纯文本内容，不是结构化的 GptProcessResult
     *
     * @param content 原始文本内容
     */
    public void collectRawContent(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        try {
            externalRawContent.append(content);
            log.debug("收集原始内容成功 - length: {}", content.length());
        } catch (Exception e) {
            log.warn("收集原始内容失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取收集的所有原始内容合并后的字符串
     * 用于外部智能体的内容提取
     *
     * @return 合并后的完整内容
     */
    public String getRawContentAsString() {
        return externalRawContent.toString();
    }

}
