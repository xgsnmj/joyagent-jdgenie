package com.jd.genie.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.core.http.HttpMethod;
import com.aliyun.sdk.gateway.pop.clients.CommonAsyncClient;
import com.aliyun.sdk.gateway.pop.models.CommonRequest;
import com.jd.genie.entity.AgentProvider;
import com.jd.genie.entity.ChatMessage;
import darabonba.core.ResponseIterable;
import darabonba.core.ResponseIterator;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通义点金智能体适配器（SSE流式版本）
 * 基于阿里云官方Gateway POP SDK实现真正的流式响应
 *
 * API文档: https://api.aliyun.com/api/DianJin/2024-06-28/RunAgent
 * PDF示例: 通义点金SSE SDK 调用示例.pdf 第10页
 *
 * @author JDGenie Team
 * @since 2025-01-07
 */
@Slf4j
@Component
public class TongyiAgentAdapter implements AgentAdapter {

    // 存储活跃的客户端（用于会话管理）
    private final Map<String, CommonAsyncClient> activeClients = new ConcurrentHashMap<>();

    // 通义点金端点
    private static final String DIANJIN_ENDPOINT = "dianjin.cn-beijing.aliyuncs.com";
    private static final String DIANJIN_REGION = "cn-beijing";
    private static final String API_VERSION = "2024-06-28";
    private static final String PRODUCT_NAME = "Dianjin";

    // ==================== 私有辅助方法 ====================

    /**
     * 创建通义点金Gateway客户端
     *
     * @param accessKeyId 访问密钥ID
     * @param accessKeySecret 访问密钥Secret
     * @return CommonAsyncClient 客户端实例
     */
    private CommonAsyncClient createGatewayClient(String accessKeyId, String accessKeySecret) {
        log.info("创建通义点金Gateway客户端 - Region: {}, Endpoint: {}", DIANJIN_REGION, DIANJIN_ENDPOINT);

        // 创建静态凭证提供者
        StaticCredentialProvider provider = StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build()
        );

