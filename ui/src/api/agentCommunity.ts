/**
 * 智能体社区API接口
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import request from '@/utils/request';
import type { AgentProvider } from '@/types/agentProvider';

/**
 * 智能体社区API
 */
export const agentCommunityAPI = {
  /**
   * 获取公开智能体列表
   *
   * @param category 分类标签（可选）
   * @returns 公开智能体列表
   */
  getPublicAgents: (category?: string) => {
    const params = category ? { category } : {};
    return request.get<AgentProvider[]>('/api/agent-community/public', { params });
  },

  /**
   * 获取我创建的智能体
   *
   * @returns 我创建的智能体列表
   */
  getMyAgents: () => {
    return request.get<AgentProvider[]>('/api/agent-community/my-agents');
  },

  /**
   * 搜索智能体
   *
   * @param keyword 搜索关键词
   * @returns 匹配的智能体列表
   */
  searchAgents: (keyword: string) => {
    return request.get<AgentProvider[]>('/api/agent-community/search', {
      params: { keyword },
    });
  },

  /**
   * 记录智能体使用
   *
   * @param id 智能体ID
   */
  recordUsage: (id: number) => {
    return request.post(`/api/agent-community/use/${id}`);
  },

  /**
   * 获取分类列表
   *
   * @returns 分类列表
   */
  getCategories: () => {
    return request.get<string[]>('/api/agent-community/categories');
  },
};
