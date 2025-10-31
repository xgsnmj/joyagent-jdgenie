package com.jd.genie.model.req;

import lombok.Data;
import java.util.Map;

/**
 * 前端上报multiAgent数据的请求体
 *
 * <p>用于接收前端在对话完成后上报的完整multiAgent结构，
 * 包含AI思考、任务执行、工具调用等所有前端渲染所需的信息。</p>
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Data
public class UploadMultiAgentRequest {

    /**
     * 请求ID
     * 用于关联到对应的assistant消息
     */
    private String requestId;

    /**
     * 完整的multiAgent数据结构
     * 包含：
     * - plan: 执行计划
     * - plan_thought: 计划思考
     * - tasks: 任务执行详情（二维数组）
     */
    private Map<String, Object> multiAgent;
}
