import { memo, useEffect, useState } from 'react';
import { Avatar, Dropdown, message } from 'antd';
import { UserOutlined, HistoryOutlined, LogoutOutlined, SettingOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useUserStore } from '@/store/user';
import { getUserInfo } from '@/api/user';
import type { MenuProps } from 'antd';

/**
 * 用户信息卡片组件
 * 显示在侧边栏底部，包含用户头像、昵称和下拉菜单
 */
const UserCard: GenieType.FC = memo(() => {
  const navigate = useNavigate();
  const { userInfo: storeUserInfo, setUserInfo, logout } = useUserStore();
  const [loading, setLoading] = useState(false);

  /**
   * 组件挂载时获取用户信息
   */
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!storeUserInfo) {
        try {
          setLoading(true);
          const userInfo = await getUserInfo();
          setUserInfo(userInfo);
        } catch (error: any) {
          console.error('获取用户信息失败:', error);
        } finally {
          setLoading(false);
        }
      }
    };

    fetchUserInfo();
  }, [storeUserInfo, setUserInfo]);

  /**
   * 处理退出登录
   */
  const handleLogout = () => {
    logout();
    message.success('已退出登录');
    navigate('/login');
  };

  /**
   * 用户下拉菜单配置
   */
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '个人设置',
      onClick: () => navigate('/settings'),
    },
    {
      key: 'history',
      icon: <HistoryOutlined />,
      label: '会话历史',
      onClick: () => navigate('/chat/history'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      danger: true,
      onClick: handleLogout,
    },
  ];

  // 显示加载状态
  if (loading || !storeUserInfo) {
    return (
      <div className="relative">
        <div className="bg-gradient-to-br from-white to-gray-50 rounded-2xl p-4 shadow-lg border border-gray-100/50">
          <div className="flex items-center gap-3">
            <div className="relative">
              <Avatar
                size={48}
                icon={<UserOutlined />}
                className="bg-gradient-to-br from-[#4040ff] to-[#764ba2] shadow-md"
              />
              <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-gray-300 rounded-full border-2 border-white"></div>
            </div>
            <div className="flex-1 min-w-0">
              <div className="h-4 bg-gray-200 rounded-full w-24 mb-2 animate-pulse"></div>
              <div className="h-3 bg-gray-100 rounded-full w-32 animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const displayName = storeUserInfo.nickname || storeUserInfo.username || '用户';
  const displayStatus = storeUserInfo.email || '在线';

  return (
    <Dropdown
      menu={{ items: userMenuItems }}
      placement="topRight"
      trigger={['click']}
      arrow
    >
      <div className="relative cursor-pointer group">
        {/* 主卡片 */}
        <div className="bg-gradient-to-br from-white to-gray-50 rounded-2xl p-4 shadow-lg border border-gray-100/50 hover:shadow-xl hover:scale-[1.02] transition-all duration-300">
          <div className="flex items-center gap-3">
            {/* 头像 */}
            <div className="relative flex-shrink-0">
              <Avatar
                size={48}
                icon={<UserOutlined />}
                src={storeUserInfo.avatar}
                className="bg-gradient-to-br from-[#4040ff] to-[#764ba2] shadow-md ring-2 ring-white"
              />
              {/* 在线状态指示器 */}
              <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-green-500 rounded-full border-2 border-white shadow-sm animate-pulse"></div>
            </div>

            {/* 用户信息 */}
            <div className="flex-1 min-w-0">
              <div className="text-sm font-bold text-gray-900 truncate mb-0.5">
                {displayName}
              </div>
              <div className="text-xs text-gray-500 truncate flex items-center gap-1.5">
                <span className="inline-block w-1.5 h-1.5 bg-green-500 rounded-full"></span>
                <span className="truncate">{displayStatus}</span>
              </div>
            </div>

            {/* 下拉指示器 */}
            <div className="flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </Dropdown>
  );
});

UserCard.displayName = 'UserCard';

export default UserCard;
