import request from '@/utils/request';

/**
 * 会话信息
 */
export interface Session {
  id: string | number;
  title: string;
  createTime: string;
  updateTime: string;
  messageCount?: number;
  [key: string]: any;
}

/**
 * 消息信息
 */
export interface Message {
  id: string | number;
  sessionId: string | number;
  role: 'user' | 'assistant';
  content: string;
  createTime: string;
  [key: string]: any;
}

/**
 * 分页参数
 */
export interface PaginationParams {
  page?: number;
  pageSize?: number;
  [key: string]: any;
}

/**
 * 分页响应数据
 */
export interface PaginationResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

/**
 * 获取会话列表
 * @param params - 分页参数
 * @returns Promise<PaginationResponse<Session>>
 */
export const getSessions = (
  params?: PaginationParams
): Promise<PaginationResponse<Session>> => {
  return request.get('/api/chat/sessions', { params });
};

/**
 * 获取会话消息列表
 * @param sessionId - 会话ID
 * @returns Promise<Message[]>
 */
export const getSessionMessages = (sessionId: string | number): Promise<Message[]> => {
  return request.get(`/api/chat/sessions/${sessionId}/messages`);
};

/**
 * 删除会话
 * @param sessionId - 会话ID
 * @returns Promise<void>
 */
export const deleteSession = (sessionId: string | number): Promise<void> => {
  return request.delete(`/api/chat/sessions/${sessionId}`);
};

/**
 * 创建新会话
 * @param title - 会话标题
 * @returns Promise<Session>
 */
export const createSession = (title?: string): Promise<Session> => {
  return request.post('/api/chat/sessions', title ? { title } : {});
};
