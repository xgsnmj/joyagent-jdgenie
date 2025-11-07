package com.jd.genie.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.entity.ChatMessage;
import com.jd.genie.service.AgentProviderService;
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

/**
 * 融汇智能体适配器
 * 对接阿里巴巴融汇平台的智能体服务
 *
 * API文档参考: 融汇平台API文档
 *
 * @author JDGenie Team
 * @since 2025-01-03
 * @updated 2025-11-07 完整实现
 */
@Slf4j
@Component
public class RonghuiAgentAdapter implements AgentAdapter {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();

    // 融汇API基础URL
    private static final String RONGHUI_API_URL = "https://qagent.rxhui.com/gateway/qagentService/chat";
    private static final String CONTENT_TYPE = "application/json";

    @Override
    public ChatResponse sendChatRequest(String sessionId,
                                       String userMessage,
                                       List<ChatMessage> history,
                                       String externalSessionId,
                                       SseEmitter customEmitter,
                                        AgentProvider agentProvider) {
        log.info("融汇适配器处理请求 - 会话ID: {}, Bot ID (app_id): {}, 外部会话ID: {}",
                sessionId, agentProvider.getBotId(), externalSessionId);

        // 验证必填参数
        if (agentProvider.getBotId() == null || agentProvider.getBotId().trim().isEmpty()) {
            throw new IllegalArgumentException("融汇平台的应用ID（app_id）不能为空");
        }

        if (agentProvider.getApiKey() == null || agentProvider.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("融汇平台的API密钥不能为空");
        }

        ChatResponse response = new ChatResponse();
        response.setEmitter(customEmitter);

        // 异步处理
        new Thread(() -> {
            try {
                // 使用apiEndpoint或默认URL
                String apiUrl = (agentProvider.getApiEndpoint() != null && !agentProvider.getApiEndpoint().trim().isEmpty())
                        ? agentProvider.getApiEndpoint() : RONGHUI_API_URL;

                log.info("融汇API URL: {}", apiUrl);

                // 构建请求体
                JSONObject requestBody = buildRonghuiRequest(userMessage, agentProvider.getBotId(), sessionId);

                // 获取AgentProvider配置以获取租户信息
                // 注意：这里需要通过某种方式获取AgentProvider，暂时使用空值，实际应该从调用链传入
                // TODO: 重构接口以传入AgentProvider或租户信息

                // 创建HTTP请求（带租户Headers）
                Request.Builder requestBuilder = new Request.Builder()
                        .url(apiUrl)
                        .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse(CONTENT_TYPE)))
                        .addHeader("Authorization", "Bearer " + agentProvider.getApiKey())
                        .addHeader("Content-Type", CONTENT_TYPE)
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("Connection", "keep alive");

                // 添加融汇专用Headers（需要从AgentProvider获取）
                // 注意：这里使用默认值，实际应该从数据库读取
                requestBuilder.addHeader("tenantid", agentProvider.getTenantId());  // 默认租户ID
                requestBuilder.addHeader("login-userid", agentProvider.getLoginUserId());   // 默认用户ID
                requestBuilder.addHeader("login-deptid", agentProvider.getLoginDeptId()); // 默认部门ID
                requestBuilder.addHeader("login-username", agentProvider.getLoginUsername()); // 默认用户名

                Request request = requestBuilder.build();

                Call call = httpClient.newCall(request);
                activeCalls.put(sessionId, call);

                // 执行请求
                Response httpResponse = call.execute();

                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "Unknown error";
                    log.error("融汇API请求失败 - 状态码: {}, 响应: {}", httpResponse.code(), errorBody);
                    throw new RuntimeException("融汇API请求失败: " + httpResponse.code());
                }

                // 处理SSE流式响应
                processRonghuiSSEStream(httpResponse.body().byteStream(), sessionId, customEmitter);

                customEmitter.send(SseEmitter.event().name("done").data("[DONE]"));
                customEmitter.complete();

            } catch (Exception e) {
                log.error("融汇适配器处理失败", e);
                try {
                    customEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("融汇智能体响应失败: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                customEmitter.completeWithError(e);
            } finally {
                activeCalls.remove(sessionId);
            }
        }).start();

