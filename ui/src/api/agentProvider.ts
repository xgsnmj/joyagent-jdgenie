/**
 * 智能体服务商API接口
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import request from '@/utils/request';
import type { AgentProvider, AgentProviderRequest } from '@/types/agentProvider';

/**
 * 智能体服务商API
 */
export const agentProviderAPI = {
  /**
   * 获取当前用户的所有智能体配置
   */
  getAll: () => {
    return request.get<AgentProvider[]>('/api/agent-providers');
  },

  /**
   * 创建智能体配置
   */
  create: (data: AgentProviderRequest) => {
    return request.post<AgentProvider>('/api/agent-providers', data);
  },

  /**
   * 更新智能体配置
   */
  update: (id: number, data: AgentProviderRequest) => {
    return request.put<AgentProvider>(`/api/agent-providers/${id}`, data);
  },

  /**
   * 删除智能体配置
   */
  delete: (id: number) => {
    return request.delete(`/api/agent-providers/${id}`);
  },

  /**
   * 设置默认智能体
   */
  setDefault: (id: number) => {
    return request.put(`/api/agent-providers/${id}/set-default`);
  },

  /**
   * 测试连接
   */
  testConnection: (data: AgentProviderRequest) => {
    return request.post<string>('/api/agent-providers/test-connection', data);
  }
};
