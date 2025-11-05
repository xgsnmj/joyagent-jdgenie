/**
 * 智能体社区状态管理
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import { create } from 'zustand';
import { agentCommunityAPI } from '@/api/agentCommunity';
import type { AgentProvider } from '@/types/agentProvider';

interface AgentCommunityState {
  // ========== 状态 ==========
  /** 所有公开智能体 */
  publicAgents: AgentProvider[];
  /** 我创建的智能体 */
  myAgents: AgentProvider[];
  /** 分类列表 */
  categories: string[];
  /** 加载状态 */
  loading: boolean;
  /** 错误信息 */
  error: string | null;

  // ========== 操作 ==========
  /** 获取公开智能体 */
  fetchPublicAgents: (category?: string) => Promise<void>;
  /** 获取我的智能体 */
  fetchMyAgents: () => Promise<void>;
  /** 获取分类列表 */
  fetchCategories: () => Promise<void>;
  /** 搜索智能体 */
  searchAgents: (keyword: string) => Promise<void>;
  /** 记录智能体使用 */
  recordUsage: (id: number) => Promise<void>;
  /** 重置状态 */
  reset: () => void;
}

/**
 * 智能体社区状态管理
 */
export const useAgentCommunityStore = create<AgentCommunityState>((set) => ({
  // ========== 初始状态 ==========
  publicAgents: [],
  myAgents: [],
  categories: [],
  loading: false,
  error: null,

  // ========== 获取公开智能体 ==========
  fetchPublicAgents: async (category?: string) => {
    set({ loading: true, error: null });
    try {
      const agents = await agentCommunityAPI.getPublicAgents(category);
      set({ publicAgents: agents, loading: false });
    } catch (error: any) {
      console.error('获取公开智能体失败:', error);
      set({ error: error.message, loading: false });
    }
  },

  // ========== 获取我的智能体 ==========
  fetchMyAgents: async () => {
    set({ loading: true, error: null });
    try {
      const agents = await agentCommunityAPI.getMyAgents();
      set({ myAgents: agents, loading: false });
    } catch (error: any) {
      console.error('获取我的智能体失败:', error);
      set({ error: error.message, loading: false });
    }
  },

  // ========== 获取分类 ==========
  fetchCategories: async () => {
    try {
      const categories = await agentCommunityAPI.getCategories();
      set({ categories });
    } catch (error: any) {
      console.error('获取分类失败:', error);
    }
  },

  // ========== 搜索智能体 ==========
  searchAgents: async (keyword: string) => {
    set({ loading: true, error: null });
    try {
      const agents = await agentCommunityAPI.searchAgents(keyword);
      set({ publicAgents: agents, loading: false });
    } catch (error: any) {
      console.error('搜索智能体失败:', error);
      set({ error: error.message, loading: false });
    }
  },

  // ========== 记录使用 ==========
  recordUsage: async (id: number) => {
    try {
      await agentCommunityAPI.recordUsage(id);
    } catch (error: any) {
      console.error('记录使用失败:', error);
    }
  },

  // ========== 重置状态 ==========
  reset: () => {
    set({
      publicAgents: [],
      myAgents: [],
      categories: [],
      loading: false,
      error: null,
    });
  },
}));
