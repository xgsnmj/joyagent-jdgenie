import { handleTaskData, combineData } from "@/utils/chat";
import { cloneDeep } from "lodash";

/**
 * 处理AI回答消息，将回答数据填充到 ChatItem 中
 * @param chatItem 聊天项
 * @param assistantMessage AI回答消息
 */
export function processAssistantHistoryMessage(
  chatItem: CHAT.ChatItem,
  assistantMessage: MESSAGE.History
) {
  // 设置基本信息
  chatItem.response = assistantMessage.content || "";

  // 处理任务数据
  if (assistantMessage.tasks) {
    try {
      let tasksData: CHAT.Task[] | null = null;
      if (typeof assistantMessage.tasks === "string") {
        tasksData = JSON.parse(assistantMessage.tasks);
      }
      if (tasksData && Array.isArray(tasksData)) {
        console.log("[历史消息] 任务", cloneDeep(tasksData));
        chatItem.multiAgent.tasks = convertTasksData(tasksData);
      }
    } catch (error) {}
  }

  // 处理计划数据
  if (assistantMessage.plan) {
    try {
      let planData: MESSAGE.Plan | null = null;
      if (typeof assistantMessage.plan === "string") {
        planData = JSON.parse(assistantMessage.plan);
      }

      if (planData) {
        chatItem.multiAgent.plan = planData;
        chatItem.plan = planData;
      }
    } catch (error) {}
  }

  // 该数据类似发送数据时响应的数据流
  const metaData: MESSAGE.Answer[] = JSON.parse(
    assistantMessage.metadata
  ).messages;
  if (metaData && Array.isArray(metaData)) {
    metaData.forEach((item) => {
      chatItem = combineData(item.resultMap.eventData!, chatItem);
    });
  }

  try {
    const taskDataResult = handleTaskData(
      chatItem,
      chatItem.deepThink, // 历史数据默认不使用深度思考
      chatItem.multiAgent
    );
    console.log("[历史消息] 转换", taskDataResult);
    return taskDataResult;
  } catch (error) {
    console.error("处理历史任务数据失败:", error);
  }
}

/**
 * 转换任务数据格式
 * @param tasksData 原始任务数据
 * @returns 转换后的任务数据
 */
function convertTasksData(tasksData: any[]): MESSAGE.Task[][] {
  const result: MESSAGE.Task[][] = [];

  // 按 taskId 分组任务
  const taskGroups: Record<string, any[]> = {};

  tasksData.forEach((task) => {
    const taskId = task.taskId || task.id;
    if (!taskId) return;

    if (!taskGroups[taskId]) {
      taskGroups[taskId] = [];
    }
    taskGroups[taskId].push(task);
  });

  // 转换为所需格式
  Object.values(taskGroups).forEach((group) => {
    if (group.length > 0) {
      result.push(group.map((task) => convertSingleTask(task)));
    }
  });

  return result;
}

/**
 * 转换单个任务
 * @param task 原始任务数据
 * @returns 转换后的任务数据
 */
function convertSingleTask(task: any): MESSAGE.Task {
  const convertedTask: MESSAGE.Task = {
    id: task.id,
    taskId: task.taskId || task.id,
    requestId: task.requestId,
    messageId: task.messageId,
    messageType: task.messageType,
    task: task.task,
    toolThought: task.toolThought,
    resultMap: {
      agentType: task.resultMap?.agentType || 5,
      messageType: task.resultMap?.messageType || task.messageType,
      query: task.resultMap?.query || task.query,
      answer: task.resultMap?.answer || task.answer || "",
      data: task.resultMap?.data || task.data,
      codeOutput: task.resultMap?.codeOutput || task.resultMap?.data || "",
      isFinal: task.resultMap?.isFinal !== false,
      fileInfo: task.resultMap?.fileInfo || task.fileInfo || [],
      searchResult: task.resultMap?.searchResult,
      task: task.resultMap?.task || task.task,
      steps: [],
      taskSummary: task.resultMap?.taskSummary,
      fileList: task.resultMap?.fileList,
    },
    messageTime: task.messageTime || new Date().toISOString(),
    finish: task.finish !== false,
    isFinal: task.isFinal !== false,
    toolResult: task.toolResult,
  };

  // 处理特殊的搜索结果格式
  if (
    task.resultMap?.searchResult &&
    typeof task.resultMap.searchResult === "string"
  ) {
    try {
      convertedTask.resultMap!.searchResult = JSON.parse(
        task.resultMap.searchResult
      );
    } catch (error) {
      console.warn("解析搜索结果失败:", error);
    }
  }

  return convertedTask;
}
