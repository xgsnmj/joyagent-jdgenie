package com.jd.genie.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Coze智能体适配器
 * 对接Coze平台的智能体服务
 *
 * API文档参考：
 * - 创建会话：POST /v1/conversation/create
 * - 发起对话：POST /v3/chat?conversation_id={conversation_id}
 * - 取消对话：POST /v3/chat/cancel
 *
 * @author JDGenie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class CozeAgentAdapter implements AgentAdapter {

    // HTTP客户端配置（设置合理的超时时间）
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 存储活跃的HTTP调用，用于取消
    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();

    // 存储chat_id，用于取消对话
    private final Map<String, String> chatIds = new ConcurrentHashMap<>();

    @Override
    public ChatResponse sendChatRequest(String sessionId,
                                       String userMessage,
                                       List<ChatMessage> history,
                                       String apiEndpoint,
                                       String apiKey,
                                       String botId,
                                       String externalSessionId) {
        log.info("Coze适配器处理请求 - 会话ID: {}, Bot ID: {}, 外部会话ID: {}",
                sessionId, botId, externalSessionId);

        // 验证必填参数
        if (botId == null || botId.trim().isEmpty()) {
            throw new IllegalArgumentException("Coze平台的Bot ID不能为空");
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        ChatResponse response = new ChatResponse();
        response.setEmitter(emitter);

        // 异步处理
        new Thread(() -> {
            try {
                String conversationId = externalSessionId;

                // 步骤1：如果没有外部会话ID，创建新会话
                if (conversationId == null || conversationId.trim().isEmpty()) {
                    log.info("创建新的Coze会话 - Bot ID: {}", botId);
                    conversationId = createConversation(apiEndpoint, apiKey, botId);
                    log.info("Coze会话创建成功 - Conversation ID: {}", conversationId);
                    response.setExternalSessionId(conversationId);
                }

                // 步骤2：发起Chat请求
                String chatApiUrl = buildChatUrl(apiEndpoint, conversationId);
                log.info("发起Coze对话 - URL: {}, Conversation ID: {}", chatApiUrl, conversationId);

                JSONObject requestBody = buildChatRequest(botId, userMessage);

                RequestBody body = RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(chatApiUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Call call = httpClient.newCall(request);
                activeCalls.put(sessionId, call);

                Response httpResponse = call.execute();

                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "Unknown error";
                    log.error("Coze API请求失败 - 状态码: {}, 响应: {}", httpResponse.code(), errorBody);
                    throw new RuntimeException("Coze API请求失败: " + httpResponse.code() + " - " + errorBody);
                }

                // 步骤3：处理SSE流式响应
                processCozeSSEStream(httpResponse.body().byteStream(), sessionId, emitter);

                // 步骤4：发送完成信号
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Coze适配器处理失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Coze智能体响应失败: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            } finally {
                activeCalls.remove(sessionId);
                chatIds.remove(sessionId);
            }
        }).start();

        return response;
    }

    /**
     * 发送聊天请求（支持自定义emitter）
     * 用于ExternalAgentExecutor的数据收集场景
     */
    @Override
    public ChatResponse sendChatRequest(String sessionId,
                                       String userMessage,
                                       List<ChatMessage> history,
                                       String apiEndpoint,
                                       String apiKey,
                                       String botId,
                                       String externalSessionId,
                                       SseEmitter customEmitter) {
        log.info("Coze适配器处理请求（使用自定义emitter） - 会话ID: {}, Bot ID: {}, 外部会话ID: {}",
                sessionId, botId, externalSessionId);

        // 验证必填参数
        if (botId == null || botId.trim().isEmpty()) {
            throw new IllegalArgumentException("Coze平台的Bot ID不能为空");
        }

        ChatResponse response = new ChatResponse();
        response.setEmitter(customEmitter);

        // 异步处理
        new Thread(() -> {
            try {
                String conversationId = externalSessionId;

                // 步骤1：如果没有外部会话ID，创建新会话
                if (conversationId == null || conversationId.trim().isEmpty()) {
                    log.info("创建新的Coze会话 - Bot ID: {}", botId);
                    conversationId = createConversation(apiEndpoint, apiKey, botId);
                    log.info("Coze会话创建成功 - Conversation ID: {}", conversationId);
                    response.setExternalSessionId(conversationId);
                }

                // 步骤2：发起Chat请求
                String chatApiUrl = buildChatUrl(apiEndpoint, conversationId);
                log.info("发起Coze对话 - URL: {}, Conversation ID: {}", chatApiUrl, conversationId);

                JSONObject requestBody = buildChatRequest(botId, userMessage);

                RequestBody body = RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(chatApiUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Call call = httpClient.newCall(request);
                activeCalls.put(sessionId, call);

                Response httpResponse = call.execute();

                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "Unknown error";
                    log.error("Coze API请求失败 - 状态码: {}, 响应: {}", httpResponse.code(), errorBody);
                    throw new RuntimeException("Coze API请求失败: " + httpResponse.code() + " - " + errorBody);
                }

                // 步骤3：处理SSE流式响应（使用自定义emitter）
                processCozeSSEStream(httpResponse.body().byteStream(), sessionId, customEmitter);

                // 步骤4：发送完成信号
                customEmitter.send(SseEmitter.event().name("done").data("[DONE]"));
                customEmitter.complete();

            } catch (Exception e) {
                log.error("Coze适配器处理失败", e);
                try {
                    customEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("Coze智能体响应失败: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                customEmitter.completeWithError(e);
            } finally {
                activeCalls.remove(sessionId);
                chatIds.remove(sessionId);
            }
        }).start();

        return response;
    }

    @Override
    public void terminateChat(String sessionId) {
        log.info("终止Coze会话: {}", sessionId);

        // 取消HTTP调用
        Call call = activeCalls.get(sessionId);
        if (call != null) {
            call.cancel();
            activeCalls.remove(sessionId);
        }

        // TODO: 调用Coze取消对话接口
        // String chatId = chatIds.get(sessionId);
        // if (chatId != null) {
        //     cancelChat(conversationId, chatId);
        // }

        chatIds.remove(sessionId);
    }

    @Override
    public String getProviderType() {
        return "coze";
    }

    @Override
    public ChatMessage formatMessage(String rawResponse, String messageFormat) {
        ChatMessage message = new ChatMessage();

        try {
            // Coze的消息格式提取
            JSONObject json = JSON.parseObject(rawResponse);
            String content = json.getString("content");
            message.setContent(content != null ? content : rawResponse);
        } catch (Exception e) {
            log.error("解析Coze响应失败", e);
            message.setContent(rawResponse);
        }

        message.setMessageFormat("coze");
        message.setRawContent(rawResponse);
        return message;
    }

    /**
     * 创建Coze会话
     * API: POST {{host}}/v1/conversation/create
     *
     * @param apiEndpoint API端点（基础URL）
     * @param apiKey API密钥
     * @param botId Bot ID
     * @return Conversation ID
     */
    private String createConversation(String apiEndpoint, String apiKey, String botId) throws Exception {
        // 构建创建会话的URL
        String baseUrl = apiEndpoint.endsWith("/") ? apiEndpoint.substring(0, apiEndpoint.length() - 1) : apiEndpoint;
        String createUrl = baseUrl + "/v1/conversation/create";

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("bot_id", botId);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(createUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("创建Coze会话失败 - 状态码: {}, 响应: {}", response.code(), errorBody);
                throw new RuntimeException("创建Coze会话失败: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("创建会话响应: {}", responseBody);

            JSONObject jsonResponse = JSON.parseObject(responseBody);

            // 检查响应状态
            Integer code = jsonResponse.getInteger("code");
            if (code != null && code != 0) {
                String msg = jsonResponse.getString("msg");
                throw new RuntimeException("创建Coze会话失败: " + msg);
            }

            // 提取conversation_id
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                throw new RuntimeException("创建Coze会话失败: 响应中没有data字段");
            }

            String conversationId = data.getString("id");
            if (conversationId == null || conversationId.trim().isEmpty()) {
                throw new RuntimeException("创建Coze会话失败: 响应中没有conversation_id");
            }

            return conversationId;
        }
    }

    /**
     * 构建Chat API URL
     * 格式：{{host}}/v3/chat?conversation_id={{conversation_id}}
     */
    private String buildChatUrl(String apiEndpoint, String conversationId) {
        String baseUrl = apiEndpoint.endsWith("/") ? apiEndpoint.substring(0, apiEndpoint.length() - 1) : apiEndpoint;
        return baseUrl + "/v3/chat?conversation_id=" + conversationId;
    }

    /**
     * 构建Chat请求体
     *
     * @param botId Bot ID
     * @param userMessage 用户消息
     * @return 请求体JSON
     */
    private JSONObject buildChatRequest(String botId, String userMessage) {
        JSONObject request = new JSONObject();

        // 必填字段
        request.put("bot_id", botId);
        request.put("user_id", "jdgenie_user"); // 使用固定的用户ID，可以后续改为动态
        request.put("stream", true); // 启用流式响应

        // 构建用户消息
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        userMsg.put("content_type", "text");
        messages.add(userMsg);

        request.put("additional_messages", messages);

        log.debug("Coze请求体: {}", request.toJSONString());
        return request;
    }

    /**
     * 处理Coze的SSE流式响应
     *
     * SSE事件类型：
     * - conversation.chat.created: 对话开始
     * - conversation.chat.in_progress: 服务端处理中
     * - conversation.message.delta: 增量消息（主要内容）
     * - conversation.message.completed: 消息回复完成
     * - conversation.chat.completed: 对话完成
     * - conversation.chat.failed: 对话失败
     * - conversation.chat.requires_action: 对话中断
     * - error: 错误事件
     * - done: 流式返回结束
     */
    private void processCozeSSEStream(InputStream inputStream, String sessionId, SseEmitter emitter) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int sequence = 0;
            String currentEvent = null;
            StringBuilder messageContent = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                log.debug("Coze SSE行: {}", line);

                // 解析事件类型
                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                    continue;
                }

                // 解析数据
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();

                    // 检查结束标记
                    if ("\"[DONE]\"".equals(data)) {
                        log.debug("Coze SSE流结束");
                        break;
                    }

                    try {
                        JSONObject eventData = JSON.parseObject(data);

                        // 提取chat_id（用于取消对话）
                        String chatId = eventData.getString("id");
                        if (chatId != null) {
                            chatIds.put(sessionId, chatId);
                        }

                        // 根据事件类型处理
                        if ("conversation.message.delta".equals(currentEvent)) {
                            // 增量消息 - 提取内容并发送
//                            String content = extractMessageContent(eventData);

                                // 转发给前端
                                emitter.send(SseEmitter.event()
                                        .name("conversation.message.delta")
                                        .data(data));
                                emitter.send(data);

                        } else if ("conversation.chat.failed".equals(currentEvent)) {
                            // 对话失败
                            String errorMsg = eventData.getString("last_error");
                            log.error("Coze对话失败: {}", errorMsg);

                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("Coze对话失败: " + errorMsg));
                        } else {
                            // 其他事件类型，记录日志
                            log.debug("Coze事件: {} - 数据: {}", currentEvent, data);
                        }

                    } catch (Exception e) {
                        log.error("解析Coze SSE数据失败: {}", data, e);
                    }
                }
            }

            log.info("Coze对话完成 - 会话ID: {}, 总消息内容长度: {}", sessionId, messageContent.length());

        } catch (Exception e) {
            log.error("处理Coze SSE流失败", e);
            throw new RuntimeException("处理Coze SSE流失败", e);
        }
    }

    /**
     * 从事件数据中提取消息内容
     *
     * Coze的消息结构可能是：
     * {
     *   "type": "answer",
     *   "content": "消息内容"
     * }
     */
    private String extractMessageContent(JSONObject eventData) {
        try {
            // 尝试从content字段提取
            String content = eventData.getString("content");
            if (content != null && !content.isEmpty()) {
                return content;
            }

            // 尝试从delta字段提取（部分API可能使用delta）
            JSONObject delta = eventData.getJSONObject("delta");
            if (delta != null) {
                content = delta.getString("content");
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("提取消息内容失败", e);
            return null;
        }
    }

}
