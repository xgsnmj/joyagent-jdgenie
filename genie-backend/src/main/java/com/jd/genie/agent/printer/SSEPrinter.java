package com.jd.genie.agent.printer;

import com.alibaba.fastjson.JSON;
import com.jd.genie.agent.enums.AgentType;
import com.jd.genie.agent.util.StringUtil;
import com.jd.genie.handler.AgentResponseHandler;
import com.jd.genie.model.multi.EventResult;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.model.response.AgentResponse;
import com.jd.genie.model.response.GptProcessResult;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Setter
public class SSEPrinter implements Printer {
    private SseEmitter emitter;
    private AgentRequest request;
    private Integer agentType;
    private com.jd.genie.agent.agent.AgentContext agentContext;  // 持有AgentContext引用
    private AgentResponseHandler handler;  // Handler用于将AgentResponse转换为GptProcessResult
    private EventResult eventResult;  // 事件结果累积器
    private List<AgentResponse> agentRespList;  // AgentResponse列表

    public SSEPrinter(SseEmitter emitter,
                     AgentRequest request,
                     Integer agentType,
                     com.jd.genie.agent.agent.AgentContext agentContext,
                     AgentResponseHandler handler) {
        this.emitter = emitter;
        this.request = request;
        this.agentType = agentType;
        this.agentContext = agentContext;
        this.handler = handler;
        this.eventResult = new EventResult();
        this.agentRespList = new ArrayList<>();
    }

    @Override
    public void send(String messageId, String messageType, Object message, String digitalEmployee, Boolean isFinal) {
        try {
            // 检查任务是否已中断，如果已中断则抛出异常终止处理线程
            if (agentContext != null && agentContext.isInterrupted()) {
                log.info("{} 检测到任务中断标志，停止发送SSE消息", request.getRequestId());
                throw new RuntimeException("任务已中断");
            }

            if (Objects.isNull(messageId)) {
                messageId = StringUtil.getUUID();
            }
            log.info("{} sse send {} {} {}", request.getRequestId(), messageType, message, digitalEmployee);
            boolean finish = "result".equals(messageType);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("agentType", agentType);
            AgentResponse response = AgentResponse.builder()
                    .requestId(request.getRequestId())
                    .messageId(messageId)
                    .messageType(messageType)
                    .messageTime(String.valueOf(System.currentTimeMillis()))
                    .resultMap(resultMap)
                    .finish(finish)
                    .isFinal(isFinal)
                    .build();
            if (!StringUtils.isEmpty(digitalEmployee)) {
                response.setDigitalEmployee(digitalEmployee);
            }
            switch (messageType) {
                case "tool_thought":
                    response.setToolThought((String) message);
                    break;
                case "task":
                    response.setTask(((String) message).replaceAll("^执行顺序(\\d+)\\.\\s?", ""));
                    break;
                case "task_summary":
                    if (message instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> taskSummary = (Map<String, Object>) message;
                        Object summary = taskSummary.get("taskSummary");
                        response.setResultMap(taskSummary);
                        response.setTaskSummary(summary != null ? summary.toString() : null);
                    } else {
                        log.error("ssePrinter task_summary format is illegal");
                    }
                    break;
                case "plan_thought":
                    response.setPlanThought((String) message);
                    break;
                case "plan":
                    AgentResponse.Plan plan = new AgentResponse.Plan();
                    BeanUtils.copyProperties(message, plan);
                    response.setPlan(AgentResponse.formatSteps(plan));
                    break;
                case "tool_result":
                    response.setToolResult((AgentResponse.ToolResult) message);
                    break;
                case "browser":
                case "code":
                case "html":
                case "markdown":
                case "ppt":
                case "file":
                case "knowledge":
                case "deep_search":
                case "data_analysis":
                    response.setResultMap(JSON.parseObject(JSON.toJSONString(message)));
                    response.getResultMap().put("agentType", agentType);
                    break;
                case "agent_stream":
                    break;
                case "result":
                    // 提取AI回复内容并保存到AgentContext
                    String assistantReply = null;
                    if (message instanceof String) {
                        assistantReply = (String) message;
                        response.setResult(assistantReply);
                    } else if (message instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> taskResult = (Map<String, Object>) message;
                        Object summary = taskResult.get("taskSummary");
                        assistantReply = summary != null ? summary.toString() : null;
                        response.setResultMap(taskResult);
                        response.setResult(assistantReply);
                    } else {
                        Map<String, Object> taskResult = JSON.parseObject(JSON.toJSONString(message));
                        assistantReply = taskResult.get("taskSummary") != null ?
                                         taskResult.get("taskSummary").toString() : null;
                        response.setResultMap(taskResult);
                        response.setResult(assistantReply);
                    }



                    response.getResultMap().put("agentType", agentType);
                    break;
                default:
                    break;
            }
            // 先转换为 GptProcessResult
            GptProcessResult result = handler.handle(request, response, agentRespList, eventResult);

            // 发送 GptProcessResult（而不是 AgentResponse）
            emitter.send(result);

        } catch (Exception e) {
            log.error("sse send error ", e);
        }
    }

    @Override
    public void send(String messageType, Object message, String digitalEmployee) {
        send(null, messageType, message, digitalEmployee, true);
    }

    @Override
    public void send(String messageType, Object message) {
        send(null, messageType, message, null, true);
    }

    @Override
    public void send(String messageId, String messageType, Object message, Boolean isFinal) {
        send(messageId, messageType, message, null, isFinal);
    }

    @Override
    public void close() {
        emitter.complete();
    }

    @Override
    public void updateAgentType(AgentType agentType) {
        this.agentType = agentType.getValue();
    }
}