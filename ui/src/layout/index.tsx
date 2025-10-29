import { memo, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { ConfigProvider, message, Layout as AntLayout } from 'antd';
import { ConstantProvider } from '@/hooks';
import * as constants from "@/utils/constants";
import { setMessage } from '@/utils';
import { Sidebar } from '@/components';

const { Content, Sider } = AntLayout;

// Layout 组件：应用的主要布局结构
const Layout: GenieType.FC = memo(() => {
  const location = useLocation();
  const [messageApi, messageContent] = message.useMessage();

  useEffect(() => {
    // 初始化全局 message
    setMessage(messageApi);
  }, [messageApi]);

  /**
   * 判断是否显示侧边栏
   * 登录页不显示侧边栏
   */
  const showSidebar = !location.pathname.startsWith('/login');

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#4040FFB2' } }}>
      {messageContent}
      <ConstantProvider value={constants}>
        <AntLayout className="min-h-screen bg-gray-50">
          {/* 左侧边栏 */}
          {showSidebar && (
            <Sider
              width={260}
              className="overflow-hidden shadow-xl"
              style={{
                position: 'fixed',
                left: 0,
                top: 0,
                bottom: 0,
                zIndex: 100,
                background: 'transparent',
                boxShadow: '4px 0 24px rgba(0, 0, 0, 0.06)',
              }}
            >
              <Sidebar className="h-full" />
            </Sider>
          )}

          {/* 内容区域 */}
          <AntLayout style={{ marginLeft: showSidebar ? 260 : 0, background: 'transparent' }}>
            <Content className="h-screen">
              <Outlet />
            </Content>
          </AntLayout>
        </AntLayout>
      </ConstantProvider>
    </ConfigProvider>
  );
});

Layout.displayName = 'Layout';

export default Layout;
