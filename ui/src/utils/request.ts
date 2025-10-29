import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { showMessage } from './utils';

// 创建axios实例
const request: AxiosInstance = axios.create({
  baseURL: SERVICE_BASE_URL,
  timeout: 10000,
  headers: {'Content-Type': 'application/json',},
});

/**
 * 获取存储的Token
 * 从localStorage中获取持久化的用户token
 */
const getToken = (): string | null => {
  try {
    const userStorage = localStorage.getItem('user-storage');
    if (userStorage) {
      const { state } = JSON.parse(userStorage);
      return state?.token || null;
    }
  } catch (error) {
    console.error('获取token失败:', error);
  }
  return null;
};

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 自动添加Authorization头
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

/**
 * 处理未认证情况
 * 清除本地存储并跳转到登录页
 */
const noAuth = (url?: string) => {
  showMessage()?.error('未登录，请先登录');
  // 清除用户信息
  localStorage.removeItem('user-storage');
  // 跳转到登录页
  const redirectUrl = url || '/login';
  // 延迟跳转，让用户看到提示信息
  setTimeout(() => {
    window.location.href = redirectUrl;
  }, 500);
};

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {

    const { data, status } = response;

    if (status === 200) {
      // 根据后端约定的数据结构处理
      if (data.code === 200) {
        return data.data;
      } else if (data.code === 401) {
        noAuth(data.redirectUrl);
      } else {
        showMessage()?.error(data.msg || '请求失败');
        return Promise.reject(new Error(data.msg || '请求失败'));
      }
    }

    return response;
  },
  (error) => {
    console.error('响应错误:', error);

    const message = showMessage();
    if (error.response) {
      const { status, data: resData } = error.response;

      switch (status) {
        case 401:
          // 未授权，清除token并跳转登录
          noAuth(resData.redirectUrl);
          break;
        case 403:
          message?.error(error.message || '没有权限访问');
          break;
        case 404:
          message?.error(error.message || '请求的资源不存在');
          break;
        case 500:
          message?.error(error.message || '服务器内部错误');
          break;
        default:
          message?.error(error.message || `请求失败，状态码: ${status}`);
      }
    } else if (error.request) {
      message?.error(error.message || '网络错误，请检查网络连接');
    } else {
      message?.error('请求配置错误');
    }

    return Promise.reject(error);
  }
);

export default request;