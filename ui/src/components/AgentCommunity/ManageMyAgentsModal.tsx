/**
 * 管理我的智能体弹窗
 * 两个标签页：
 * 1. 我的智能体 - 展示和编辑已创建的智能体
 * 2. 添加智能体 - 创建新智能体（复用AgentForm）
 *
 * @author JDGenie Team
 * @since 2025-01-05
 */

import React, { useState, useEffect } from 'react';
import { Modal, Tabs, message } from 'antd';
import { PlusOutlined, AppstoreOutlined } from '@ant-design/icons';
import { MyAgentsList } from './MyAgentsList';
import { AgentForm } from '../AgentSettings/AgentForm';
import { useAgentCommunityStore } from '@/store/agentCommunity';
import { useAgentProviderStore } from '@/store/agentProvider';
import { agentProviderAPI } from '@/api/agentProvider';
import type { AgentProvider, AgentProviderFormData } from '@/types/agentProvider';

interface ManageMyAgentsModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
}

/**
 * 管理我的智能体弹窗
 */
export const ManageMyAgentsModal: React.FC<ManageMyAgentsModalProps> = ({
  visible,
  onClose,
}) => {
  const { myAgents, fetchMyAgents } = useAgentCommunityStore();
  const { addProvider, updateProvider, removeProvider } = useAgentProviderStore();

  const [activeTab, setActiveTab] = useState<string>('list');
  const [editingAgent, setEditingAgent] = useState<AgentProvider | null>(null);

  // 弹窗打开时加载数据
  useEffect(() => {
    if (visible) {
      fetchMyAgents();
      setActiveTab('list');
      setEditingAgent(null);
    }
  }, [visible, fetchMyAgents]);

  // 编辑智能体
  const handleEdit = (agent: AgentProvider) => {
    setEditingAgent(agent);
    setActiveTab('form');
  };

  // 删除智能体
  const handleDelete = async (id: number) => {
    try {
      await agentProviderAPI.delete(id);
      removeProvider(id);
      fetchMyAgents(); // 刷新我的智能体列表
      message.success('删除成功');
    } catch (error: any) {
      message.error('删除失败: ' + error.message);
    }
  };

  // 切换公开/私有
  const handleTogglePublic = async (id: number, isPublic: boolean) => {
    try {
      const agent = myAgents.find((a) => a.id === id);
      if (agent) {
        const updated = await agentProviderAPI.update(id, { ...agent, isPublic });
        updateProvider(id, updated);
        fetchMyAgents();
        message.success(isPublic ? '已设为公开' : '已设为私有');
      }
    } catch (error: any) {
      message.error('操作失败: ' + error.message);
    }
  };

  // 提交表单
  const handleSubmit = async (values: AgentProviderFormData) => {
    try {
      if (editingAgent) {
        // 更新
        const updated = await agentProviderAPI.update(editingAgent.id, values);
        updateProvider(editingAgent.id, updated);
        message.success('更新成功');
      } else {
        // 创建
        const created = await agentProviderAPI.create(values);
        addProvider(created);
        message.success('创建成功');
      }

      // 刷新列表并返回列表页
      fetchMyAgents();
      setActiveTab('list');
      setEditingAgent(null);
    } catch (error: any) {
      message.error('操作失败: ' + error.message);
    }
  };

  // 取消表单
  const handleCancel = () => {
    setActiveTab('list');
    setEditingAgent(null);
  };

  return (
    <Modal
      title="管理我的智能体"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={900}
      bodyStyle={{ padding: '24px', minHeight: '500px' }}
      destroyOnClose
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'list',
            label: (
              <span>
                <AppstoreOutlined />
                我的智能体
              </span>
            ),
            children: (
              <MyAgentsList
                agents={myAgents}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onTogglePublic={handleTogglePublic}
                onAddNew={() => {
                  setEditingAgent(null);
                  setActiveTab('form');
                }}
              />
            ),
          },
          {
            key: 'form',
            label: (
              <span>
                <PlusOutlined />
                {editingAgent ? '编辑智能体' : '添加智能体'}
              </span>
            ),
            children: (
              <AgentForm
                initialValues={editingAgent || undefined}
                onSubmit={handleSubmit}
                onCancel={handleCancel}
                showExtendedFields // 显示icon、description、category等新字段
              />
            ),
          },
        ]}
      />
    </Modal>
  );
};

export default ManageMyAgentsModal;
