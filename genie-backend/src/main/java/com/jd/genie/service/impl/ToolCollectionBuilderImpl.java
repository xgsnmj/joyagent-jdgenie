package com.jd.genie.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jd.genie.agent.agent.AgentContext;
import com.jd.genie.agent.tool.ToolCollection;
import com.jd.genie.agent.tool.common.*;
import com.jd.genie.agent.tool.mcp.McpTool;
import com.jd.genie.config.GenieConfig;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.service.ToolCollectionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 工具集合构建器实现类
 * 从GenieController中提取的工具构建逻辑
 *
 * 构建规则：
 * 1. 如果是dataAgent模式，只添加ReportTool和DataAnalysisTool
 * 2. 否则添加FileTool + 根据配置添加的工具集
 * 3. 始终尝试添加MCP工具（如果配置了MCP服务器）
 *
 * @author JD Genie Team
 * @since 2025-01-04
 */
@Slf4j
@Component
public class ToolCollectionBuilderImpl implements ToolCollectionBuilder {

    @Autowired
    private GenieConfig genieConfig;

    @Override
    public ToolCollection build(AgentContext agentContext, AgentRequest request) {
        log.debug("[工具构建] 开始构建 - requestId: {}, outputStyle: {}",
                agentContext.getRequestId(), request.getOutputStyle());

        ToolCollection toolCollection = new ToolCollection();
        toolCollection.setAgentContext(agentContext);

        // 根据outputStyle选择工具集
        if ("dataAgent".equals(request.getOutputStyle())) {
            buildDataAgentTools(agentContext, toolCollection);
        } else {
            buildDefaultTools(agentContext, toolCollection);
        }

        // 添加MCP工具
        addMcpTools(agentContext, toolCollection);

        log.debug("[工具构建] 构建完成 - requestId: {}, toolCount: {}",
                agentContext.getRequestId(), toolCollection.getToolMap().size());

        return toolCollection;
    }

    /**
     * 构建DataAgent模式的工具集
     * 只包含ReportTool和DataAnalysisTool
     */
    private void buildDataAgentTools(AgentContext agentContext, ToolCollection toolCollection) {
        log.debug("[工具构建] DataAgent模式 - 添加ReportTool和DataAnalysisTool");

        // 添加ReportTool
        ReportTool htmlTool = new ReportTool();
        htmlTool.setAgentContext(agentContext);
        toolCollection.addTool(htmlTool);

        // 添加DataAnalysisTool
        DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
        dataAnalysisTool.setAgentContext(agentContext);
        toolCollection.addTool(dataAnalysisTool);
    }

    /**
     * 构建默认模式的工具集
     * 包含FileTool + 配置的工具集
     */
    private void buildDefaultTools(AgentContext agentContext, ToolCollection toolCollection) {
        log.debug("[工具构建] 默认模式 - 添加FileTool和配置的工具");

        // 添加FileTool
        FileTool fileTool = new FileTool();
        fileTool.setAgentContext(agentContext);
        toolCollection.addTool(fileTool);

        // 从配置中获取默认工具列表
        String toolListStr = genieConfig.getMultiAgentToolListMap()
                .getOrDefault("default", "search,code,report");
        List<String> agentToolList = Arrays.asList(toolListStr.split(","));

        if (agentToolList.isEmpty()) {
            log.warn("[工具构建] 默认工具列表为空");
            return;
        }

        // 根据配置添加工具
        if (agentToolList.contains("code")) {
            CodeInterpreterTool codeTool = new CodeInterpreterTool();
            codeTool.setAgentContext(agentContext);
            toolCollection.addTool(codeTool);
            log.debug("[工具构建] 已添加CodeInterpreterTool");
        }

        if (agentToolList.contains("report")) {
            ReportTool htmlTool = new ReportTool();
            htmlTool.setAgentContext(agentContext);
            toolCollection.addTool(htmlTool);
            log.debug("[工具构建] 已添加ReportTool");
        }

        if (agentToolList.contains("search")) {
            DeepSearchTool deepSearchTool = new DeepSearchTool();
            deepSearchTool.setAgentContext(agentContext);
            toolCollection.addTool(deepSearchTool);
            log.debug("[工具构建] 已添加DeepSearchTool");
        }

        if (agentToolList.contains("data_analysis")) {
            DataAnalysisTool dataAnalysisTool = new DataAnalysisTool();
            dataAnalysisTool.setAgentContext(agentContext);
            toolCollection.addTool(dataAnalysisTool);
            log.debug("[工具构建] 已添加DataAnalysisTool");
        }
    }

    /**
     * 添加MCP工具
     * 遍历所有配置的MCP服务器，注册可用的工具
     */
    private void addMcpTools(AgentContext agentContext, ToolCollection toolCollection) {
        try {
            String[] mcpServers = genieConfig.getMcpServerUrlArr();
            if (mcpServers == null || mcpServers.length == 0) {
                log.debug("[工具构建] 未配置MCP服务器");
                return;
            }

            log.debug("[工具构建] 开始添加MCP工具 - 服务器数量: {}", mcpServers.length);

            McpTool mcpTool = new McpTool();
            mcpTool.setAgentContext(agentContext);

            for (String mcpServer : mcpServers) {
                try {
                    addMcpServerTools(agentContext, toolCollection, mcpTool, mcpServer);
                } catch (Exception e) {
                    log.error("[工具构建] 添加MCP服务器工具失败 - server: {}, error: {}",
                            mcpServer, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("[工具构建] 添加MCP工具失败 - requestId: {}",
                    agentContext.getRequestId(), e);
        }
    }

    /**
     * 从单个MCP服务器添加工具
     */
    private void addMcpServerTools(
            AgentContext agentContext,
            ToolCollection toolCollection,
            McpTool mcpTool,
            String mcpServer) {

        // 获取工具列表
        String listToolResult = mcpTool.listTool(mcpServer);
        if (listToolResult == null || listToolResult.isEmpty()) {
            log.error("[工具构建] MCP服务器无响应 - requestId: {}, server: {}",
                    agentContext.getRequestId(), mcpServer);
            return;
        }

        // 解析响应
        JSONObject resp = JSON.parseObject(listToolResult);
        if (resp.getIntValue("code") != 200) {
            log.error("[工具构建] MCP服务器返回错误 - requestId: {}, server: {}, code: {}, message: {}",
                    agentContext.getRequestId(), mcpServer,
                    resp.getIntValue("code"), resp.getString("message"));
            return;
        }

        // 获取工具数据
        JSONArray data = resp.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            log.warn("[工具构建] MCP服务器无可用工具 - requestId: {}, server: {}",
                    agentContext.getRequestId(), mcpServer);
            return;
        }

        // 注册工具
        for (int i = 0; i < data.size(); i++) {
            try {
                JSONObject tool = data.getJSONObject(i);
                String method = tool.getString("name");
                String description = tool.getString("description");
                String inputSchema = tool.getString("inputSchema");

                toolCollection.addMcpTool(method, description, inputSchema, mcpServer);

                log.debug("[工具构建] MCP工具已注册 - server: {}, tool: {}", mcpServer, method);

            } catch (Exception e) {
                log.error("[工具构建] 注册MCP工具失败 - server: {}, index: {}, error: {}",
                        mcpServer, i, e.getMessage());
            }
        }

        log.info("[工具构建] MCP服务器工具注册完成 - server: {}, count: {}",
                mcpServer, data.size());
    }
}
