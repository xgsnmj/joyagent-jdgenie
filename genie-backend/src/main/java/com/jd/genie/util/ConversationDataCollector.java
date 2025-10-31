package com.jd.genie.util;

import com.alibaba.fastjson.JSON;
import com.jd.genie.model.response.AgentResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话数据收集器
 *
 * <p>用于在SSE流式响应过程中收集完整的会话数据，包括思考过程、任务详情、计划信息等。
 * 收集的数据将被保存到数据库的chat_message表中，用于历史会话的完整还原。</p>
 *
 * <p>主要功能：
 * <ul>
 *   <li>收集AI的所有思考过程（planThought, toolThought等）</li>
 *   <li>收集任务执行的完整时间线</li>
 *   <li>收集执行计划信息</li>
 *   <li>将收集的数据序列化为JSON格式</li>
 * </ul>
 * </p>
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@Data
public class ConversationDataCollector {

    /**
     * 思考过程列表
     * 存储AI在执行过程中的所有思考内容，按时间顺序排列
     */
    private final List<ThoughtItem> thoughts = new ArrayList<>();

    /**
     * 任务详情列表
     * 存储任务执行的完整时间线，包括每个任务的状态和结果
     */
    private final List<TaskItem> tasks = new ArrayList<>();

    /**
     * 计划信息
     * 存储AI制定的执行计划（如果有）
     */
    private AgentResponse.Plan plan;

    /**
     * 额外元数据
     * 用于存储其他需要保存的信息
     */
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * 原始SSE消息列表（作为备份）
     * 保存所有原始的AgentResponse对象，用于：
     * 1. 调试和问题排查
     * 2. 当前端上报失败时作为fallback数据源
     */
    private final List<AgentResponse> rawMessages = new ArrayList<>();

    /**
     * 思考项数据结构
     */
    @Data
    public static class ThoughtItem {
        /**
         * 思考类型（plan/tool/other）
         */
        private String type;

        /**
         * 思考内容
         */
        private String content;

        /**
         * 时间戳
         */
        private String timestamp;

        public ThoughtItem(String type, String content, String timestamp) {
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    /**
     * 任务项数据结构
     */
    @Data
    public static class TaskItem {
        /**
         * 任务ID
         */
        private String id;

        /**
         * 消息ID
         */
        private String messageId;

        /**
         * 消息类型
         */
        private String messageType;

        /**
         * 任务描述
         */
        private String task;

        /**
         * 任务摘要
         */
        private String taskSummary;

        /**
         * 任务结果
         */
        private String result;

        /**
         * 结果详情
         */
        private Map<String, Object> resultMap;

        /**
         * 是否完成
         */
        private Boolean finish;

        /**
         * 工具调用结果（如果有）
         */
        private AgentResponse.ToolResult toolResult;

        /**
         * 时间戳
         */
        private String timestamp;
    }

    /**
     * 收集消息数据
     *
     * @param response SSE响应对象
     */
    public void collectMessage(AgentResponse response) {
        try {
            // 保存原始消息（作为备份）
            rawMessages.add(response);

            String messageType = response.getMessageType();
            String messageTime = response.getMessageTime();

            // 收集思考过程
            if (response.getPlanThought() != null && !response.getPlanThought().isEmpty()) {
                thoughts.add(new ThoughtItem("plan", response.getPlanThought(), messageTime));
            }
            if (response.getToolThought() != null && !response.getToolThought().isEmpty()) {
                thoughts.add(new ThoughtItem("tool", response.getToolThought(), messageTime));
            }

            // 收集计划信息（只保存最新的计划）
            if (response.getPlan() != null) {
                this.plan = response.getPlan();
            }

            // 收集任务详情
            if (messageType != null && (
                    messageType.equals("task") ||
                    messageType.equals("result") ||
                    messageType.equals("tool"))) {

                TaskItem taskItem = new TaskItem();
                taskItem.setId(response.getRequestId());
                taskItem.setMessageId(response.getMessageId());
                taskItem.setMessageType(messageType);
                taskItem.setTask(response.getTask());
                taskItem.setTaskSummary(response.getTaskSummary());
                taskItem.setResult(response.getResult());
                taskItem.setResultMap(response.getResultMap());
                taskItem.setFinish(response.getFinish());
                taskItem.setToolResult(response.getToolResult());
                taskItem.setTimestamp(messageTime);

                tasks.add(taskItem);
            }

        } catch (Exception e) {
            log.warn("收集会话数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取思考过程的JSON字符串
     *
     * <p>直接合并所有思考内容，不添加类型标记（如[plan]、[tool]等），
     * 保持与实时SSE流输出一致，确保历史会话显示与实时对话体验相同。</p>
     *
     * @return JSON字符串，如果没有思考内容则返回null
     */
    public String getThoughtJson() {
        if (thoughts.isEmpty()) {
            return null;
        }

        // 直接合并思考内容，保持与实时对话一致，不添加类型标记
        StringBuilder combined = new StringBuilder();
        for (ThoughtItem thought : thoughts) {
            if (combined.length() > 0) {
                combined.append("\n\n");
            }
            // 只保存纯净的思考内容，不添加 [type] 前缀
            combined.append(thought.getContent());
        }

        return combined.toString();
    }

    /**
     * 获取任务详情的JSON字符串
     *
     * @return JSON字符串，如果没有任务则返回null
     */
    public String getTasksJson() {
        if (tasks.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(tasks);
    }

    /**
     * 获取计划信息的JSON字符串
     *
     * @return JSON字符串，如果没有计划则返回null
     */
    public String getPlanJson() {
        if (plan == null) {
            return null;
        }
        return JSON.toJSONString(plan);
    }

    /**
     * 获取元数据的JSON字符串
     *
     * <p>优先返回用户自定义的元数据。如果元数据为空，则返回包含原始SSE消息的备份数据，
     * 用于在前端上报失败时提供fallback数据源。</p>
     *
     * @return JSON字符串，如果没有元数据且没有原始消息则返回null
     */
    public String getMetadataJson() {
        // 如果有自定义元数据，优先返回
        if (!metadata.isEmpty()) {
            return JSON.toJSONString(metadata);
        }

        // 如果没有自定义元数据，但有原始消息，则将原始消息作为fallback
        if (!rawMessages.isEmpty()) {
            Map<String, Object> fallbackMetadata = new HashMap<>();
            fallbackMetadata.put("rawMessages", rawMessages);
            fallbackMetadata.put("source", "backend-fallback");
            fallbackMetadata.put("version", "1.0");
            fallbackMetadata.put("timestamp", System.currentTimeMillis());
            fallbackMetadata.put("messageCount", rawMessages.size());
            return JSON.toJSONString(fallbackMetadata);
        }

        return null;
    }

    /**
     * 添加元数据
     *
     * @param key 键
     * @param value 值
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}
