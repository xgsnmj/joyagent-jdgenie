package com.jd.genie.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.agent.enums.AgentType;
import com.jd.genie.agent.enums.AutoBotsResultStatus;
import com.jd.genie.agent.enums.ResponseTypeEnum;
import com.jd.genie.config.GenieConfig;
import com.jd.genie.handler.AgentResponseHandler;
import com.jd.genie.model.dto.AutoBotsResult;
import com.jd.genie.model.multi.EventResult;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.model.req.GptQueryReq;
import com.jd.genie.model.response.AgentResponse;
import com.jd.genie.model.response.GptProcessResult;
import com.jd.genie.service.IMultiAgentService;
import com.jd.genie.service.SessionOrchestrationService;
import com.jd.genie.util.ChateiUtils;
import com.jd.genie.util.InterceptableSseEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class MultiAgentServiceImpl implements IMultiAgentService {
    @Autowired
    private GenieConfig genieConfig;
    @Autowired
    private SessionOrchestrationService sessionOrchestrationService;

    @Override
    public AutoBotsResult searchForAgentRequest(GptQueryReq gptQueryReq, SseEmitter sseEmitter) {
        AgentRequest agentRequest = buildAgentRequest(gptQueryReq);
        log.info("{} start handle Agent request: {}", gptQueryReq.getRequestId(), JSON.toJSONString(agentRequest));
        try {
            handleMultiAgentRequest(agentRequest, sseEmitter);
        } catch (Exception e) {
            log.error("{}, error in requestMultiAgent, deepThink: {}, errorMsg: {}", gptQueryReq.getRequestId(), gptQueryReq.getDeepThink(), e.getMessage(), e);
            throw e;
        } finally {
            log.info("{}, agent.query.web.singleRequest end, requestId: {}", gptQueryReq.getRequestId(), JSON.toJSONString(gptQueryReq));
        }

        return ChateiUtils.toAutoBotsResult(agentRequest, AutoBotsResultStatus.loading.name());
    }

    /**
     * 处理多智能体请求
     * 根据平台类型选择处理方式：
     * - 默认平台：使用拦截器进行AgentResponse -> GptProcessResult格式转换
     * - 外部平台：直接转发SSE（由ExternalAgentExecutor处理数据收集）
     *
     * @param autoReq       智能体请求对象
     * @param targetEmitter 目标SSE发射器（最终发送给前端）
     */
    public void handleMultiAgentRequest(AgentRequest autoReq, SseEmitter targetEmitter) {
        // 将userId从erp字段解析
        Long userId = null;
        if (autoReq.getErp() != null && !autoReq.getErp().isEmpty()) {
            try {
                userId = Long.parseLong(autoReq.getErp());
            } catch (NumberFormatException e) {
                log.debug("{} erp不是有效的userId: {}", autoReq.getRequestId(), autoReq.getErp());
            }
        }

        try {
            log.info("{} 调用SessionOrchestrationService (external platform): userId={}", autoReq.getRequestId(), userId);
            // 直接传递targetEmitter，不使用格式转换拦截器
            sessionOrchestrationService.orchestrate(autoReq, userId, targetEmitter);

        } catch (Exception e) {
            log.error("{} 调用SessionOrchestrationService失败 (external platform)", autoReq.getRequestId(), e);
            try {
                targetEmitter.completeWithError(e);
            } catch (Exception sendException) {
                log.error("{} 发送错误失败", autoReq.getRequestId(), sendException);
            }
        }

    }


    private AgentRequest buildAgentRequest(GptQueryReq req) {
        AgentRequest request = new AgentRequest();
        request.setRequestId(req.getTraceId());
        request.setSessionId(req.getSessionId()); // 传递 sessionId 用于多轮对话文件管理
        request.setErp(req.getUser());
        request.setQuery(req.getQuery());
        //根据如果是深度研究，则使用规划解决模式，否则使用REACT模式
        request.setAgentType(req.getDeepThink() == 0 ? 5 : 3);
        request.setSopPrompt(request.getAgentType().equals(AgentType.PLAN_SOLVE.getValue()) ? genieConfig.getGenieSopPrompt() : "");
        request.setBasePrompt(request.getAgentType().equals(AgentType.REACT.getValue()) ? genieConfig.getGenieBasePrompt() : "");
        request.setIsStream(true);
        request.setOutputStyle(req.getOutputStyle());
        request.setAgentProviderId(req.getAgentProviderId());
        return request;
    }

}
