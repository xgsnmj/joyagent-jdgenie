package com.jd.genie.service;

import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.agent.util.DateUtil;
import com.jd.genie.config.GenieConfig;
import com.jd.genie.context.SessionContext;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.util.ConversationDataCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Objects;

/**
 * 会话上下文构建器
 * 负责构建SessionContext和AgentContext
 *
 * 职责：
 * 1. 处理输出样式（拼接提示词）
 * 2. 构建AgentContext
 * 3. 组装SessionContext
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class SessionContextBuilder {

    @Autowired
    private GenieConfig genieConfig;

    /**
     * 构建会话上下文
     *
     * @param request 请求对象
     * @param userId 用户ID（可能为null）
     * @param emitter SSE发射器
     * @return SessionContext 会话上下文
     */
    public SessionContext build(AgentRequest request, Long userId, SseEmitter emitter) {
        log.debug("[上下文构建] 开始构建 - requestId: {}", request.getRequestId());

        // 1. 保存原始query
        String originalQuery = request.getQuery();

        // 2. 处理输出样式（拼接提示词）
        String processedQuery = processOutputStyle(request);
        request.setQuery(processedQuery);

        // 3. 构建AgentContext
        AgentContext agentContext = buildAgentContext(request, processedQuery);

        // 4. 构建SessionContext
        SessionContext context = SessionContext.builder()
                .sessionId(request.getSessionId())
                .requestId(request.getRequestId())
                .userId(userId)
                .request(request)
                .originalQuery(originalQuery)
                .processedQuery(processedQuery)
                .agentContext(agentContext)
                .emitter(emitter)
                .build();

        log.debug("[上下文构建] 构建完成 - sessionId: {}, userId: {}, platformType: {}",
                context.getSessionId(), userId, context.getPlatformType());

        return context;
    }

    /**
     * 处理输出样式（拼接提示词）
     *
     * 根据outputStyle配置，在query后面拼接对应的提示词
     * 例如：html模式会拼接 "以HTML格式展示"
     *
     * @param request 请求对象
     * @return 处理后的query
     */
    private String processOutputStyle(AgentRequest request) {
        String query = request.getQuery();

        if (request.getOutputStyle() == null || request.getOutputStyle().isEmpty()) {
            return query;
        }

        // 从配置中获取输出样式对应的提示词
        String stylePrompt = genieConfig.getOutputStylePrompts()
                .getOrDefault(request.getOutputStyle(), "");

        if (!stylePrompt.isEmpty()) {
            query += stylePrompt;
            log.debug("[上下文构建] 拼接输出样式提示词 - outputStyle: {}, promptLength: {}",
                    request.getOutputStyle(), stylePrompt.length());
        }

        return query;
    }

    /**
     * 构建AgentContext
     *
     * AgentContext是智能体执行的核心上下文，包含：
     * - 请求信息
     * - 日期信息
     * - 文件列表
     * - Prompt配置
     * - 流式响应开关
     * - 数据收集器
     *
     * @param request 请求对象
     * @param processedQuery 处理后的query
     * @return AgentContext
     */
    private AgentContext buildAgentContext(AgentRequest request, String processedQuery) {
        // 构建基础AgentContext
        AgentContext agentContext = AgentContext.builder()
                .requestId(request.getRequestId())
                .sessionId(request.getSessionId())
                .printer(null)  // printer由执行器设置
                .query(processedQuery)
                .task("")
                .dateInfo(DateUtil.CurrentDateInfo())
                .productFiles(new ArrayList<>())
                .taskProductFiles(new ArrayList<>())
                .sopPrompt(request.getSopPrompt())
                .basePrompt(request.getBasePrompt())
                .agentType(request.getAgentType())
                .isStream(Objects.nonNull(request.getIsStream()) ? request.getIsStream() : false)
                .templateType("dataAgent".equals(request.getOutputStyle()) ? "fix" : "empty")
                .assistantResponse(new StringBuilder())
                .build();

        // 初始化数据收集器（用于收集思考过程、任务详情等）
        agentContext.setDataCollector(new ConversationDataCollector());

        log.debug("[上下文构建] AgentContext已创建 - agentType: {}, isStream: {}, templateType: {}",
                agentContext.getAgentType(),
                agentContext.getIsStream(),
                agentContext.getTemplateType());

        return agentContext;
    }
}
