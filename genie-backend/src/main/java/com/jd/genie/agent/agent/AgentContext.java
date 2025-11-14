package com.jd.genie.agent.agent;

import com.jd.genie.agent.dto.File;
import com.jd.genie.agent.printer.Printer;
import com.jd.genie.agent.tool.ToolCollection;
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
     * 任务中断标志（用于停止会话时通知处理线程终止）
     * 使用volatile确保多线程可见性
     */
    private volatile boolean interrupted = false;


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