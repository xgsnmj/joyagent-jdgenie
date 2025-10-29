import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 用户信息类型定义
 */
export interface UserInfo {
  id: string | number;
  username: string;
  email?: string;
  avatar?: string;
  nickname?: string;
  [key: string]: any;
}

/**
 * 用户状态类型定义
 */
interface UserState {
  token: string | null;
  userInfo: UserInfo | null;
  setToken: (token: string | null) => void;
  setUserInfo: (userInfo: UserInfo | null) => void;
  logout: () => void;
}

/**
 * 用户状态管理Store
 * 使用zustand管理用户认证状态
 * 使用persist中间件持久化存储到localStorage
 */
export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      // 用户Token
      token: null,

      // 用户信息
      userInfo: null,

      /**
       * 设置用户Token
       * @param token - JWT Token或其他认证token
       */
      setToken: (token) => set({ token }),

      /**
       * 设置用户信息
       * @param userInfo - 用户详细信息
       */
      setUserInfo: (userInfo) => set({ userInfo }),

      /**
       * 退出登录
       * 清空token和用户信息
       */
      logout: () => {
        set({ token: null, userInfo: null });
        // 清除localStorage中的持久化数据
        localStorage.removeItem('user-storage');
      },
    }),
    {
      // 持久化配置
      name: 'user-storage', // localStorage中的key名称
      // 可选：自定义存储引擎，默认使用localStorage
      // storage: createJSONStorage(() => sessionStorage),
    }
  )
);
