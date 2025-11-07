import { useEffect, useState, useRef, useMemo } from "react";
import {
  getUniqId,
  scrollToTop,
  ActionViewItemEnum,
  getSessionId,
} from "@/utils";
import querySSE, { SSEController } from "@/utils/querySSE";
import { handleTaskData, combineData, createChat } from "@/utils/chat";
import Dialogue from "@/components/Dialogue";
import DataDialogue from "@/components/Dialogue/DataDialogue";
import GeneralInput from "@/components/GeneralInput";
import ActionView from "@/components/ActionView";
import { RESULT_TYPES, AGENT_NAME } from "@/utils/constants";
import { useMemoizedFn } from "ahooks";
import classNames from "classnames";
import Logo from "../Logo";
import { Modal, message } from "antd";
import { useSessionStore } from "@/store/session";
import { processAssistantHistoryMessage } from "@/utils/historyConverter";
import { useAgentProviderStore } from "@/store/agentProvider";

type Props = {
  // 用户输入
  inputInfo: CHAT.TInputInfo;
  product?: CHAT.Product;
  // 历史会话ID
  initialSessionId?: string | null;
  // 历史会话内容
  initialMessages?: any[];
};

const ChatView: GenieType.FC<Props> = (props) => {
  const {
    inputInfo: inputInfoProp,
    product,
    initialSessionId,
    initialMessages = [],
  } = props;

  useEffect(() => {
    if (inputInfoProp.message?.length !== 0) {
      product?.type === "dataAgent" && !inputInfoProp.deepThink
        ? sendDataMessage(inputInfoProp)
        : sendMessage(inputInfoProp);
    }
  }, [inputInfoProp]);

  useEffect(() => {
    // 处理历史会话数据
    historyToMessage(initialMessages);
  }, [initialMessages]);

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
  const actionViewRef = ActionView.useActionView();

  // 优先使用传入的sessionId，否则生成新的
  const sessionId = useMemo(() => {
    return initialSessionId || getSessionId();
  }, [initialSessionId]);

  const [modal, contextHolder] = Modal.useModal();

  // SSE连接控制器，用于中断连接
  const sseControllerRef = useRef<SSEController | null>(null);

  // 获取会话store中的setIsStreaming、addMessage和fetchSessions方法，用于同步流式输出状态和消息
  const { setIsStreaming, addMessage, fetchSessions } = useSessionStore();

  // 智能体状态管理
  const { providers, currentProvider, setCurrentProvider, fetchProviders } =
    useAgentProviderStore();

  const [selectedProviderId, setSelectedProviderId] = useState<number>();

  // 初始化：获取智能体配置
  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  // 设置默认选中的智能体
  useEffect(() => {
    if (currentProvider) {
      setSelectedProviderId(currentProvider.id);
    }
  }, [currentProvider]);

  // 处理智能体切换
  const handleProviderChange = (providerId: number) => {
    // 历史会话中禁止切换智能体
    if (initialSessionId) {
      message.warning("历史会话中无法切换智能体，请创建新会话");
      return;
    }

    setSelectedProviderId(providerId);
    const provider = providers.find((p) => p.id === providerId);
    if (provider) {
      setCurrentProvider(provider);
      message.success(`已切换到智能体: ${provider.providerName}`);
    }
  };

  const openAction = (taskList: MESSAGE.Task[]) => {
    if (taskList.filter((t) => !RESULT_TYPES.includes(t.messageType)).length) {
      setShowAction(true);
    }
  };

  const historyToMessage = (historyMessageList: MESSAGE.History[]) => {
    if (historyMessageList && historyMessageList.length > 0) {
      try {
        setChatTitle(historyMessageList[0].content);
        console.log("[历史会话] 开始处理历史数据:", historyMessageList);
        if (product?.type === "dataAgent") {
          // TODO
        } else {
          const historyChatList: CHAT.ChatItem[] = [];

          // 遍历历史消息，奇数项是用户问题，偶数项是AI回答
          for (let i = 0; i < historyMessageList.length; i += 2) {
            const userMessage = historyMessageList[i];
            const assistantMessage = historyMessageList[i + 1];

            if (!userMessage || userMessage.role !== "user") {
              continue;
            }
            const chatItem = createChat(
              {
                deepThink: !!userMessage.deepThink,
                message: userMessage.content,
                files: userMessage.files
                  ? JSON.parse(userMessage.files)
                  : undefined,
              },
              userMessage.sessionId,
              String(userMessage.id)
            );

            // 如果有对应的AI回答，则处理回答数据
            if (assistantMessage && assistantMessage.role === "assistant") {
              const taskData = processAssistantHistoryMessage(
                chatItem,
                assistantMessage
              );

              if (taskData) {
                setTaskList(taskData.taskList);
                temporaryChangeTask(taskData.taskList);
                updatePlan(taskData.plan!);
                openAction(taskData.taskList);
              }

              console.log("[历史会话] 处理后的对话数据:", chatItem);
            }
            chatItem.tasks = [];
            historyChatList.push(chatItem);
          }

          chatList.current = historyChatList;
        }
      } catch (error) {
        console.error("[历史会话] 历史数据处理失败:", error);
      }
    }
  };

  const sendMessage = useMemoizedFn((inputInfo: CHAT.TInputInfo) => {
    const { message, deepThink, outputStyle, agentProviderId } = inputInfo;
    const requestId = getUniqId();
    let currentChat = createChat(inputInfo, sessionId, requestId);
    currentChat.loading = true;
    // currentChat.tip = "已接收到你的任务，将立即开始处理...";
    currentChat.tip = "";
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
      role: "user",
      content: message!,
      createTime: new Date().toISOString(),
    });

    const params = {
      sessionId: sessionId,
      requestId: requestId,
      query: message,
      deepThink: deepThink ? 1 : 0,
      outputStyle,
      agentProviderId: agentProviderId || selectedProviderId, // 优先使用传入的，fallback到内部state
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
              // 刷新左侧历史对话列表
              fetchSessions();
            }
            const newChatList = [...chatList.current];
            newChatList.splice(newChatList.length - 1, 1, currentChat);
            chatList.current = newChatList;
          }
        });
        scrollToTop(chatRef.current!);
      }
    };

    const handleError = (error: unknown) => {
      throw error;
    };

    const handleClose = () => {
      console.log("🚀 ~ close");
    };

    // 保存SSE控制器引用
    const controller = querySSE({
      body: params,
      handleMessage,
      handleError,
      handleClose,
    });
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

    // 刷新左侧历史对话列表
    fetchSessions();

    console.log("已停止生成");
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
      role: "user",
      content: inputInfo.message,
      createTime: new Date().toISOString(),
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
          // 刷新左侧历史对话列表
          fetchSessions();
          break;
        case "READY":
          currentChat.loading = false;
          setLoading(false);
          setIsStreaming(false); // 同步流式输出状态
          // 刷新左侧历史对话列表
          fetchSessions();
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

  const renderMultAgent = () => {
    console.log("[渲染多智能体] 渲染开始", chatList.current);

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
                <svg
                  className="w-4 h-4"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <rect
                    x="6"
                    y="6"
                    width="8"
                    height="8"
                  />
                </svg>
                停止生成
              </button>
            </div>
          )}

          <GeneralInput
            placeholder={
              loading ? "任务进行中" : `希望 ${AGENT_NAME} 为你做哪些任务呢？`
            }
            showBtn={currentProvider?.providerType === "default"} // 只有默认智能体显示深度研究
            size="medium"
            disabled={loading}
            product={
              currentProvider?.providerType === "default" ? product : undefined
            } // 非默认不传product
            agentProviderId={selectedProviderId}
            agentProviders={providers}
            onAgentChange={handleProviderChange}
            isHistorySession={!!initialSessionId}
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
          placeholder={loading ? "任务进行中" : `希望 ${AGENT_NAME} 为你做哪些任务呢？`}
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
