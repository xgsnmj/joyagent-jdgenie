/**
 * 智能体配置列表组件
 * 展示用户的所有智能体配置
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import React from 'react';
import { List, Button, Space, Tag, Popconfirm, message, Tooltip } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  StarOutlined,
  StarFilled,
  RobotOutlined,
} from '@ant-design/icons';
import type { AgentProvider } from '@/types/agentProvider';
import { PROVIDER_TYPE_LABELS } from '@/types/agentProvider';

interface AgentListProps {
  /** 智能体配置列表 */
  providers: AgentProvider[];
  /** 编辑回调 */
  onEdit: (provider: AgentProvider) => void;
  /** 删除回调 */
  onDelete: (id: number) => Promise<void>;
  /** 设置默认回调 */
  onSetDefault: (id: number) => Promise<void>;
}

/**
 * 智能体配置列表
 */
export const AgentList: React.FC<AgentListProps> = ({
  providers,
  onEdit,
  onDelete,
  onSetDefault,
}) => {
  const handleDelete = async (id: number) => {
    try {
      await onDelete(id);
      message.success('删除成功');
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  const handleSetDefault = async (id: number) => {
    try {
      await onSetDefault(id);
      message.success('设置成功');
    } catch (error: any) {
      message.error(error.message || '设置失败');
    }
  };

  return (
    <List
      dataSource={providers}
      locale={{ emptyText: '暂无智能体配置' }}
      renderItem={(provider) => {
        const isDefault = provider.providerType === 'default';

        return (
          <List.Item
            actions={[
              // 设为默认按钮
              <Tooltip title={provider.isDefault ? '已是默认' : '设为默认'} key="default">
                <Button
                  type="text"
                  icon={provider.isDefault ? <StarFilled /> : <StarOutlined />}
                  onClick={() => handleSetDefault(provider.id)}
                  disabled={provider.isDefault}
                  className={provider.isDefault ? 'text-yellow-500' : ''}
                >
                  {provider.isDefault ? '默认' : '设为默认'}
                </Button>
              </Tooltip>,

              // 编辑按钮（default类型不允许编辑）
              !isDefault && (
                <Tooltip title="编辑" key="edit">
                  <Button
                    type="text"
                    icon={<EditOutlined />}
                    onClick={() => onEdit(provider)}
                  >
                    编辑
                  </Button>
                </Tooltip>
              ),

              // 删除按钮（default类型不允许删除）
              !isDefault && (
                <Popconfirm
                  title="确定要删除这个智能体配置吗？"
                  description="删除后无法恢复，请谨慎操作"
                  onConfirm={() => handleDelete(provider.id)}
                  okText="确定"
                  cancelText="取消"
                  key="delete"
                >
                  <Tooltip title="删除">
                    <Button
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                    >
                      删除
                    </Button>
                  </Tooltip>
                </Popconfirm>
              ),
            ].filter(Boolean)}
          >
            <List.Item.Meta
              avatar={
                <div className="flex items-center justify-center w-10 h-10 rounded-full bg-blue-100">
                  <RobotOutlined className="text-blue-600 text-xl" />
                </div>
              }
              title={
                <Space>
                  <span className="font-medium">{provider.providerName}</span>
                  {provider.isDefault && <Tag color="blue">默认</Tag>}
                  {isDefault && <Tag color="green">系统</Tag>}
                  {provider.status === 0 && <Tag color="red">已禁用</Tag>}
                </Space>
              }
              description={
                <div className="space-y-1">
                  <div className="text-sm text-gray-600">
                    平台: {PROVIDER_TYPE_LABELS[provider.providerType]}
                  </div>
                  {provider.apiEndpoint && (
                    <div className="text-xs text-gray-400 truncate max-w-md">
                      端点: {provider.apiEndpoint}
                    </div>
                  )}
                  <div className="text-xs text-gray-400">
                    创建时间: {new Date(provider.createTime).toLocaleString('zh-CN')}
                  </div>
                </div>
              }
            />
          </List.Item>
        );
      }}
    />
  );
};

export default AgentList;
