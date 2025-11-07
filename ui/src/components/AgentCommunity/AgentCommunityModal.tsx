/**
 * 智能体社区弹窗
 * 展示所有公开智能体的卡片式画廊
 *
 * 功能:
 * 1. 搜索智能体
 * 2. 分类筛选
 * 3. 卡片展示
 * 4. 点击卡片跳转聊天并自动选择智能体
 * 5. "管理我的智能体"按钮
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import React, { useState, useEffect } from 'react';
import { Modal, Input, Empty, Spin, Badge, Tag, Button } from 'antd';
import { SearchOutlined, SettingOutlined, FireOutlined, CheckOutlined } from '@ant-design/icons';
import { AgentCard } from './AgentCard';
import { useAgentCommunityStore } from '@/store/agentCommunity';
import { useNavigate } from 'react-router-dom';
import { useAgentProviderStore } from '@/store/agentProvider';
import type { AgentProvider } from '@/types/agentProvider';

interface AgentCommunityModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 打开"管理我的智能体"回调 */
  onManageClick: () => void;
}

/**
 * 智能体社区弹窗
 */
export const AgentCommunityModal: React.FC<AgentCommunityModalProps> = ({
  visible,
  onClose,
  onManageClick,
}) => {
  const navigate = useNavigate();
  const {
    publicAgents,
    categories,
    loading,
    fetchPublicAgents,
    fetchCategories,
    searchAgents,
    recordUsage,
  } = useAgentCommunityStore();

  const { setCurrentProvider } = useAgentProviderStore();

  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('全部');

  // 弹窗打开时加载数据
  useEffect(() => {
    if (visible) {
      fetchPublicAgents();
      fetchCategories();
      setSearchKeyword('');
      setSelectedCategory('全部');
    }
  }, [visible, fetchPublicAgents, fetchCategories]);

  // 搜索处理
  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    if (value.trim()) {
      searchAgents(value);
    } else {
      fetchPublicAgents(selectedCategory === '全部' ? undefined : selectedCategory);
    }
  };

  // 分类切换
  const handleCategoryChange = (category: string) => {
    setSelectedCategory(category);
    setSearchKeyword('');
    fetchPublicAgents(category === '全部' ? undefined : category);
  };

  // 点击智能体卡片
  const handleAgentClick = async (agent: AgentProvider) => {
    // 1. 记录使用次数
    await recordUsage(agent.id);

    // 2. 设置为当前智能体
    setCurrentProvider(agent);

    // 3. 关闭弹窗
    onClose();

    // 4. 跳转到聊天页面（如果不在聊天页，则跳转；如果已在聊天页，创建新会话）
    const currentPath = window.location.pathname;
    if (currentPath.includes('/chat/')) {
      // 已在聊天页，跳转到首页（会创建新会话）
      navigate('/');
    } else {
      // 不在聊天页，跳转到首页
      navigate('/');
    }
  };

  return (
    <Modal
      title={
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">智能体社区</span>
          <Tag color="blue" icon={<FireOutlined />}>
            {publicAgents.length} 个智能体
          </Tag>
        </div>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1200}
      bodyStyle={{ padding: '24px', minHeight: '600px', maxHeight: '80vh', overflow: 'auto' }}
      destroyOnClose
    >
      {/* 搜索栏 */}
      <div className="mb-4">
        <Input
          size="large"
          placeholder="搜索智能体名称或简介..."
          prefix={<SearchOutlined />}
          value={searchKeyword}
          onChange={(e) => handleSearch(e.target.value)}
          allowClear
        />
      </div>

      {/* 分类标签云 */}
      <div className="mb-6">
        <div className="text-sm text-gray-500 mb-3 flex items-center">
          <span className="mr-2">🏷️</span>
          <span>选择分类：</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {categories.map((cat) => (
            <Tag
              key={cat}
              color={selectedCategory === cat ? 'blue' : undefined}
              className={`
                cursor-pointer transition-all duration-200 px-4 py-2 text-sm
                ${selectedCategory === cat
                  ? 'font-bold shadow-md scale-105'
                  : 'hover:scale-105 hover:shadow-sm'
                }
              `}
              style={{
                borderRadius: '20px',
                border: selectedCategory === cat ? '2px solid #1890ff' : '1px solid #d9d9d9',
                fontSize: '14px',
                lineHeight: '24px',
              }}
              onClick={() => handleCategoryChange(cat)}
              icon={selectedCategory === cat ? <CheckOutlined /> : undefined}
            >
              {cat}
            </Tag>
          ))}
        </div>
      </div>

      {/* 智能体卡片网格 */}
      <Spin spinning={loading}>
        {publicAgents.length === 0 ? (
          <Empty description="暂无智能体" />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {publicAgents.map((agent) => (
              <AgentCard
                key={agent.id}
                agent={agent}
                onClick={() => handleAgentClick(agent)}
              />
            ))}
          </div>
        )}
      </Spin>

      {/* 底部操作栏 */}
      <div className="mt-6 pt-4 border-t border-gray-200 flex justify-end">
        <Button
          type="primary"
          size="large"
          icon={<SettingOutlined />}
          onClick={onManageClick}
        >
          管理我的智能体
        </Button>
      </div>
    </Modal>
  );
};

export default AgentCommunityModal;
