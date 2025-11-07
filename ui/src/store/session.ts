import { create } from "zustand";
import {
  Session,
  Message,
  getSessions,
  createSession,
  deleteSession,
  getSessionMessages,
} from "@/api/chat";
import { message } from "antd";

/**
 * 会话状态类型定义
 */
interface SessionState {
  // 当前会话ID
  currentSessionId: string | null;

  // 会话列表
  sessions: Session[];

  // 当前会话的消息列表
  messages: Message[];

  // 加载状态
  loading: boolean;

  // 消息流式输出状态
  isStreaming: boolean;

  /**
   * 设置当前会话ID
   * @internal 内部方法，外部组件请使用URL参数跳转: navigate(`/?sessionId=${sessionId}`)
   */
  setCurrentSessionId: (sessionId: string | null) => void;

  /**
   * 设置流式输出状态
   */
  setIsStreaming: (isStreaming: boolean) => void;

  /**
   * 获取会话列表
   */
  fetchSessions: () => Promise<void>;

  /**
   * 创建新会话
   */
  createNewSession: (title?: string) => Promise<Session | null>;

  /**
   * 删除会话
   */
  removeSession: (sessionId: string | number) => Promise<void>;

  /**
   * 切换会话
   * @deprecated 已废弃，推荐使用URL参数跳转: navigate(`/?sessionId=${sessionId}`)
   * 该方法将在未来版本中移除
   */
  switchSession: (sessionId: string) => Promise<void>;

  /**
   * 获取会话消息
   * @deprecated 已废弃，由Home组件通过loadHistorySession统一处理
   * 该方法将在未来版本中移除
   */
  fetchMessages: (sessionId: string) => Promise<void>;

  /**
   * 清空当前会话
   */
  clearCurrentSession: () => void;

  /**
   * 添加消息到当前会话
   * 用于ChatView发送消息时同步状态
   */
  addMessage: (message: Message) => void;
}

/**
 * 会话状态管理Store
 * 使用zustand管理会话相关状态
 */
export const useSessionStore = create<SessionState>((set, get) => ({
  // 初始状态
  currentSessionId: null,
  sessions: [],
  messages: [],
  loading: false,
  isStreaming: false,

  /**
   * 设置当前会话ID
   * @internal 内部方法，外部组件请使用URL参数跳转: navigate(`/?sessionId=${sessionId}`)
   */
  setCurrentSessionId: (sessionId) => {
    set({ currentSessionId: sessionId });
  },

  /**
   * 设置流式输出状态
   */
  setIsStreaming: (isStreaming) => {
    set({ isStreaming });
  },

  /**
   * 获取会话列表
   */
  fetchSessions: async () => {
    try {
      set({ loading: true });
      const response = await getSessions();
      set({
        sessions: response.list || [],
        loading: false,
      });
    } catch (error: any) {
      console.error("获取会话列表失败:", error);
      message.error(error.message || "获取会话列表失败");
      set({ loading: false });
    }
  },

  /**
   * 创建新会话
   */
  createNewSession: async (title?: string) => {
    try {
      set({ loading: true });
      const newSession = await createSession(title);

      // 将新会话添加到列表顶部
      set((state) => ({
        sessions: [newSession, ...state.sessions],
        currentSessionId: newSession.sessionId,
        messages: [],
        loading: false,
      }));

      message.success("新会话已创建");
      return newSession;
    } catch (error: any) {
      console.error("创建会话失败:", error);
      message.error(error.message || "创建会话失败");
      set({ loading: false });
      return null;
    }
  },

  /**
   * 删除会话
   */
  removeSession: async (sessionId: string | number) => {
    try {
      await deleteSession(sessionId);

      // 从列表中移除该会话
      set((state) => {
        const newSessions = state.sessions.filter(
          (session) =>
            session.id !== sessionId && session.sessionId !== sessionId
        );

        // 如果删除的是当前会话，清空当前会话
        const newCurrentSessionId =
          state.currentSessionId === sessionId ? null : state.currentSessionId;

        return {
          sessions: newSessions,
          currentSessionId: newCurrentSessionId,
          messages: newCurrentSessionId ? state.messages : [],
        };
      });

      message.success("会话已删除");
    } catch (error: any) {
      console.error("删除会话失败:", error);
      message.error(error.message || "删除会话失败");
    }
  },

  /**
   * 切换会话
   * @deprecated 已废弃，推荐使用URL参数跳转: navigate(`/?sessionId=${sessionId}`)
   * 该方法将在未来版本中移除
   */
  switchSession: async (sessionId: string) => {
    console.warn(
      "switchSession已废弃，推荐使用URL参数跳转: navigate(`/?sessionId=${sessionId}`)"
    );
    set({ currentSessionId: sessionId });
    await get().fetchMessages(sessionId);
  },

  /**
   * 获取会话消息
   * @deprecated 已废弃，由Home组件通过loadHistorySession统一处理
   * 该方法将在未来版本中移除
   */
  fetchMessages: async (sessionId: string) => {
    console.warn(
      "fetchMessages已废弃，由Home组件通过loadHistorySession统一处理"
    );
    try {
      set({ loading: true });
      const messages = await getSessionMessages(sessionId);
      set({
        messages,
        loading: false,
      });
    } catch (error: any) {
      console.error("获取消息列表失败:", error);
      message.error(error.message || "获取消息列表失败");
      set({ loading: false });
    }
  },

  /**
   * 清空当前会话
   */
  clearCurrentSession: () => {
    set({
      currentSessionId: null,
      messages: [],
    });
  },

  /**
   * 添加消息到当前会话
   * 用于ChatView发送消息时同步状态
   */
  addMessage: (message) => {
    set((state) => ({
      messages: [...state.messages, message],
    }));
  },
}));
