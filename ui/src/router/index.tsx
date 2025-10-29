import React, { Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import Layout from '@/layout/index';
import { Loading } from '@/components';
import PrivateRoute from '@/components/PrivateRoute';

// 使用常量存储路由路径
const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  CHAT_HISTORY: '/chat/history',
  NOT_FOUND: '*',
};

// 使用 React.lazy 懒加载组件
const Home = React.lazy(() => import('@/pages/Home'));
const Login = React.lazy(() => import('@/pages/Login'));
const ChatHistory = React.lazy(() => import('@/pages/ChatHistory'));
const NotFound = React.lazy(() => import('@/components/NotFound'));

// 创建路由配置
const router = createBrowserRouter([
  {
    path: ROUTES.HOME,
    element: <Layout />,
    children: [
      {
        // 首页 - 需要登录认证
        index: true,
        element: (
          <Suspense fallback={<Loading loading={true} className="h-full"/>}>
            <PrivateRoute>
              <Home />
            </PrivateRoute>
          </Suspense>
        ),
      },
      {
        // 登录页 - 无需认证
        path: ROUTES.LOGIN,
        element: (
          <Suspense fallback={<Loading loading={true} className="h-full"/>}>
            <Login />
          </Suspense>
        ),
      },
      {
        // 会话历史页 - 需要登录认证
        path: ROUTES.CHAT_HISTORY,
        element: (
          <Suspense fallback={<Loading loading={true} className="h-full"/>}>
            <PrivateRoute>
              <ChatHistory />
            </PrivateRoute>
          </Suspense>
        ),
      },
      {
        // 404页面
        path: ROUTES.NOT_FOUND,
        element: (
          <Suspense fallback={<Loading loading={true} className="h-full"/>}>
            <NotFound />
          </Suspense>
        ),
      },
    ],
  },
  // 重定向所有未匹配的路由到 404 页面
  {
    path: '*',
    element: <Navigate to={ROUTES.NOT_FOUND} replace />,
  },
]);

export default router;
