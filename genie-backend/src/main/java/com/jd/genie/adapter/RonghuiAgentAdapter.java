package com.jd.genie.adapter;

import com.alibaba.fastjson.JSON;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 融汇（阿里点金）智能体适配器
 * 对接融汇平台的智能体服务
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */
@Slf4j
@Component
public class RonghuiAgentAdapter implements AgentAdapter {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();



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
        log.info("融汇适配器处理请求（使用自定义emitter） - 会话ID: {}, 端点: {}, 外部会话ID: {}",
                sessionId, apiEndpoint, externalSessionId);

        ChatResponse response = new ChatResponse();
        response.setEmitter(customEmitter);

        // 异步处理
        new Thread(() -> {
            try {
                // TODO: 根据融汇（阿里点金）API文档构建请求
                JSONObject requestBody = buildRonghuiRequest(userMessage, history, externalSessionId);

                RequestBody body = RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(apiEndpoint)
                        .post(body)
                        .addHeader("Authorization", apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Call call = httpClient.newCall(request);
                activeCalls.put(sessionId, call);

                Response httpResponse = call.execute();

                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "Unknown error";
                    log.error("融汇API请求失败 - 状态码: {}, 响应: {}", httpResponse.code(), errorBody);
                    throw new RuntimeException("融汇API请求失败: " + httpResponse.code());
                }

                // TODO: 从响应中提取外部会话ID
                String newExternalSessionId = extractExternalSessionId(httpResponse);
                response.setExternalSessionId(newExternalSessionId);

                // 处理SSE流（使用自定义emitter）
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
            // TODO: 根据融汇的响应格式提取内容
            JSONObject json = JSON.parseObject(rawResponse);
            String content = json.getString("content"); // 字段名需确认
            message.setContent(content != null ? content : rawResponse);
        } catch (Exception e) {
            log.error("解析融汇响应失败", e);
            message.setContent(rawResponse);
        }

        message.setMessageFormat("ronghui");
        message.setRawContent(rawResponse);
        return message;
    }

    /**
     * 构建融汇API请求体
     * TODO: 根据融汇API文档调整请求格式
     */
    private JSONObject buildRonghuiRequest(String userMessage, List<ChatMessage> history, String externalSessionId) {
        JSONObject request = new JSONObject();

        // TODO: 根据融汇API文档构建请求体

        // 1. 如果有外部会话ID，添加到请求中
        if (externalSessionId != null) {
            request.put("session_id", externalSessionId); // 字段名需确认
        }

        // 2. 构建消息历史
        List<JSONObject> messages = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", msg.getRole());
                msgObj.put("content", msg.getContent());
                messages.add(msgObj);
            }
        }

        // 3. 添加当前用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        request.put("messages", messages);
        request.put("stream", true); // 启用流式响应

        return request;
    }

    /**
     * 从响应中提取外部会话ID
     * TODO: 根据融汇API实际返回格式调整
     */
    private String extractExternalSessionId(Response response) {
        // 从响应头或响应体中提取会话ID
        String sessionIdFromHeader = response.header("X-Session-ID");
        if (sessionIdFromHeader != null) {
            return sessionIdFromHeader;
        }
        return null;
    }

    /**
     * 处理融汇的SSE流
     * TODO: 根据融汇的SSE事件格式调整
     */
    private void processRonghuiSSEStream(InputStream inputStream, String sessionId, SseEmitter emitter) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int sequence = 0;

            while ((line = reader.readLine()) != null) {
                // TODO: 根据融汇的SSE格式解析

                if (line.startsWith("data: ")) {
                    String data = line.substring(6);

                    if ("[DONE]".equals(data)) {
                        log.debug("融汇SSE流结束");
                        break;
                    }

                    // 转发给前端
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(data));
                }
            }
        } catch (Exception e) {
            log.error("处理融汇SSE流失败", e);
            throw new RuntimeException(e);
        }
    }

}
