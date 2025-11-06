/**
 * 智能体卡片组件
 * 展示智能体的图标、名称、简介、分类、使用次数
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import React from 'react';
import { Card, Tag, Avatar, Badge } from 'antd';
import { RobotOutlined, FireOutlined } from '@ant-design/icons';
import type { AgentProvider } from '@/types/agentProvider';

interface AgentCardProps {
  /** 智能体数据 */
  agent: AgentProvider;
  /** 点击回调 */
  onClick: () => void;
}

/**
 * 智能体卡片
 */
export const AgentCard: React.FC<AgentCardProps> = ({ agent, onClick }) => {
  // 平台类型标签颜色映射
  const platformColors: Record<string, string> = {
    default: 'blue',
    coze: 'purple',
    ronghui: 'orange',
  };

  // 平台类型显示名称
  const platformLabels: Record<string, string> = {
    default: '本地智能体',
    coze: 'Coze平台',
    ronghui: '融汇平台',
  };

  return (
    <Card
      hoverable
      onClick={onClick}
      className="h-full cursor-pointer transition-all duration-300 hover:shadow-xl hover:-translate-y-1"
      bodyStyle={{ padding: '20px' }}
    >
      {/* 顶部：平台标签 + 使用次数 */}
      <div className="flex items-center justify-between mb-3">
        <Tag color={platformColors[agent.providerType] || 'default'}>
          {platformLabels[agent.providerType] || '未知平台'}
        </Tag>
        <Badge
          count={agent.usageCount || 0}
          showZero
          overflowCount={999}
          style={{ backgroundColor: '#52c41a' }}
        />
      </div>

      {/* 中部：图标 + 名称 */}
      <div className="flex items-center mb-3">
        <Avatar
          size={56}
          src={agent.icon}
          icon={!agent.icon && <RobotOutlined />}
          className="mr-3"
        />
        <div className="flex-1">
          <div className="text-lg font-semibold text-gray-800 truncate">
            {agent.providerName}
          </div>
          {agent.category && (
            <Tag color="cyan" className="mt-1">
              {agent.category}
            </Tag>
          )}
        </div>
      </div>

      {/* 底部：简介 */}
      <div className="text-sm text-gray-600 line-clamp-3 min-h-[60px]">
        {agent.description || '暂无简介'}
      </div>

      {/* 热门标识（使用次数>100） */}
      {agent.usageCount && agent.usageCount > 100 && (
        <div className="mt-3 flex items-center text-red-500 text-xs">
          <FireOutlined className="mr-1" />
          <span>热门智能体</span>
        </div>
      )}
    </Card>
  );
};

export default AgentCard;
