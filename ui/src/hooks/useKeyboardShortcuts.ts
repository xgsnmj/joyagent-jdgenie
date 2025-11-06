import { useEffect, useCallback } from 'react';

/**
 * 键盘快捷键处理的自定义Hook
 * 负责管理组件级别的键盘快捷键
 */
export const useKeyboardShortcuts = (shortcuts: Record<string, () => void>) => {
  /**
   * 处理键盘事件
   */
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    // 遍历所有注册的快捷键
    for (const [key, handler] of Object.entries(shortcuts)) {
      if (isKeyMatch(e, key)) {
        e.preventDefault();
        handler();
        break;
      }
    }
  }, [shortcuts]);

  /**
   * 检查按键是否匹配快捷键
   * 支持格式：'Ctrl+K', 'Meta+K', 'Escape'
   */
  const isKeyMatch = (e: KeyboardEvent, shortcut: string): boolean => {
    const keys = shortcut.toLowerCase().split('+');
    const hasCtrl = keys.includes('ctrl') || keys.includes('control');
    const hasMeta = keys.includes('meta') || keys.includes('cmd');
    const hasAlt = keys.includes('alt');
    const hasShift = keys.includes('shift');
    const key = keys.find(k => !['ctrl', 'control', 'meta', 'cmd', 'alt', 'shift'].includes(k));

    // 检查修饰键
    if (hasCtrl && !(e.ctrlKey || e.metaKey)) return false;
    if (hasMeta && !e.metaKey) return false;
    if (hasAlt && !e.altKey) return false;
    if (hasShift && !e.shiftKey) return false;

    // 检查主键
    if (key && e.key.toLowerCase() !== key) return false;

    return true;
  };

  /**
   * 注册和清理键盘事件监听器
   */
  useEffect(() => {
    if (Object.keys(shortcuts).length === 0) {
      return;
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown, shortcuts]);
};

/**
 * 专门用于Sidebar组件的快捷键Hook
 */
export const useSidebarShortcuts = (onNewSession: () => void) => {
  const shortcuts = {
    'Ctrl+K': onNewSession,
    'Meta+K': onNewSession, // Mac的Cmd键
  };

  useKeyboardShortcuts(shortcuts);
};