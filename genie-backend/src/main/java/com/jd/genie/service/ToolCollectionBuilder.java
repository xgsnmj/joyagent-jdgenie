package com.jd.genie.service;

import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.agent.tool.ToolCollection;
import com.jd.genie.model.req.AgentRequest;

/**
 * 工具集合构建器接口
 * 根据请求类型构建对应的工具集合
 *
 * 设计目的：
 * 1. 将工具构建逻辑从Controller中提取出来
 * 2. 便于测试和维护
 * 3. 支持不同类型请求使用不同的工具集
 *
 * 工具类型：
 * - FileTool：文件处理工具
 * - CodeInterpreterTool：代码执行工具
 * - ReportTool：报告生成工具
 * - DeepSearchTool：深度搜索工具
 * - DataAnalysisTool：数据分析工具
 * - McpTool：MCP协议工具
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
public interface ToolCollectionBuilder {

    /**
     * 构建工具集合
     *
     * 构建规则：
     * 1. 如果是dataAgent模式，只添加ReportTool和DataAnalysisTool
     * 2. 否则根据配置添加默认工具集（search/code/report/data_analysis）
     * 3. 始终尝试添加MCP工具（如果配置了MCP服务器）
     *
     * @param agentContext Agent上下文
     * @param request 请求对象
     * @return 工具集合
     */
    ToolCollection build(AgentContext agentContext, AgentRequest request);
}
