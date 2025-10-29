import request from '@/utils/request';
import { UserInfo } from '@/store/user';

/**
 * 登录请求参数
 */
export interface LoginParams {
  username: string;
  password: string;
}

/**
 * 登录响应数据
 */
export interface LoginResponse {
  token: string;
  userInfo: UserInfo;
}

/**
 * 注册请求参数
 */
export interface RegisterParams {
  username: string;
  password: string;
  email?: string;
  nickname?: string;
}

/**
 * 注册响应数据
 */
export interface RegisterResponse {
  token: string;
  userInfo: UserInfo;
}

/**
 * 用户登录
 * @param data - 登录表单数据
 * @returns Promise<LoginResponse>
 */
export const login = (data: LoginParams): Promise<LoginResponse> => {
  return request.post('/api/user/login', data);
};

/**
 * 用户注册
 * @param data - 注册表单数据
 * @returns Promise<RegisterResponse>
 */
export const register = (data: RegisterParams): Promise<RegisterResponse> => {
  return request.post('/api/user/register', data);
};

/**
 * 获取当前登录用户信息
 * @returns Promise<UserInfo>
 */
export const getUserInfo = (): Promise<UserInfo> => {
  return request.get('/api/user/info');
};

/**
 * 退出登录
 * @returns Promise<void>
 */
export const logout = (): Promise<void> => {
  return request.post('/api/user/logout');
};
