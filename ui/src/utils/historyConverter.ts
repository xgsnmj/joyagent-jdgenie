import { handleTaskData, combineData } from "@/utils/chat";
import { agentConverterFactory } from "./agentConverters";

/**
 * 处理AI回答消息，将回答数据填充到 ChatItem 中
 * @param chatItem 聊天项
 * @param assistantMessage AI回答消息
 */
export function processAssistantHistoryMessage(
  chat: CHAT.ChatItem,
  assistantMessage: MESSAGE.History,
  agentType: CHAT.AgentType = "default"
) {
  console.log("===================处理历史会话[开始]===================");
  switch (agentType) {
    case "coze":
      chat = cozeHistoryToChat(chat, assistantMessage);
      break;
    case "tongyi":
      chat = tongyiHistoryToChat(chat, assistantMessage);
      break;
    case "ronghui":
      chat = ronghuiHistoryToChat(chat, assistantMessage);
      break;
    default:
      chat = localHistoryToChat(chat, assistantMessage);
      break;
  }
  console.log("===================处理历史会话[结束]===================");
  return transformHistory(chat);
}

function localHistoryToChat(
  chat: CHAT.ChatItem,
  assistantMessage: MESSAGE.History
) {
  // 设置基本信息
  chat.response = assistantMessage.content || "";

  if (assistantMessage.tasks) {
    try {
      let tasksData: CHAT.Task[] | null = null;
      if (typeof assistantMessage.tasks === "string") {
        tasksData = JSON.parse(assistantMessage.tasks);
      }
      if (tasksData && Array.isArray(tasksData)) {
        chat.multiAgent.tasks = convertTasksData(tasksData);
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
        chat.multiAgent.plan = planData;
        chat.plan = planData;
      }
    } catch (error) {}
  }

  // 该数据类似发送数据时响应的数据流
  const metaData: MESSAGE.Answer[] = JSON.parse(
    assistantMessage.metadata
  ).messages;
  if (metaData && Array.isArray(metaData)) {
    metaData.forEach((item) => {
      if (item?.resultMap?.eventData) {
        chat = combineData(item.resultMap.eventData!, chat);
      }
    });
  }
  return chat;
}

function cozeHistoryToChat(
  chat: CHAT.ChatItem,
  assistantMessage: MESSAGE.History
) {
  if (assistantMessage.metadata) {
    try {
      const converter = agentConverterFactory.getConverter("coze");
      let metadata: MESSAGE.CozeHistoryMeta;
      if (typeof assistantMessage.metadata === "string") {
        metadata = JSON.parse(assistantMessage.metadata);
        console.log("[coze:metadata]", metadata);
        metadata.messages.forEach((item) => {
          const { eventName, data } = item;
          if (eventName !== "done") {
            const messageData = JSON.parse(data);
            const cozeMessage = converter?.convert(messageData, eventName);
            if (cozeMessage?.resultMap.eventData) {
              chat = combineData(cozeMessage.resultMap.eventData!, chat);
            }
          }
        });
      }
    } catch (error) {
      console.log(error);
    }
  }
  return chat;
}

function tongyiHistoryToChat(
  chat: CHAT.ChatItem,
  assistantMessage: MESSAGE.History
) {
  if (assistantMessage.metadata) {
    try {
      const converter = agentConverterFactory.getConverter("tongyi");
      let metadata: MESSAGE.CozeHistoryMeta;
      if (typeof assistantMessage.metadata === "string") {
        metadata = JSON.parse(assistantMessage.metadata);
        console.log("[tongyi:metadata]", metadata);
        metadata.messages.forEach((item) => {
          const { eventName, data } = item;
          if (eventName !== "done") {
            const messageData = JSON.parse(data);
            const cozeMessage = converter?.convert(messageData, eventName);
            if (cozeMessage?.resultMap.eventData) {
              chat = combineData(cozeMessage.resultMap.eventData!, chat);
            }
          }
        });
      }
    } catch (error) {
      console.log(error);
    }
  }
  return chat;
}

function ronghuiHistoryToChat(
  chat: CHAT.ChatItem,
  assistantMessage: MESSAGE.History
) {
  if (assistantMessage.metadata) {
    try {
      const converter = agentConverterFactory.getConverter("ronghui");
      let metadata: MESSAGE.CozeHistoryMeta;
      if (typeof assistantMessage.metadata === "string") {
        metadata = JSON.parse(assistantMessage.metadata);
        console.log("[ronghui:metadata]", metadata);
        metadata.messages.forEach((item) => {
          const { eventName, data } = item;
          if (eventName !== "done") {
            const messageData = JSON.parse(data);
            const cozeMessage = converter?.convert(messageData, eventName);
            if (cozeMessage?.resultMap.eventData) {
              chat = combineData(cozeMessage.resultMap.eventData!, chat);
            }
          }
        });
      }
    } catch (error) {
      console.log(error);
    }
  }
  return chat;
}

function transformHistory(chat: CHAT.ChatItem) {
  try {
    const taskDataResult = handleTaskData(
      chat,
      chat.deepThink, // 历史数据默认不使用深度思考
      chat.multiAgent
    );
    return taskDataResult;
  } catch (error) {}
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