        return response;
    }

    @Override
    public void terminateChat(String sessionId) {
        log.info("终止融汇会话: {}", sessionId);

        Call call = activeCalls.get(sessionId);
        if (call != null) {
            call.cancel();
            activeCalls.remove(sessionId);
        }
    }

    @Override
    public String getProviderType() {
        return "ronghui";
    }

    @Override
    public ChatMessage formatMessage(String rawResponse, String messageFormat) {
        ChatMessage message = new ChatMessage();

        try {
            JSONObject json = JSON.parseObject(rawResponse);

            // 融汇响应格式：{ "success": true, "data": { "response": "...", ... } }
            if (json.containsKey("data")) {
                JSONObject data = json.getJSONObject("data");
                String content = data.getString("data"); // 实际内容字段
                message.setContent(content != null ? content : rawResponse);
            } else if (json.containsKey("content")) {
                String content = json.getString("content");
                message.setContent(content != null ? content : rawResponse);
            } else {
                message.setContent(rawResponse);
            }

        } catch (Exception e) {
            log.error("解析融汇响应失败", e);
            message.setContent(rawResponse);
        }

        message.setMessageFormat("ronghui");
        message.setRawContent(rawResponse);
        return message;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建融汇API请求体
     *
     * @param userMessage 用户消息
     * @param appId 应用ID（botId）
     * @param externalSessionId 外部会话ID（格式：session_id:group_id）
     * @return 请求体JSON
     */
    private JSONObject buildRonghuiRequest(String userMessage, String appId, String sessionId) {
        JSONObject request = new JSONObject();

        // 必填参数
        request.put("app_id", appId);
        request.put("question", userMessage);
        request.put("inputs", new JSONObject());  // 附加信息，可选
        request.put("stream", true);  // 启用流式输出
        request.put("session_id", sessionId);
        request.put("group_id", sessionId);

        return request;
    }

    /**
     * 处理融汇的SSE流式响应
     *
     * 融汇响应结构：
     * {
     *   "event": "message|error|finish",
     *   "data": {
     *     "app_id": "97",
     *     "session_id": "...",
     *     "group_id": "...",
     *     "question": "...",
     *     "action": "roger|intermediate|reply|finish|error|region_begin|region_finish",
     *     "result_type": "文本",
     *     "output_mode": "stream",
     *     "data": "答案内容",
     *     "reference": {}
     *   }
     * }
     */
    private void processRonghuiSSEStream(InputStream inputStream, String sessionId, SseEmitter emitter) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            String currentEvent = null;
            StringBuilder dataBuffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    // 空行表示一个SSE事件结束
                    if (currentEvent != null && dataBuffer.length() > 0) {
                        processRonghuiSSEEvent(currentEvent, dataBuffer.toString(), emitter);
                        currentEvent = null;
                        dataBuffer.setLength(0);
                    }
                    continue;
                }

                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append("\n");
                    }
                    dataBuffer.append(data);
                }
            }

            // 处理最后一个事件
            if (currentEvent != null && dataBuffer.length() > 0) {
                processRonghuiSSEEvent(currentEvent, dataBuffer.toString(), emitter);
            }

        } catch (Exception e) {
            log.error("处理融汇SSE流失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理单个融汇SSE事件
     *
     * @param event 事件类型
     * @param data 事件数据（JSON字符串）
     * @param emitter SSE发射器
     */
    private void processRonghuiSSEEvent(String event, String data, SseEmitter emitter) throws Exception {
        try {
            if ("[DONE]".equals(data) || data.isEmpty()) {
                return;
            }

            JSONObject eventData = JSON.parseObject(data);
            JSONObject dataObj = eventData.getJSONObject("data");

            if (dataObj == null) {
                return;
            }

            String action = dataObj.getString("action");

            // 根据action类型处理
            switch (action) {
                case "roger":
                    // 服务端已收到请求
                    log.debug("融汇服务端已收到请求");
                    break;

                case "intermediate":
                case "reply":
                    // 发送消息内容
                    String content = dataObj.getString("data");
                    if (content != null && !content.isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(content));
                    }
                    break;

                case "finish":
                    // 对话成功结束
                    log.info("融汇对话完成");

                    // 提取session_id和group_id
                    String sessionIdStr = dataObj.getString("session_id");
                    String groupId = dataObj.getString("group_id");

                    if (sessionIdStr != null && groupId != null) {
                        log.info("融汇获取到session_id: {}, group_id: {}", sessionIdStr, groupId);
                        // 通过特殊事件发送给前端保存
                        emitter.send(SseEmitter.event()
                                .name("session_id")
                                .data(sessionIdStr + ":" + groupId));
                    }
                    break;

                case "error":
                    // 对话失败
                    String errorMsg = dataObj.getString("data");
                    log.error("融汇对话错误: {}", errorMsg);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("融汇错误: " + (errorMsg != null ? errorMsg : "未知错误")));
                    break;

                case "region_begin":
                    // 回答区（工具/智能体）开始执行
                    log.debug("融汇回答区开始执行");
                    // 可以发送状态通知到前端
                    break;

                case "region_finish":
                    // 回答区（工具/智能体）执行完成
                    log.debug("融汇回答区执行完成");
                    // 可以发送状态通知到前端
                    break;

                default:
                    log.warn("未知的融汇action类型: {}", action);
                    break;
            }

        } catch (Exception e) {
            log.error("解析融汇SSE事件失败: event={}, data={}", event, data, e);
            throw e;
        }
    }
}
