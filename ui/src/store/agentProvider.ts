/**
 * 智能体服务商状态管理
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import { create } from 'zustand';
import { agentProviderAPI } from '@/api/agentProvider';
import type { AgentProvider } from '@/types/agentProvider';

interface AgentProviderState {
  // ========== 状态 ==========
  /** 所有智能体配置列表 */
  providers: AgentProvider[];
  /** 默认智能体配置 */
  defaultProvider: AgentProvider | null;
  /** 当前选中的智能体配置 */
  currentProvider: AgentProvider | null;
  /** 加载状态 */
  loading: boolean;
  /** 错误信息 */
  error: string | null;

  // ========== 操作 ==========
  /** 获取所有智能体配置 */
  fetchProviders: () => Promise<void>;
  /** 设置当前使用的智能体 */
  setCurrentProvider: (provider: AgentProvider) => void;
  /** 设置默认智能体 */
  setDefaultProvider: (id: number) => Promise<void>;
  /** 添加智能体 */
  addProvider: (provider: AgentProvider) => void;
  /** 更新智能体 */
  updateProvider: (id: number, provider: AgentProvider) => void;
  /** 删除智能体 */
  removeProvider: (id: number) => void;
  /** 重置状态 */
  reset: () => void;
}

/**
 * 智能体服务商状态管理
 */
export const useAgentProviderStore = create<AgentProviderState>((set, get) => ({
  // ========== 初始状态 ==========
  providers: [],
  defaultProvider: null,
  currentProvider: null,
  loading: false,
  error: null,

  // ========== 获取所有智能体配置 ==========
  fetchProviders: async () => {
    set({ loading: true, error: null });
    try {
      // request拦截器已经返回了data.data，所以这里直接使用response
      const providers = (await agentProviderAPI.getAll()) || [];
      const defaultProvider = providers.find((p) => p.isDefault) || null;

      set({
        providers,
        defaultProvider,
        // 如果当前没有选中的智能体，使用默认智能体
        currentProvider: get().currentProvider || defaultProvider,
        loading: false,
      });
    } catch (error: any) {
      console.error('获取智能体配置失败:', error);
      set({
        error: error.message || '获取智能体配置失败',
        loading: false,
      });
    }
  },

  // ========== 设置当前使用的智能体 ==========
  setCurrentProvider: (provider: AgentProvider) => {
    console.log('设置当前智能体:', provider);
    set({ currentProvider: provider });
  },

  // ========== 设置默认智能体 ==========
  setDefaultProvider: async (id: number) => {
    try {
      await agentProviderAPI.setDefault(id);

      // 更新本地状态
      const providers = get().providers.map((p) => ({
        ...p,
        isDefault: p.id === id,
      }));

      const defaultProvider = providers.find((p) => p.id === id) || null;

      set({ providers, defaultProvider });
      console.log('设置默认智能体成功:', id);
    } catch (error: any) {
      console.error('设置默认智能体失败:', error);
      set({ error: error.message || '设置默认智能体失败' });
      throw error;
    }
  },

  // ========== 添加智能体 ==========
  addProvider: (provider: AgentProvider) => {
    set((state) => ({
      providers: [...state.providers, provider],
    }));
    console.log('添加智能体成功:', provider);
  },

  // ========== 更新智能体 ==========
  updateProvider: (id: number, provider: AgentProvider) => {
    set((state) => ({
      providers: state.providers.map((p) => (p.id === id ? provider : p)),
    }));
    console.log('更新智能体成功:', id);
  },

  // ========== 删除智能体 ==========
  removeProvider: (id: number) => {
    set((state) => ({
      providers: state.providers.filter((p) => p.id !== id),
    }));
    console.log('删除智能体成功:', id);
  },

  // ========== 重置状态 ==========
  reset: () => {
    set({
      providers: [],
      defaultProvider: null,
      currentProvider: null,
      loading: false,
      error: null,
    });
    console.log('重置智能体状态');
  },
}));
