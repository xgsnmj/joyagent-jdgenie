import { memo, useEffect } from "react";
import { useSessionStore } from "@/store/session";
import { useSessionManager } from "@/hooks/useSessionManager";
import { useSidebarShortcuts } from "@/hooks/useKeyboardShortcuts";
import LogoSection from "./LogoSection";
import NewSessionButton from "./NewSessionButton";
import SessionListSection from "./SessionListSection";
import UserCard from "./UserCard";

/**
 * Sidebar组件Props
 */
interface SidebarProps {
  className?: string;
}

/**
 * 侧边栏组件
 * 显示会话列表、新建会话按钮和用户信息
 * 遵循单一职责原则，将各个功能模块拆分为独立的组件和Hook
 */
const Sidebar: GenieType.FC<SidebarProps> = memo(({ className = "" }) => {
  const { fetchSessions, sessions, currentSessionId, loading } =
    useSessionStore();

  // 使用会话管理Hook处理所有会话相关逻辑
  const {
    handleNewSession,
    handleSessionClick,
    handleSessionDelete,
    handleViewAllHistory,
    getRecentSessions,
    isCreateButtonEnabled,
  } = useSessionManager();

  // 使用键盘快捷键Hook处理快捷键逻辑
  useSidebarShortcuts(handleNewSession);

  /**
   * 组件挂载时获取会话列表
   */
  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  /**
   * 获取最近的会话列表
   */
  const recentSessions = getRecentSessions(10);

  return (
    <div
      className={`flex flex-col h-full bg-gradient-to-br from-slate-50 via-blue-50/30 to-purple-50/20 ${className}`}
    >
      {/* 顶部：Logo和新建会话按钮 */}
      <div className="p-5 pb-4">
        {/* Logo区域组件 */}
        <LogoSection />

        {/* 新建会话按钮组件 */}
        <NewSessionButton
          enabled={isCreateButtonEnabled}
          onClick={handleNewSession}
        />
      </div>

      {/* 中间：历史会话列表组件 */}
      <div className="flex-1 overflow-y-auto px-3 py-2 scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-transparent">
        <SessionListSection
          sessions={recentSessions}
          currentSessionId={currentSessionId}
          loading={loading}
          onSessionClick={handleSessionClick}
          onSessionDelete={handleSessionDelete}
          onViewAllHistory={handleViewAllHistory}
        />
      </div>

      {/* 底部：用户信息卡片 */}
      <div className="p-4 pt-3 border-t border-gray-200/60 bg-gradient-to-t from-white/60 via-white/30 to-transparent backdrop-blur-sm">
        <UserCard />
      </div>
    </div>
  );
});

Sidebar.displayName = "Sidebar";

export default Sidebar;
