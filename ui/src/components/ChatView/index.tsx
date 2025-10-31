import { useEffect, useState, useRef, useMemo } from "react";
import {
  getUniqId,
  scrollToTop,
  ActionViewItemEnum,
  getSessionId,
} from "@/utils";
import querySSE, { SSEController } from "@/utils/querySSE";
import { handleTaskData, combineData } from "@/utils/chat";
import { uploadMultiAgentData } from "@/api/chat";
import Dialogue from "@/components/Dialogue";
import DataDialogue from "@/components/Dialogue/DataDialogue";
import GeneralInput from "@/components/GeneralInput";
import ActionView from "@/components/ActionView";
import { RESULT_TYPES } from "@/utils/constants";
import { useMemoizedFn } from "ahooks";
import classNames from "classnames";
import Logo from "../Logo";
import { Modal } from "antd";
import { useSessionStore } from "@/store/session";

type Props = {
  inputInfo: CHAT.TInputInfo;
  product?: CHAT.Product;
  initialSessionId?: string | null;
  initialMessages?: any[];
};

const ChatView: GenieType.FC<Props> = (props) => {
  const { inputInfo: inputInfoProp, product, initialSessionId, initialMessages = [] } = props;

  const [chatTitle, setChatTitle] = useState("");
  const [taskList, setTaskList] = useState<MESSAGE.Task[]>([]);
  const chatList = useRef<CHAT.ChatItem[]>([]);
  const [dataChatList, setDataChatList] = useState<Record<string, any>[]>([]);
  const [activeTask, setActiveTask] = useState<CHAT.Task>();
  const [plan, setPlan] = useState<CHAT.Plan>();
  const [showAction, setShowAction] = useState(false);
  const [loading, setLoading] = useState(false);
  const chatRef = useRef<HTMLInputElement>(null);
  // 用于强制触发重新渲染（当chatList.current更新但需要UI刷新时）
  const [, setForceUpdate] = useState(0);
  const actionViewRef = ActionView.useActionView();

  // 优先使用传入的sessionId，否则生成新的
  const sessionId = useMemo(() => {
    return initialSessionId || getSessionId();
  }, [initialSessionId]);

  const [modal, contextHolder] = Modal.useModal();

  // SSE连接控制器，用于中断连接
  const sseControllerRef = useRef<SSEController | null>(null);

  // 获取会话store中的setIsStreaming和addMessage方法，用于同步流式输出状态和消息
  const { setIsStreaming, addMessage } = useSessionStore();

  const combineCurrentChat = (
    inputInfo: CHAT.TInputInfo,
    sessionId: string,
    requestId: string
  ): CHAT.ChatItem => {
    return {
      query: inputInfo.message!,
      files: inputInfo.files!,
      responseType: "txt",
      sessionId,
      requestId,
      loading: true,
      forceStop: false,
      tasks: [],
      thought: "",
      response: "",
      taskStatus: 0,
      tip: "已接收到你的任务，将立即开始处理...",
      multiAgent: { tasks: [] },
    };
  };

  const sendMessage = useMemoizedFn((inputInfo: CHAT.TInputInfo) => {
    const { message, deepThink, outputStyle } = inputInfo;
    const requestId = getUniqId();
    let currentChat = combineCurrentChat(inputInfo, sessionId, requestId);
    chatList.current = [...chatList.current, currentChat];
    if (!chatTitle) {
      setChatTitle(message!);
    }
    setLoading(true);
    setIsStreaming(true); // 同步流式输出状态

    // 同步用户消息到store（用于新建会话判断）
    addMessage({
      id: requestId,
      sessionId: sessionId,
      role: 'user',
      content: message!,
      createTime: new Date().toISOString()
    });

    const params = {
      sessionId: sessionId,
      requestId: requestId,
      query: message,
      deepThink: deepThink ? 1 : 0,
      outputStyle,
    };
    const handleMessage = (data: MESSAGE.Answer) => {
      const { finished, resultMap, packageType, status } = data;
      if (status === "tokenUseUp") {
        modal.info({
          title: "您的试用次数已用尽",
          content: "如需额外申请，请联系 liyang.1236@jd.com",
        });
        const taskData = handleTaskData(
          currentChat,
          deepThink,
          currentChat.multiAgent
        );
        currentChat.loading = false;
        setLoading(false);
        setIsStreaming(false); // 同步流式输出状态

        setTaskList(taskData.taskList);
        return;
      }
      if (packageType !== "heartbeat") {
        requestAnimationFrame(() => {
          if (resultMap?.eventData) {
            currentChat = combineData(resultMap.eventData || {}, currentChat);
            const taskData = handleTaskData(
              currentChat,
              deepThink,
              currentChat.multiAgent
            );
            setTaskList(taskData.taskList);
            temporaryChangeTask(taskData.taskList);
            updatePlan(taskData.plan!);
            openAction(taskData.taskList);
            if (finished) {
              currentChat.loading = false;
              setLoading(false);
              setIsStreaming(false); // 同步流式输出状态

              // 对话完成后，上报完整的multiAgent数据到后端用于历史会话恢复
              if (currentChat.multiAgent && Object.keys(currentChat.multiAgent).length > 0) {
                uploadMultiAgentData(sessionId, requestId, currentChat.multiAgent)
                  .then(() => {
                    console.log('[uploadMultiAgentData] 成功上报multiAgent数据:', {
                      sessionId,
                      requestId,
                      dataSize: JSON.stringify(currentChat.multiAgent).length
                    });
                  })
                  .catch((error) => {
                    console.error('[uploadMultiAgentData] 上报multiAgent数据失败:', error);
                    // 上报失败不影响用户体验，只记录日志
                  });
              }
            }
            const newChatList = [...chatList.current];
            newChatList.splice(newChatList.length - 1, 1, currentChat);
            chatList.current = newChatList;
          }
        });
        scrollToTop(chatRef.current!);
      }
    };

    const openAction = (taskList: MESSAGE.Task[]) => {
      if (
        taskList.filter((t) => !RESULT_TYPES.includes(t.messageType)).length
      ) {
        setShowAction(true);
      }
    };

    const handleError = (error: unknown) => {
      throw error;
    };

    const handleClose = () => {
      console.log("🚀 ~ close");
    };

    // 保存SSE控制器引用
    const controller = querySSE(
      {
        body: params,
        handleMessage,
        handleError,
        handleClose,
      }
    );
    sseControllerRef.current = controller;
  });

  const temporaryChangeTask = (taskList: MESSAGE.Task[]) => {
    const task = taskList[taskList.length - 1] as CHAT.Task;
    if (!["task_summary", "result"].includes(task?.messageType)) {
      setActiveTask(task);
    }
  };

  /**
   * 处理停止生成
   * 中断SSE连接，后端会保存已生成的部分回复
   */
  const handleStopGeneration = () => {
    // 设置停止标志
    if (chatList.current.length > 0) {
      const lastChat = chatList.current[chatList.current.length - 1];
      lastChat.forceStop = true;
      lastChat.loading = false;
    }

    // 更新UI状态
    setLoading(false);
    setIsStreaming(false);

    // 中断SSE连接
    if (sseControllerRef.current) {
      sseControllerRef.current.abort();
      sseControllerRef.current = null;
    }

    console.log('已停止生成');
  };

  const changeTask = (task: CHAT.Task) => {
    actionViewRef.current?.changeActionView(ActionViewItemEnum.follow);
    changeActionStatus(true);
    setActiveTask(task);
  };

  const updatePlan = (plan: CHAT.Plan) => {
    setPlan(plan);
  };

  const changeFile = (file: CHAT.TFile) => {
    changeActionStatus(true);
    actionViewRef.current?.setFilePreview(file);
  };

  const changePlan = () => {
    changeActionStatus(true);
    actionViewRef.current?.openPlanView();
  };

  const changeActionStatus = (status: boolean) => {
    setShowAction(status);
  };

  const sendDataMessage = (inputInfo: any) => {
    const params = {
      content: inputInfo.message,
    };
    const currentChat = {
      query: inputInfo.message,
      loading: true,
      think: "",
      chartData: undefined,
      error: "",
    };
    setDataChatList([...dataChatList, currentChat]);
    scrollToTop(chatRef.current!);

    setChatTitle(inputInfo.message);
    setLoading(true);
    setIsStreaming(true); // 同步流式输出状态

    // 同步用户消息到store（用于新建会话判断）
    const dataMessageId = getUniqId();
    addMessage({
      id: dataMessageId,
      sessionId: sessionId,
      role: 'user',
      content: inputInfo.message,
      createTime: new Date().toISOString()
    });

    const handleMessage = (data: any) => {
      // currentChat.loading = false;
      switch (data.eventType) {
        case "THINK":
          currentChat.think = data.data;
          break;
        case "CHART_DATA":
          currentChat.chartData = data.data;
          break;
        case "ERROR":
          currentChat.error = data.data;
          currentChat.loading = false;
          setLoading(false);
          setIsStreaming(false); // 同步流式输出状态
          break;
        case "READY":
          currentChat.loading = false;
          setLoading(false);
          setIsStreaming(false); // 同步流式输出状态
          break;
      }
      const newChatList = [...dataChatList];
      newChatList.splice(newChatList.length, 1, currentChat);
      setDataChatList(newChatList);
      // 滚动到顶部
      scrollToTop(chatRef.current!);
    };
    const handleError = (error: unknown) => {
      throw error;
    };

    const handleClose = () => {
      console.log("🚀 ~ close");
    };

    // 保存SSE控制器引用
    const controller = querySSE(
      {
        body: params,
        handleMessage,
        handleError,
        handleClose,
      },
      `${SERVICE_BASE_URL}/data/chatQuery`
    );
    sseControllerRef.current = controller;
  };

  /**
   * 模拟SSE推送处理rawMessages
   * 将历史消息中的rawMessages逐条通过combineData处理，
   * 完全复用实时SSE推送的处理逻辑
   * @param rawMessages 原始SSE消息数组
   * @param initialChatItem 初始ChatItem对象
   * @returns 处理后的ChatItem对象
   */
  const simulateSSEFromRawMessages = (
    rawMessages: any[],
    initialChatItem: CHAT.ChatItem
  ): CHAT.ChatItem => {
    console.log('[simulateSSE] 开始模拟SSE推送, 消息数:', rawMessages.length);

    // 复制ChatItem，避免修改原对象
    let currentChat: CHAT.ChatItem = {
      ...initialChatItem,
      multiAgent: { tasks: [] }, // 初始化空的multiAgent
      tasks: [],
      thought: "",
      conclusion: undefined,
      plan: undefined,
      planList: []
    };

    // 分析消息类型分布（用于调试）
    const messageTypeCounts: Record<string, number> = {};
    rawMessages.forEach((msg: any) => {
      const type = msg.messageType;
      messageTypeCounts[type] = (messageTypeCounts[type] || 0) + 1;
    });
    console.log('[simulateSSE] 消息类型分布:', messageTypeCounts);

    // 输出前3条原始消息的完整结构（用于调试数据格式）
    console.log('[simulateSSE] ===== 前3条原始消息完整结构 =====');
    rawMessages.slice(0, 3).forEach((msg, idx) => {
      console.log(`[simulateSSE] 原始消息 ${idx}:`, JSON.parse(JSON.stringify(msg)));
    });

    // 逐条模拟SSE消息推送，调用combineData处理
    rawMessages.forEach((rawMsg: any, index: number) => {
      try {
        // 输出前5条消息的转换详情（调试用）
        if (index < 5) {
          console.log(`[simulateSSE] ===== 消息 ${index} 转换详情 =====`);
          console.log('[simulateSSE] 原始数据:', {
            messageType: rawMsg.messageType,
            messageId: rawMsg.messageId,
            taskId: rawMsg.taskId,
            messageOrder: rawMsg.messageOrder,
            taskOrder: rawMsg.taskOrder,
            finish: rawMsg.finish,
            isFinal: rawMsg.isFinal,
            hasToolThought: !!rawMsg.toolThought,
            toolThoughtLength: rawMsg.toolThought?.length || 0,
            hasResultMap: !!rawMsg.resultMap,
            resultMapType: typeof rawMsg.resultMap,
            resultMapKeys: rawMsg.resultMap ? Object.keys(rawMsg.resultMap) : [],
            hasResult: !!rawMsg.result
          });
        }

        // 将rawMessage转换为SSE eventData格式
        const eventData: MESSAGE.EventData = {
          messageId: rawMsg.messageId,
          messageOrder: rawMsg.messageOrder || index,
          messageType: rawMsg.messageType,
          taskId: rawMsg.taskId,
          taskOrder: rawMsg.taskOrder || 0,
          resultMap: {
            ...rawMsg.resultMap,
            // 将rawMsg中的额外字段合并到resultMap中
            finish: rawMsg.finish,
            isFinal: rawMsg.isFinal,
            toolThought: rawMsg.toolThought,
            result: rawMsg.result
          } as MESSAGE.Task
        };

        // 输出前5条消息转换后的eventData（调试用）
        if (index < 5) {
          console.log('[simulateSSE] 转换后eventData:', {
            messageId: eventData.messageId,
            messageType: eventData.messageType,
            taskId: eventData.taskId,
            resultMapKeys: eventData.resultMap ? Object.keys(eventData.resultMap) : [],
            hasToolThought: !!eventData.resultMap?.toolThought,
            hasResult: !!eventData.resultMap?.result
          });
        }

        // 记录combineData调用前的state
        const beforeTasksLength = currentChat.multiAgent?.tasks?.length || 0;

        // 调用combineData，完全复用实时推送的处理逻辑
        currentChat = combineData(eventData, currentChat);

        // 记录combineData调用后的state（前5条消息）
        const afterTasksLength = currentChat.multiAgent?.tasks?.length || 0;
        if (index < 5 || afterTasksLength > beforeTasksLength) {
          if (afterTasksLength > beforeTasksLength) {
            console.log(`[simulateSSE] 消息 ${index} combineData后tasks增加: ${beforeTasksLength} → ${afterTasksLength}`);
          } else if (index < 5) {
            console.log(`[simulateSSE] 消息 ${index} combineData后tasks无变化:`, beforeTasksLength);
          }
        }

        if (index % 50 === 0 || index === rawMessages.length - 1) {
          console.log(`[simulateSSE] 已处理 ${index + 1}/${rawMessages.length} 条消息, 当前tasks组数: ${afterTasksLength}`);
        }
      } catch (error) {
        console.error(`[simulateSSE] 处理消息 ${index} 失败:`, error);
        console.log(`[simulateSSE] 失败的消息:`, rawMsg);
      }
    });

    console.log('[simulateSSE] SSE模拟完成, multiAgent结构:', {
      hasPlan: !!currentChat.multiAgent.plan,
      thoughtLength: currentChat.multiAgent.plan_thought?.length || 0,
      taskGroupCount: currentChat.multiAgent.tasks?.length || 0
    });

    // 最后调用handleTaskData生成TimeLine数据
    handleTaskData(
      currentChat,
      false, // deepThink=false，历史会话使用普通模式
      currentChat.multiAgent
    );

    console.log('[simulateSSE] handleTaskData处理完成:', {
      thoughtLength: currentChat.thought?.length || 0,
      tasksCount: currentChat.tasks?.length || 0,
      hasConclusion: !!currentChat.conclusion,
      hasPlan: !!currentChat.plan
    });

    console.log('[simulateSSE] ===== 准备返回结果 =====');
    console.log('[simulateSSE] 返回值检查:', {
      hasQuery: !!currentChat.query,
      hasSessionId: !!currentChat.sessionId,
      hasRequestId: !!currentChat.requestId,
      hasResponse: !!currentChat.response,
      tasksLength: currentChat.tasks?.length || 0,
      thoughtLength: currentChat.thought?.length || 0,
      hasConclusion: !!currentChat.conclusion,
      hasPlan: !!currentChat.plan,
      hasPlanList: currentChat.planList?.length || 0,
      hasMultiAgent: !!currentChat.multiAgent,
      multiAgentTasksLength: currentChat.multiAgent?.tasks?.length || 0
    });

    if (currentChat.tasks && currentChat.tasks.length > 0) {
      console.log('[simulateSSE] 返回的tasks预览 (前2组):', currentChat.tasks.slice(0, 2));
    }

    return currentChat;
  };

  /**
   * 转换历史消息为ChatItem格式
   * 将从后端获取的历史消息转换为前端ChatView使用的格式
   */
  const convertHistoryMessages = (messages: any[]): CHAT.ChatItem[] => {
    console.log('[convertHistoryMessages] ===== 开始处理 =====');
    console.log('[convertHistoryMessages] 输入消息数:', messages.length);
    console.log('[convertHistoryMessages] 输入消息详情:', messages);

    try {
      const chatItems: CHAT.ChatItem[] = [];

    // 记录已使用的assistant消息ID，防止重复使用
    const usedAssistantIds = new Set<number>();

    // 按用户消息和AI回复成对处理
    for (let i = 0; i < messages.length; i++) {
      const msg = messages[i];

      if (msg.role === 'user') {
        // 查找对应的AI回复，排除已使用的assistant
        const assistantMsg = messages.find(
          (m: any, idx: number) =>
            idx > i &&
            m.role === 'assistant' &&
            !usedAssistantIds.has(m.id)
        );

        console.log('[convertHistoryMessages] 处理用户消息:', {
          userMsg: msg,
          assistantMsg: assistantMsg,
          hasResponse: !!assistantMsg?.content,
          usedAssistantIds: Array.from(usedAssistantIds)
        });

        // 如果找到assistant，标记为已使用
        if (assistantMsg) {
          usedAssistantIds.add(assistantMsg.id);
          console.log('[convertHistoryMessages] 标记assistant已使用:', assistantMsg.id);
        }

        // 解析完整的会话数据
        let conclusion: CHAT.Task | undefined = undefined;
        let thoughtText = "";
        let tasksList: CHAT.Task[] = [];
        let planData: CHAT.Plan | undefined = undefined;
        let multiAgentData: any = null;
        let metadata: any = null; // 保存metadata到外层作用域

        if (assistantMsg?.content) {
          // 解析文件信息
          const fileList = assistantMsg.files && assistantMsg.files !== 'null'
            ? (typeof assistantMsg.files === 'string' ? JSON.parse(assistantMsg.files) : assistantMsg.files)
            : [];

          // 优先从metadata获取完整数据（新架构）
          let useMetadata = false;
          if (assistantMsg.metadata && assistantMsg.metadata !== 'null') {
            try {
              metadata = typeof assistantMsg.metadata === 'string'
                ? JSON.parse(assistantMsg.metadata)
                : assistantMsg.metadata;

              console.log('[convertHistoryMessages] metadata结构:', {
                hasMultiAgent: !!metadata.multiAgent,
                hasRawMessages: !!metadata.rawMessages,
                source: metadata.source,
                messageCount: metadata.messageCount || metadata.rawMessages?.length
              });

              // 策略1：优先模拟SSE推送处理rawMessages（完全复用实时逻辑）
              if (metadata.rawMessages && Array.isArray(metadata.rawMessages) && metadata.rawMessages.length > 0) {
                // 标记为使用metadata，但不在这里处理
                // 具体处理在后面的if (!useMetadata && metadata.rawMessages)分支中
                useMetadata = true;
                console.log('[convertHistoryMessages] 发现rawMessages, 将模拟SSE推送处理');
              }
              // 策略2：如果有已处理的multiAgent，直接使用
              else if (metadata.multiAgent) {
                multiAgentData = metadata.multiAgent;
                useMetadata = true;
                console.log('[convertHistoryMessages] 使用metadata.multiAgent数据 (source:', metadata.source, ')');
              }
            } catch (e) {
              console.error('[convertHistoryMessages] metadata解析失败:', e);
              console.log('[convertHistoryMessages] 原始metadata:', assistantMsg.metadata);
              console.warn('[convertHistoryMessages] fallback到字段重构');
            }
          }

          // 如果没有metadata或解析失败，则从各字段重构（旧架构fallback）
          if (!useMetadata) {
            console.log('[convertHistoryMessages] 使用字段重构模式 (fallback)');

            // 解析思考过程
            if (assistantMsg.thought && assistantMsg.thought !== 'null') {
              thoughtText = typeof assistantMsg.thought === 'string'
                ? assistantMsg.thought
                : JSON.stringify(assistantMsg.thought);
            }

            // 解析任务详情（JSON数组）
            if (assistantMsg.tasks && assistantMsg.tasks !== 'null') {
              try {
                const tasksData = typeof assistantMsg.tasks === 'string'
                  ? JSON.parse(assistantMsg.tasks)
                  : assistantMsg.tasks;

                if (Array.isArray(tasksData)) {
                  tasksList = tasksData.map((task: any) => ({
                    id: task.id || getUniqId(),
                    messageId: task.messageId || task.id || getUniqId(),
                    requestId: task.id || getUniqId(),
                    messageTime: task.timestamp || assistantMsg.createTime || new Date().toISOString(),
                    messageType: task.messageType || "task",
                    finish: task.finish || false,
                    isFinal: task.finish || false,
                    result: task.result || task.taskSummary || "",
                    resultMap: {
                      ...(task.resultMap || {}),
                      searchResult: task.resultMap?.searchResult ? {
                        ...task.resultMap.searchResult,
                        docs: Array.isArray(task.resultMap.searchResult.docs?.[0])
                          ? task.resultMap.searchResult.docs.flat()
                          : (task.resultMap.searchResult.docs || [])
                      } : undefined,
                      code: task.resultMap?.code
                    },
                    task: task.task,
                    taskSummary: task.taskSummary,
                    toolResult: task.toolResult,
                    children: task.children
                  } as CHAT.Task));
                }
              } catch (e) {
                console.warn('[convertHistoryMessages] 解析tasks失败:', e);
              }
            }

            // 解析计划信息（JSON对象）
            if (assistantMsg.plan && assistantMsg.plan !== 'null') {
              try {
                planData = typeof assistantMsg.plan === 'string'
                  ? JSON.parse(assistantMsg.plan)
                  : assistantMsg.plan;
              } catch (e) {
                console.warn('[convertHistoryMessages] 解析plan失败:', e);
              }
            }
          }

          // 构建conclusion（最终结论）
          conclusion = {
            id: assistantMsg.id?.toString() || getUniqId(),
            messageId: assistantMsg.id?.toString() || getUniqId(),
            requestId: msg.id?.toString() || getUniqId(),
            messageTime: assistantMsg.createTime || new Date().toISOString(),
            messageType: "result",
            finish: true,
            isFinal: true,
            result: assistantMsg.content,
            resultMap: {
              taskSummary: assistantMsg.content,
              fileList: fileList
            }
          } as CHAT.Task;
        }

        // 安全解析用户消息的files字段
        const userFiles = msg.files && msg.files !== 'null'
          ? (typeof msg.files === 'string' ? JSON.parse(msg.files) : msg.files)
          : [];

        // 将tasks列表转换为二维数组格式（按messageType分组）
        let tasksGrouped: CHAT.Task[][] = tasksList.length > 0 ? [tasksList] : [];

        // 将Plan转换为PlanItem[]格式
        const planItems: CHAT.PlanItem[] = [];
        if (planData) {
          // 根据Plan结构转换为PlanItem
          if (planData.steps && Array.isArray(planData.steps)) {
            planItems.push({
              name: planData.title || "执行计划",
              list: planData.steps
            });
          }
        }

        // 输出处理策略选择日志
        console.log('[convertHistoryMessages] 选择处理策略:', {
          hasRawMessages: !!(metadata && metadata.rawMessages && metadata.rawMessages.length > 0),
          hasMultiAgent: !!multiAgentData,
          willUseSSESimulation: !!(metadata && metadata.rawMessages && metadata.rawMessages.length > 0),
          willUseMultiAgentFallback: !(metadata && metadata.rawMessages && metadata.rawMessages.length > 0) && !!multiAgentData
        });

        // 策略1优先：如果有rawMessages，模拟SSE推送（优先级最高）
        if (metadata && metadata.rawMessages && Array.isArray(metadata.rawMessages) && metadata.rawMessages.length > 0) {
          console.log('[convertHistoryMessages] ===== 策略1：模拟SSE推送处理rawMessages =====');
          console.log('[convertHistoryMessages] 消息数:', metadata.rawMessages.length);

          const initialChatItem: CHAT.ChatItem = {
            query: msg.content,
            files: userFiles,
            responseType: "txt",
            sessionId: sessionId,
            requestId: msg.id || getUniqId(),
            loading: false,
            forceStop: false,
            tasks: [],
            thought: "",
            response: assistantMsg?.content || "",
            taskStatus: 1,
            tip: "",
            multiAgent: { tasks: [] },
            plan: undefined,
            planList: [],
            conclusion: undefined
          };

          const processedChatItem = simulateSSEFromRawMessages(
            metadata.rawMessages,
            initialChatItem
          );

          thoughtText = processedChatItem.thought || "";
          tasksGrouped = processedChatItem.tasks || [];
          conclusion = processedChatItem.conclusion;
          planData = processedChatItem.plan;

          planItems.length = 0;
          if (planData && planData.steps && Array.isArray(planData.steps)) {
            planItems.push({
              name: planData.title || "执行计划",
              list: planData.steps
            });
          }

          console.log('[convertHistoryMessages] ===== 策略1完成 =====', {
            thoughtLength: thoughtText.length,
            tasksCount: tasksGrouped.length,
            hasConclusion: !!conclusion,
            hasPlan: !!planData
          });
        }
        // 策略2备选：如果没有rawMessages，但有metadata.multiAgent，使用handleTaskData处理
        else if (multiAgentData) {
          console.log('[convertHistoryMessages] ===== 策略2：handleTaskData处理multiAgent =====');

          // 创建临时ChatItem用于handleTaskData处理
          const tempChatItem: CHAT.ChatItem = {
            query: msg.content,
            files: userFiles,
            responseType: "txt",
            sessionId: sessionId,
            requestId: msg.id || getUniqId(),
            loading: false,
            forceStop: false,
            tasks: [],
            thought: "",
            response: assistantMsg?.content || "",
            taskStatus: 1,
            tip: "",
            multiAgent: multiAgentData,
            plan: undefined,
            planList: [],
            conclusion: undefined
          };

          // 调用handleTaskData处理，生成TimeLine需要的数据结构
          handleTaskData(
            tempChatItem,
            false, // deepThink=false，历史会话使用普通模式
            multiAgentData
          );

          console.log('[convertHistoryMessages] handleTaskData处理完成:', {
            thoughtLength: tempChatItem.thought?.length || 0,
            tasksCount: tempChatItem.tasks?.length || 0,
            hasConclusion: !!tempChatItem.conclusion,
            hasPlan: !!tempChatItem.plan
          });

          // 使用handleTaskData处理后的数据
          thoughtText = tempChatItem.thought || "";
          tasksGrouped = tempChatItem.tasks || [];  // 这是关键！使用处理后的tasks
          conclusion = tempChatItem.conclusion;
          planData = tempChatItem.plan;

          // 重新构建planItems
          planItems.length = 0;
          if (planData && planData.steps && Array.isArray(planData.steps)) {
            planItems.push({
              name: planData.title || "执行计划",
              list: planData.steps
            });
          }
        }

        const chatItem: CHAT.ChatItem = {
          query: msg.content,
          files: userFiles,
          responseType: "txt",
          sessionId: sessionId,
          requestId: msg.id || getUniqId(),
          loading: false,
          forceStop: false,
          tasks: tasksGrouped,  // 二维数组格式
          thought: thoughtText,  // 填充思考过程
          response: assistantMsg?.content || "",
          taskStatus: 1,
          tip: "",
          multiAgent: multiAgentData || {
            tasks: [],  // 历史消息不需要multiAgent结构，保持空数组
            plan: planData  // 填充计划数据
          },
          planList: planItems,  // PlanItem[]格式
          conclusion: conclusion,  // 最终结论
        };
        chatItems.push(chatItem);
        console.log(`[convertHistoryMessages] 添加chatItem ${chatItems.length}:`, {
          requestId: chatItem.requestId,
          query: chatItem.query,
          hasResponse: !!chatItem.response,
          tasksCount: chatItem.tasks?.length || 0,
          thoughtLength: chatItem.thought?.length || 0
        });
      }
    }

      console.log('[convertHistoryMessages] ===== 处理完成 =====');
      console.log('[convertHistoryMessages] 输出chatItems数:', chatItems.length);
      console.log('[convertHistoryMessages] chatItems详情:', chatItems);
      if (chatItems.length > 0) {
        console.log('[convertHistoryMessages] 第一个chatItem预览:', {
          requestId: chatItems[0].requestId,
          query: chatItems[0].query,
          response: chatItems[0].response?.substring(0, 100) + '...',
          tasksCount: chatItems[0].tasks?.length || 0,
          thoughtLength: chatItems[0].thought?.length || 0,
          hasConclusion: !!chatItems[0].conclusion,
          hasPlan: !!chatItems[0].plan
        });
      }

      return chatItems;
    } catch (error) {
      console.error('[convertHistoryMessages] ===== 处理失败 =====');
      console.error('[convertHistoryMessages] 错误类型:', error?.constructor?.name);
      console.error('[convertHistoryMessages] 错误消息:', (error as Error)?.message);
      console.error('[convertHistoryMessages] 错误堆栈:', (error as Error)?.stack);
      console.error('[convertHistoryMessages] 输入消息:', messages);
      return []; // 返回空数组避免完全崩溃
    }
  };

  /**
   * 初始化历史消息
   * 当传入initialMessages时，转换并加载到chatList
   */
  useEffect(() => {
    console.log('[useEffect] initialMessages变化:', {
      length: initialMessages.length,
      sessionId: sessionId,
      messages: initialMessages
    });

    if (initialMessages.length > 0) {
      console.log('[useEffect] 开始初始化历史消息');
      const historyChatList = convertHistoryMessages(initialMessages);
      console.log('[useEffect] 转换后的chatList:', historyChatList);

      chatList.current = historyChatList;
      console.log('[useEffect] 赋值后的chatList.current:', chatList.current);

      // 设置标题为第一条用户消息
      const firstUserMsg = initialMessages.find((msg: any) => msg.role === 'user');
      if (firstUserMsg) {
        setChatTitle(firstUserMsg.content);
        console.log('[useEffect] 设置标题:', firstUserMsg.content);
      }

      // 强制触发重新渲染以显示历史消息
      // 因为chatList是ref，修改它不会自动触发渲染
      console.log('[useEffect] 触发强制渲染');
      setForceUpdate(prev => {
        console.log('[useEffect] forceUpdate from', prev, 'to', prev + 1);
        return prev + 1;
      });

      // 滚动到底部显示最新消息
      setTimeout(() => {
        scrollToTop(chatRef.current!);
      }, 100);
    }
  }, [initialMessages, sessionId]);

  useEffect(() => {
    if (inputInfoProp.message?.length !== 0) {
      product?.type === "dataAgent" && !inputInfoProp.deepThink
        ? sendDataMessage(inputInfoProp)
        : sendMessage(inputInfoProp);
    }
  }, [inputInfoProp, sendMessage]);

  const renderMultAgent = () => {
    console.log('[renderMultAgent] 渲染开始, chatList.current:', chatList.current);
    console.log('[renderMultAgent] chatList.current.length:', chatList.current.length);

    return (
      <div className="h-full w-full flex justify-center">
        <div
          className={classNames("p-24 flex flex-col flex-1 w-0", {
            "max-w-[1200px]": !showAction,
          })}
          id="chat-view"
        >
          <div className="w-full flex justify-between">
            <div className="w-full flex items-center pb-8">
              <Logo />
              <div className="overflow-hidden whitespace-nowrap text-ellipsis text-[16px] font-[500] text-[#27272A] mr-8">
                {chatTitle}
              </div>
              {inputInfoProp.deepThink && (
                <div className="rounded-[4px] px-6 border-1 border-solid border-gray-300 flex items-center shrink-0">
                  <i className="font_family icon-shendusikao mr-6 text-[12px]"></i>
                  <span className="ml-[-4px]">深度研究</span>
                </div>
              )}
            </div>
          </div>
          <div
            className="w-full flex-1 overflow-auto no-scrollbar mb-[36px]"
            ref={chatRef}
          >
            {chatList.current.map((chat, index) => {
              console.log(`[renderMultAgent] 渲染Dialogue ${index}:`, chat);
              return (
                <div key={chat.requestId}>
                  <Dialogue
                    chat={chat}
                    deepThink={inputInfoProp.deepThink}
                    changeTask={changeTask}
                    changeFile={changeFile}
                    changePlan={changePlan}
                  />
                </div>
              );
            })}
          </div>

          {/* 停止生成按钮 */}
          {loading && (
            <div className="w-full flex justify-center mb-4">
              <button
                onClick={handleStopGeneration}
                className="px-6 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg shadow-md transition-all flex items-center gap-2 text-sm font-medium"
              >
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <rect x="6" y="6" width="8" height="8" />
                </svg>
                停止生成
              </button>
            </div>
          )}

          <GeneralInput
            placeholder={
              loading ? "任务进行中" : "希望 Genie 为你做哪些任务呢？"
            }
            showBtn={false}
            size="medium"
            disabled={loading}
            product={product}
            // 多轮问答也不支持切换deepThink，使用传进来的
            send={(info) =>
              sendMessage({
                ...info,
                deepThink: inputInfoProp.deepThink,
              })
            }
          />
        </div>
        {contextHolder}
        <div
          className={classNames("transition-all w-0", {
            "opacity-0 overflow-hidden": !showAction,
            "flex-1": showAction,
          })}
        >
          <ActionView
            activeTask={activeTask}
            taskList={taskList}
            plan={plan}
            ref={actionViewRef}
            onClose={() => changeActionStatus(false)}
          />
        </div>
      </div>
    );
  };

  const renderDataAgent = () => {
    return (
      <div
        className={classNames("p-24 flex flex-col flex-1 w-0 max-w-[1200px]")}
      >
        <div className="w-full flex justify-between">
          <div className="w-full flex items-center pb-8">
            <Logo />
            <div className="overflow-hidden whitespace-nowrap text-ellipsis text-[16px] font-[500] text-[#27272A] mr-8">
              {chatTitle}
            </div>
          </div>
        </div>
        <div
          className="w-full flex-1 overflow-auto no-scrollbar mb-[36px]"
          ref={chatRef}
        >
          {dataChatList.map((chat, index) => {
            return (
              <div key={index}>
                <DataDialogue chat={chat} />
              </div>
            );
          })}
        </div>
        <GeneralInput
          placeholder={loading ? "任务进行中" : "希望 Genie 为你做哪些任务呢？"}
          showBtn={false}
          size="medium"
          disabled={loading}
          product={product}
          send={(info) =>
            sendDataMessage({
              ...info,
            })
          }
        />
      </div>
    );
  };

  return (
    <div className="h-full w-full flex justify-center">
      {product?.type === "dataAgent" && !inputInfoProp.deepThink
        ? renderDataAgent()
        : renderMultAgent()}
    </div>
  );
};

export default ChatView;
