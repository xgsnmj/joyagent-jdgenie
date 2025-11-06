/**
 * 我的智能体列表
 * 展示当前用户创建的所有智能体（包括公开和私有）
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import React from 'react';
import { List, Button, Switch, Popconfirm, Tag, Avatar, Empty } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  PlusOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import type { AgentProvider } from '@/types/agentProvider';

interface MyAgentsListProps {
  /** 智能体列表 */
  agents: AgentProvider[];
  /** 编辑回调 */
  onEdit: (agent: AgentProvider) => void;
  /** 删除回调 */
  onDelete: (id: number) => void;
  /** 公开/私有切换回调 */
  onTogglePublic: (id: number, isPublic: boolean) => void;
  /** 添加新智能体回调 */
  onAddNew: () => void;
}

/**
 * 我的智能体列表
 */
export const MyAgentsList: React.FC<MyAgentsListProps> = ({
  agents,
  onEdit,
  onDelete,
  onTogglePublic,
  onAddNew,
}) => {
  return (
    <div>
      {/* 顶部按钮 */}
      <div className="mb-4">
        <Button type="primary" icon={<PlusOutlined />} onClick={onAddNew}>
          添加新智能体
        </Button>
      </div>

      {/* 列表 */}
      {agents.length === 0 ? (
        <Empty
          description="您还没有创建智能体"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          <Button type="primary" onClick={onAddNew}>
            立即创建
          </Button>
        </Empty>
      ) : (
        <List
          itemLayout="horizontal"
          dataSource={agents}
          renderItem={(agent) => (
            <List.Item
              actions={[
                <Switch
                  key="public"
                  checked={agent.isPublic}
                  onChange={(checked) => onTogglePublic(agent.id, checked)}
                  checkedChildren={<EyeOutlined />}
                  unCheckedChildren={<EyeInvisibleOutlined />}
                />,
                <Button
                  key="edit"
                  type="link"
                  icon={<EditOutlined />}
                  onClick={() => onEdit(agent)}
                >
                  编辑
                </Button>,
                <Popconfirm
                  key="delete"
                  title="确定要删除这个智能体吗？"
                  onConfirm={() => onDelete(agent.id)}
                  okText="确定"
                  cancelText="取消"
                  disabled={agent.providerType === 'default'}
                >
                  <Button
                    type="link"
                    danger
                    icon={<DeleteOutlined />}
                    disabled={agent.providerType === 'default'}
                  >
                    删除
                  </Button>
                </Popconfirm>,
              ]}
            >
              <List.Item.Meta
                avatar={
                  <Avatar
                    size={48}
                    src={agent.icon}
                    icon={!agent.icon && <RobotOutlined />}
                  />
                }
                title={
                  <div className="flex items-center gap-2">
                    <span>{agent.providerName}</span>
                    {agent.isDefault && <Tag color="blue">默认</Tag>}
                    {agent.isPublic ? (
                      <Tag color="green">公开</Tag>
                    ) : (
                      <Tag color="gray">私有</Tag>
                    )}
                    {agent.category && <Tag color="cyan">{agent.category}</Tag>}
                  </div>
                }
                description={
                  <div>
                    <div className="text-gray-600 mb-1">
                      {agent.description || '暂无简介'}
                    </div>
                    <div className="text-xs text-gray-400">
                      使用次数: {agent.usageCount || 0} · 平台:{' '}
                      {agent.providerType}
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  );
};

export default MyAgentsList;
