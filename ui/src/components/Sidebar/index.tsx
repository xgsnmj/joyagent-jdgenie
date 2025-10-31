import { memo, useEffect } from 'react';
import { Button, Spin } from 'antd';
import { PlusOutlined, HistoryOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/store/session';
import SessionItem from './SessionItem';
import UserCard from './UserCard';

/**
 * Sidebar组件Props
 */
interface SidebarProps {
  className?: string;
}

/**
 * 侧边栏组件
 * 显示会话列表、新建会话按钮和用户信息
 */
const Sidebar: GenieType.FC<SidebarProps> = memo(({ className = '' }) => {
  const navigate = useNavigate();
  const {
    sessions,
    currentSessionId,
    messages,
    loading,
    isStreaming,
    fetchSessions,
    createNewSession,
    removeSession,
    clearCurrentSession,
  } = useSessionStore();

  /**
   * 组件挂载时获取会话列表
   */
  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  /**
   * 监听Ctrl+K快捷键创建新会话
   */
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        handleNewSession();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  /**
   * 处理新建会话
   * 逻辑说明：
   * 1. 未输入内容时：可点击，清空状态并回到首页，不创建新会话
   * 2. 正在输出时：已禁用，不会触发
   * 3. 有内容且输出完成时：创建新会话，当前对话自动保存
   */
  const handleNewSession = async () => {
    // 如果正在输出，不处理（按钮已禁用，这里是双重保险）
    if (isStreaming) {
      return;
    }

    // 如果当前会话没有消息，清空状态并回到首页（不创建新会话）
    if (messages.length === 0) {
      // 清空当前会话状态
      clearCurrentSession();
      // 导航到首页
      navigate('/');
      return;
    }

    // 有消息且输出完成：创建新会话，当前对话自动保存
    await createNewSession();
    // 创建成功后跳转到首页
    navigate('/');
  };

  /**
   * 处理会话点击
   * 跳转到首页并通过URL参数传递sessionId，复用历史会话加载逻辑
   */
  const handleSessionClick = (sessionId: string) => {
    // 跳转到首页，URL参数会触发Home组件加载历史会话
    navigate(`/?sessionId=${sessionId}`);
  };

  /**
   * 处理会话删除
   */
  const handleSessionDelete = async (sessionId: string | number) => {
    await removeSession(sessionId);
  };

  /**
   * 查看全部历史
   */
  const handleViewAllHistory = () => {
    navigate('/chat/history');
  };

  /**
   * 显示最近的10条会话
   */
  const recentSessions = sessions.slice(0, 10);

  /**
   * 判断按钮是否可用
   * 只有在正在输出时才禁用按钮
   */
  const isButtonEnabled = !isStreaming;

  return (
    <div className={`flex flex-col h-full bg-gradient-to-br from-slate-50 via-blue-50/30 to-purple-50/20 ${className}`}>
      {/* 顶部：Logo和新建会话按钮 */}
      <div className="p-5 pb-4">
        {/* Logo区域 */}
        <div className="mb-6 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-3xl bg-gradient-to-br from-[#4040ff] to-[#764ba2] shadow-xl shadow-[#4040ff]/30 mb-4">
            <svg className="w-9 h-9 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
            </svg>
          </div>
          <h1 className="text-3xl font-black bg-gradient-to-r from-[#4040ff] via-[#5050ff] to-[#764ba2] bg-clip-text text-transparent tracking-tight leading-tight">
            JD Genie
          </h1>
          <p className="text-sm text-gray-600 mt-1.5 font-semibold">智能问答助手</p>
        </div>

        {/* 新建会话按钮 */}
        <Button
          type="primary"
          icon={<PlusOutlined className="text-xl font-black" />}
          block
          size="large"
          onClick={handleNewSession}
          disabled={!isButtonEnabled}
          className={`group relative border-none rounded-2xl h-20 px-6 py-5 font-black text-lg overflow-hidden transition-all duration-300 ${
            isButtonEnabled
              ? 'bg-gradient-to-r from-[#4040ff] via-[#5050ff] to-[#4040ff] hover:from-[#3535ee] hover:via-[#4545ee] hover:to-[#3535ee] shadow-xl shadow-[#4040ff]/30 hover:shadow-2xl hover:shadow-[#4040ff]/40 hover:scale-[1.02] cursor-pointer'
              : 'bg-gray-300 shadow-md cursor-not-allowed opacity-60'
          }`}
        >
          <span className="relative z-10 font-black text-lg">新建会话</span>
          <span className="ml-2 text-sm opacity-75 font-medium relative z-10">Ctrl K</span>
          {isButtonEnabled && (
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700"></div>
          )}
        </Button>
      </div>

      {/* 中间：历史会话列表 */}
      <div className="flex-1 overflow-y-auto px-3 py-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent">
        {/* 列表标题 */}
        <div className="px-2 mb-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-1 h-5 bg-gradient-to-b from-[#4040ff] to-[#764ba2] rounded-full"></div>
            <span className="text-sm font-black text-gray-800 uppercase tracking-wider">最近对话</span>
          </div>
          {sessions.length > 10 && (
            <Button
              type="text"
              size="small"
              icon={<HistoryOutlined />}
              onClick={handleViewAllHistory}
              className="text-xs text-gray-500 hover:text-[#4040ff] hover:bg-[#4040ff]/5 h-7 px-2 rounded-lg font-medium"
            >
              全部
            </Button>
          )}
        </div>

        {/* 会话列表内容 */}
        {loading ? (
          <div className="flex flex-col justify-center items-center py-16">
            <Spin size="large" />
            <p className="text-sm text-gray-500 mt-4">加载中...</p>
          </div>
        ) : recentSessions.length === 0 ? (
          <div className="px-4 py-12">
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-gray-100 to-gray-50 mb-4">
                <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <p className="text-sm font-semibold text-gray-700 mb-1">暂无会话记录</p>
              <p className="text-xs text-gray-500">
                点击上方"新建会话"按钮<br />
                或按 <kbd className="px-2 py-0.5 text-xs bg-gray-200 rounded">⌘K</kbd> 开始对话
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-1.5">
            {recentSessions.map((session) => (
              <SessionItem
                key={session.sessionId}
                session={session}
                isActive={session.sessionId === currentSessionId}
                onClick={() => handleSessionClick(session.sessionId)}
                onDelete={() => handleSessionDelete(session.sessionId)}
              />
            ))}
          </div>
        )}
      </div>

      {/* 底部：用户信息卡片 */}
      <div className="p-4 pt-3 border-t border-gray-200/60 bg-gradient-to-t from-white/60 via-white/30 to-transparent backdrop-blur-sm">
        <UserCard />
      </div>
    </div>
  );
});

Sidebar.displayName = 'Sidebar';

export default Sidebar;
