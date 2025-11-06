import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/store/session';

/**
 * 会话管理相关的自定义Hook
 * 负责处理会话的创建、切换、删除等操作
 */
export const useSessionManager = () => {
  const navigate = useNavigate();
  const {
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    createNewSession,
    removeSession,
    clearCurrentSession,
  } = useSessionStore();

  /**
   * 处理新建会话
   * 逻辑说明：
   * 1. 未输入内容时：可点击，清空状态并回到首页，不创建新会话
   * 2. 正在输出时：已禁用，不会触发
   * 3. 有内容且输出完成时：创建新会话，当前对话自动保存
   */
  const handleNewSession = useCallback(async () => {
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
  }, [isStreaming, messages.length, clearCurrentSession, createNewSession, navigate]);

  /**
   * 处理会话点击
   * 跳转到首页并通过URL参数传递sessionId，复用历史会话加载逻辑
   */
  const handleSessionClick = useCallback((sessionId: string) => {
    // 跳转到首页，URL参数会触发Home组件加载历史会话
    navigate(`/?sessionId=${sessionId}`);
  }, [navigate]);

  /**
   * 处理会话删除
   */
  const handleSessionDelete = useCallback(async (sessionId: string | number) => {
    await removeSession(sessionId);
  }, [removeSession]);

  /**
   * 查看全部历史
   */
  const handleViewAllHistory = useCallback(() => {
    navigate('/chat/history');
  }, [navigate]);

  /**
   * 获取最近的会话列表（默认10条）
   */
  const getRecentSessions = useCallback((limit: number = 10) => {
    return sessions.slice(0, limit);
  }, [sessions]);

  /**
   * 判断新建会话按钮是否可用
   * 只有在正在输出时才禁用按钮
   */
  const isCreateButtonEnabled = !isStreaming;

  return {
    // 状态
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    isCreateButtonEnabled,

    // 操作方法
    handleNewSession,
    handleSessionClick,
    handleSessionDelete,
    handleViewAllHistory,
    getRecentSessions,
  };
};