import { memo, ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUserStore } from '@/store/user';

interface PrivateRouteProps {
  children: ReactNode;
}

/**
 * 路由守卫组件
 * 用于保护需要登录才能访问的页面
 *
 * 使用方式：
 * <PrivateRoute>
 *   <YourComponent />
 * </PrivateRoute>
 */
const PrivateRoute: GenieType.FC<PrivateRouteProps> = memo(({ children }) => {
  const location = useLocation();
  const { token } = useUserStore();

  /**
   * 如果没有token，重定向到登录页
   * 并保存当前路径，登录后可以跳转回来
   */
  if (!token) {
    return (
      <Navigate
        to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`}
        replace
      />
    );
  }

  /**
   * 有token，正常渲染子组件
   */
  return <>{children}</>;
});

PrivateRoute.displayName = 'PrivateRoute';

export default PrivateRoute;
