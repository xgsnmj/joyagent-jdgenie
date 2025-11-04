import { useState, useCallback, memo, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { message, Spin } from "antd";
import GeneralInput from "@/components/GeneralInput";
import Slogn from "@/components/Slogn";
import ChatView from "@/components/ChatView";
import DataListDrawer from "@/components/DataListDrawer";
import ColsAndDataDrawer from "@/components/DataListDrawer/ColsAndDataDrawer";
import { getSessionMessages } from "@/api/chat";
import { useAgentProviderStore } from "@/store/agentProvider";

import { productList, defaultProduct, chatQustions } from "@/utils/constants";
import classNames from "classnames";

type HomeProps = Record<string, never>;

const Home: GenieType.FC<HomeProps> = memo(() => {
  const [searchParams] = useSearchParams();
  const {
    providers,
    currentProvider,
    setCurrentProvider,
    fetchProviders
  } = useAgentProviderStore();  // 获取完整的智能体状态
  const [inputInfo, setInputInfo] = useState<CHAT.TInputInfo>({
    message: "",
    deepThink: false,
  });
  const [product, setProduct] = useState(defaultProduct);
  const [dbsShow, setDbsShow] = useState(false);
  const [dataShow, setDataShow] = useState(false);
  const [curModel, setCurModel] = useState<CHAT.ModelInfo>({
    modelName: "",
    modelCode: "",
    schemaList: [],
  });

  // 历史会话相关状态
  const [loadedSessionId, setLoadedSessionId] = useState<string | null>(null);
  const [historyMessages, setHistoryMessages] = useState<any[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // 智能体选择状态
  const [selectedProviderId, setSelectedProviderId] = useState<number>();

  const changeInputInfo = useCallback((info: CHAT.TInputInfo) => {
    setInputInfo(info);
  }, []);

  const toSendMessage = useCallback((query: Record<string, any>) => {
    setInputInfo({
      message: query.label,
      outputStyle: "dataAgent",
      deepThink: query.type === 2,
    });
  }, []);

  const showDetail = useCallback((modelInfo: any) => {
    setCurModel(modelInfo);
    setDataShow(true);
  }, []);

  /**
   * 处理智能体切换
   * 首页只允许切换，不需要检查历史会话
   */
  const handleProviderChange = useCallback((providerId: number) => {
    setSelectedProviderId(providerId);
    const provider = providers.find((p) => p.id === providerId);
    if (provider) {
      setCurrentProvider(provider);
      message.success(`已切换到智能体: ${provider.providerName}`);
    }
  }, [providers, setCurrentProvider]);

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

  /**
   * 加载历史会话
   * 从URL参数中获取sessionId并加载该会话的历史消息
   * 注意：
   * 1. 不设置inputInfo，避免触发ChatView重新发送消息
   * 2. 不清空URL参数，URL是sessionId的唯一数据源
   * 3. 纯数据加载函数，不修改URL状态
   */
  const loadHistorySession = useCallback(async (sessionId: string) => {
    try {
      setLoadingHistory(true);
      const messages = await getSessionMessages(sessionId);

      if (messages && messages.length > 0) {
        // 只设置历史消息，不设置inputInfo
        // 这样ChatView会直接显示历史消息，而不会重新发送
        setHistoryMessages(messages);
        setLoadedSessionId(sessionId);
        // 保持URL中的sessionId参数，作为唯一数据源
        console.log('历史会话加载成功:', { sessionId, messageCount: messages.length });
      }
    } catch (error: any) {
      console.error('加载历史会话失败:', error);
      message.error(error.message || '加载历史会话失败');
    } finally {
      setLoadingHistory(false);
    }
  }, []);

  /**
   * 监听URL参数变化，自动加载或清理历史会话
   */
  useEffect(() => {
    const sessionId = searchParams.get('sessionId');

    // 情况1：URL有sessionId，且不是当前已加载的会话 → 加载新的历史会话
    if (sessionId && sessionId !== loadedSessionId) {
      console.log('检测到sessionId参数，加载历史会话:', sessionId);
      loadHistorySession(sessionId);
    }

    // 情况2：URL没有sessionId，但当前有已加载的会话ID → 清理历史会话状态
    // 这种情况发生在：用户查看历史会话后，点击"新建会话"或手动清空URL
    // 注意：只根据loadedSessionId判断，避免循环依赖
    else if (!sessionId && loadedSessionId) {
      console.log('检测到URL无sessionId参数，清理历史会话状态');
      setHistoryMessages([]);
      setLoadedSessionId(null);
      setInputInfo({ message: "", deepThink: false });
    }
  }, [searchParams, loadedSessionId, loadHistorySession]);

  const renderContent = () => {
    // 显示加载历史会话的loading状态
    if (loadingHistory) {
      return (
        <div className="flex items-center justify-center h-full">
          <Spin size="large" tip="加载历史会话中..." />
        </div>
      );
    }

    // 如果有输入内容或已加载历史消息，显示ChatView
    if (inputInfo.message.length > 0 || historyMessages.length > 0) {
      // 如果是加载历史会话（有历史消息且没有新输入），传入空的inputInfo
      // 这样ChatView只会显示历史消息，不会触发新的对话
      const chatInputInfo = historyMessages.length > 0 && inputInfo.message.length === 0
        ? { message: "", deepThink: false }
        : inputInfo;

      return (
        <ChatView
          inputInfo={chatInputInfo}
          product={product}
          initialSessionId={loadedSessionId}
          initialMessages={historyMessages}
        />
      );
    }

    // 默认显示首页欢迎界面
    return (
      <div className="flex flex-col items-center">
        <Slogn />
        <div className="w-640 rounded-xl shadow-[0_18px_39px_0_rgba(198,202,240,0.1)]">
          <GeneralInput
            placeholder={product.placeholder}
            showBtn={currentProvider?.providerType === 'default'}  // 只有默认智能体显示深度研究
            size="big"
            disabled={false}
            product={currentProvider?.providerType === 'default' ? product : undefined}  // 非默认不传product
            send={changeInputInfo}
            dbsShow={setDbsShow}
            // 智能体相关props
            agentProviderId={selectedProviderId}
            agentProviders={providers}
            onAgentChange={handleProviderChange}
            isHistorySession={false}  // 首页不是历史会话
          />
        </div>
        {/* 输出模式选择 - 仅系统默认智能体显示 */}
        {(!currentProvider || currentProvider.providerType === 'default') && (
          <div className="w-640 flex justify-between mt-[16px]">
            {productList.map((item, i) => (
              <div
                key={i}
                className={`flex-1 h-[36px] cursor-pointer flex items-center justify-center border rounded-[8px] ${item.type === product.type ? "border-[#4040ff] bg-[rgba(64,64,255,0.02)] text-[#4040ff]" : "border-[#E9E9F0] text-[#666]"} ${i < productList.length - 1 ? "mr-[12px]" : ""}`}
                onClick={() => setProduct(item)}
              >
                <i className={`font_family ${item.img} ${item.color}`}></i>
                <div className="ml-[6px]">{item.name}</div>
              </div>
            ))}
          </div>
        )}
        <div className="mt-80 mb-120 relative">
          {/* 漂浮的建议问题 */}
          <div
            className={classNames("absolute top-[-45px] p-0 w-full overflow-hidden transition-all duration-400 opacity-0", { "opacity-100 top-[-65px]": product.type === "dataAgent" })}
          >
            <div className="flex gap-x-[12px] justify-center ">
              {chatQustions.map((item, i) => (
                <div
                  key={i}
                  className="text-[#52525B] cursor-pointer border border-[#E9E9F0] rounded-[8px] px-[16px] py-[4px] text-[14px] whitespace-nowrap flex items-center gap-[3px]"
                  onClick={() => toSendMessage(item)}
                >
                  {item.type === 2 && <i className="font_family icon-shendusikao"></i>}
                  {item.label}
                </div>
              ))}
            </div>
          </div>
        </div>
        {/* 模型列表 */}
        <DataListDrawer show={dbsShow} dbsShow={setDbsShow} showDetail={showDetail}></DataListDrawer>
        {/* 列字段和数据 */}
        {dataShow && <ColsAndDataDrawer show={dataShow} dataShow={setDataShow} modelInfo={curModel}></ColsAndDataDrawer>}
      </div>
    );
  };

  return <div className="h-full flex flex-col items-center justify-center">{renderContent()}</div>;
});

Home.displayName = "Home";

export default Home;
