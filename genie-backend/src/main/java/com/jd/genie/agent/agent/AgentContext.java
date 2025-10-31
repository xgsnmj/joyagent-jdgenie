package com.jd.genie.agent.agent;

import com.jd.genie.agent.dto.File;
import com.jd.genie.agent.printer.Printer;
import com.jd.genie.agent.tool.ToolCollection;
import com.jd.genie.model.dto.FileInformation;
import com.jd.genie.model.req.AgentRequest;
import com.jd.genie.util.ConversationDataCollector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Builder
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {
    String requestId;
    String sessionId;
    String query;
    String task;
    Printer printer;
    ToolCollection toolCollection;
    String dateInfo;
    List<File> productFiles;
    Boolean isStream;
    String streamMessageType;
    String sopPrompt;
    String basePrompt;
    Integer agentType;
    List<File> taskProductFiles;
    String templateType;

    /**
     * 用于累积AI助手的完整回复内容，供后续保存到数据库
     * 支持流式回复和最终结果的累积
     */
    @Builder.Default
    StringBuilder assistantResponse = new StringBuilder();

    /**
     * 任务中断标志（用于停止会话时通知处理线程终止）
     * 使用volatile确保多线程可见性
     */
    private volatile boolean interrupted = false;

    /**
     * 会话数据收集器
     * 用于收集SSE流式响应过程中的完整会话数据（思考过程、任务详情、计划信息等）
     * 收集的数据将保存到数据库，用于历史会话的完整还原
     */
    private ConversationDataCollector dataCollector;

    /**
     * 标记任务为已中断
     */
    public void markInterrupted() {
        this.interrupted = true;
    }

    /**
     * 检查任务是否已中断
     * @return true表示已中断，false表示正常运行
     */
    public boolean isInterrupted() {
        return this.interrupted;
    }
}