        // 构建客户端
        return CommonAsyncClient.builder()
            .region(DIANJIN_REGION)
            .credentialsProvider(provider)
            .overrideConfiguration(
                ClientOverrideConfiguration.create()
                    .setEndpointOverride(DIANJIN_ENDPOINT)
            )
            .build();
    }

    /**
     * 从apiKey中解析accessKeyId和accessKeySecret
     * 格式：accessKeyId:accessKeySecret
     *
     * @param apiKey API密钥字符串
     * @return [accessKeyId, accessKeySecret]
     */
    private String[] parseCredentials(String apiKey) {
        if (apiKey == null || !apiKey.contains(":")) {
            throw new IllegalArgumentException(
                "通义点金API密钥格式错误，应为：accessKeyId:accessKeySecret");
        }
        String[] parts = apiKey.split(":", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException(
                "通义点金API密钥格式错误，accessKeyId或accessKeySecret不能为空");
        }
        return parts;
    }

    /**
     * 构建RunAgent API请求
     *
     * @param workspaceId 工作空间ID
     * @param botId Bot ID
     * @param userContent 用户消息内容
     * @param threadId 线程ID（可选，用于多轮对话）
     * @return CommonRequest 请求对象
     */
    private CommonRequest buildRunAgentRequest(String workspaceId,
                                               String botId,
                                               String userContent,
                                               String threadId) {
        // 构建API路径：/{workspaceId}/api/bot/thread/run
        String apiPath = String.format("/%s/api/bot/thread/run", workspaceId);

        log.info("构建RunAgent请求 - Path: {}, BotId: {}, ThreadId: {}",
                apiPath, botId, threadId);

        // 构建请求
        // 如果有threadId，添加到请求中（用于多轮对话）
        if (threadId != null && !threadId.isEmpty()) {
            return CommonRequest.builder()
                    .product(PRODUCT_NAME)
                    .version(API_VERSION)
                    .action("RunAgent")
                    .path(apiPath)
                    .httpMethod(HttpMethod.POST)
                    .putBodyParameters("stream", true)  // 启用流式响应
                    .putBodyParameters("botId", botId)
                    .putBodyParameters("userContent", userContent)
                    .putBodyParameters("threadId", threadId)
                    .build();
        }else {
           return   CommonRequest.builder()
                    .product(PRODUCT_NAME)
                    .version(API_VERSION)
                    .action("RunAgent")
                    .path(apiPath)
                    .httpMethod(HttpMethod.POST)
                    .putBodyParameters("stream", true)  // 启用流式响应
                    .putBodyParameters("botId", botId)
                    .putBodyParameters("userContent", userContent)
                    .build();
        }
    }

    /**
     * 处理SSE流式响应
     *
     * @param iterable SSE响应迭代器
     * @param emitter SSE发射器
     * @return 新的threadId（如果有）
     */
    private String processSSEStream(ResponseIterable<String> iterable, SseEmitter emitter) {
        String newThreadId = null;

        try {
            ResponseIterator<String> iterator = iterable.iterator();

            while (iterator.hasNext()) {
                String event = iterator.next();
                try {
                    emitter.send(event);
                } catch (Exception e) {
                    log.warn("解析SSE事件失败，发送原始数据: {}", event, e);
                    // 如果解析失败，发送原始事件数据
                    emitter.send(event);
                }
            }
        } catch (Exception e) {
            log.error("处理SSE流失败", e);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理响应流失败: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }

        return newThreadId;
    }

    // ==================== 公共接口方法 ====================

    @Override
    public ChatResponse sendChatRequest(String sessionId,
                                       String userMessage,
                                       List<ChatMessage> history,
                                       String externalSessionId,
                                       SseEmitter customEmitter,
                                        AgentProvider agentProvider) {
        log.info("通义点金SSE流式适配器处理请求 - 会话ID: {}, Bot ID: {}, ThreadId: {}",
                sessionId, agentProvider.getBotId(), externalSessionId);

        // 验证必填参数
        if (agentProvider.getBotId() == null || agentProvider.getBotId().trim().isEmpty()) {
            throw new IllegalArgumentException("Bot ID不能为空");
        }

        if (agentProvider.getApiKey() == null || agentProvider.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("API密钥不能为空");
        }

        // 对于通义点金，apiEndpoint直接传入workspaceId
        String workspaceId = agentProvider.getApiEndpoint();
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            throw new IllegalArgumentException("工作空间ID不能为空");
        }

        ChatResponse response = new ChatResponse();
        response.setEmitter(customEmitter);

        // 异步处理SSE流
        new Thread(() -> {
            CommonAsyncClient client = null;
            try {
                // 1. 解析凭据
                String[] credentials = parseCredentials(agentProvider.getApiKey());
                String accessKeyId = credentials[0];
                String accessKeySecret = credentials[1];

                log.info("通义点金凭据解析成功 - AccessKeyId: {}...",
                        accessKeyId.substring(0, Math.min(8, accessKeyId.length())));

                // 2. 创建Gateway客户端
                client = createGatewayClient(accessKeyId, accessKeySecret);

                // 保存客户端引用（用于会话管理）
                activeClients.put(sessionId, client);

                // 3. 构建请求
                CommonRequest commonRequest = buildRunAgentRequest(
                    workspaceId, agentProvider.getBotId(), userMessage, externalSessionId);

                log.info("开始SSE流式调用 - WorkspaceId: {}, BotId: {}", workspaceId, agentProvider.getBotId());

                // 4. 调用SSE API
                ResponseIterable<String> iterable = client.callSseApi(commonRequest);

                // 5. 处理SSE流式响应
                String newThreadId = processSSEStream(iterable, customEmitter);

                // 6. 记录新的threadId
                if (newThreadId != null) {
                    log.info("会话ThreadId已更新: {} -> {}", externalSessionId, newThreadId);
                }

                // 7. 发送完成信号
                customEmitter.send(SseEmitter.event().name("done").data("[DONE]"));
                customEmitter.complete();

                log.info("通义点金SSE流式调用成功完成 - 会话ID: {}", sessionId);

            } catch (Exception e) {
                log.error("通义点金SSE调用失败 - 会话ID: {}", sessionId, e);
                try {
                    customEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("通义点金响应失败: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                customEmitter.completeWithError(e);
            } finally {
                // 清理客户端引用
                if (client != null) {
                    activeClients.remove(sessionId);
                    log.debug("清理客户端引用 - 会话ID: {}", sessionId);
                }
            }
        }).start();

        return response;
    }

    @Override
    public void terminateChat(String sessionId) {
        log.info("终止通义点金会话: {}", sessionId);

        // 移除客户端引用
        CommonAsyncClient client = activeClients.remove(sessionId);
        if (client != null) {
            log.info("已移除会话客户端引用: {}", sessionId);
        } else {
            log.debug("会话客户端引用不存在: {}", sessionId);
        }
    }

    @Override
    public String getProviderType() {
        return "tongyi";
    }

    @Override
    public ChatMessage formatMessage(String rawResponse, String messageFormat) {
        ChatMessage message = new ChatMessage();

        try {
            // 尝试解析JSON格式的响应
            if (rawResponse != null && rawResponse.trim().startsWith("{")) {
                JSONObject json = JSON.parseObject(rawResponse);

                // 提取response字段作为内容
                String content = json.getString("response");
                if (content != null && !content.isEmpty()) {
                    message.setContent(content);
                } else {
                    // 如果没有response字段，使用整个JSON
                    message.setContent(rawResponse);
                }

                // 提取threadId（如果有）
                String threadId = json.getString("threadId");
                if (threadId != null) {
                    log.debug("消息包含ThreadId: {}", threadId);
                }
            } else {
                // 纯文本响应
                message.setContent(rawResponse);
            }

        } catch (Exception e) {
            log.warn("解析通义点金响应失败，使用原始内容: {}", rawResponse, e);
            message.setContent(rawResponse);
        }

        message.setMessageFormat("tongyi");
        message.setRawContent(rawResponse);
        return message;
    }
}
