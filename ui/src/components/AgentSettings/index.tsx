/**
 * 智能体设置对话框组件
 * 主要功能：
 * 1. 展示智能体配置列表
 * 2. 创建/编辑智能体配置
 * 3. 删除智能体配置
 * 4. 设置默认智能体
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import React, { useState, useEffect } from 'react';
import { Modal, Button, Tabs, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { AgentList } from './AgentList';
import { AgentForm } from './AgentForm';
import { useAgentProviderStore } from '@/store/agentProvider';
import { agentProviderAPI } from '@/api/agentProvider';
import type { AgentProvider, AgentProviderFormData } from '@/types/agentProvider';

interface AgentSettingsProps {
  /** 是否显示对话框 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
}

/**
 * 智能体设置对话框
 */
export const AgentSettings: React.FC<AgentSettingsProps> = ({
  visible,
  onClose,
}) => {
  const {
    providers,
    fetchProviders,
    setDefaultProvider,
    addProvider,
    updateProvider,
    removeProvider,
  } = useAgentProviderStore();

  const [activeTab, setActiveTab] = useState<string>('list');
  const [editingProvider, setEditingProvider] = useState<AgentProvider | null>(null);

  // 对话框打开时获取最新数据
  useEffect(() => {
    if (visible) {
      fetchProviders();
      setActiveTab('list');
      setEditingProvider(null);
    }
  }, [visible, fetchProviders]);

  // 添加智能体
  const handleAdd = () => {
    setEditingProvider(null);
    setActiveTab('form');
  };

  // 编辑智能体
  const handleEdit = (provider: AgentProvider) => {
    setEditingProvider(provider);
    setActiveTab('form');
  };

  // 删除智能体
  const handleDelete = async (id: number) => {
    await agentProviderAPI.delete(id);
    removeProvider(id);
  };

  // 设置默认智能体
  const handleSetDefault = async (id: number) => {
    await setDefaultProvider(id);
  };

  // 提交表单
  const handleSubmit = async (values: AgentProviderFormData) => {
    if (editingProvider) {
      // 更新 - request拦截器已返回data.data，直接使用response
      const provider = await agentProviderAPI.update(editingProvider.id, values);
      updateProvider(editingProvider.id, provider);
    } else {
      // 创建 - request拦截器已返回data.data，直接使用response
      const provider = await agentProviderAPI.create(values);
      addProvider(provider);
    }

    setActiveTab('list');
    setEditingProvider(null);
  };

  // 取消表单
  const handleCancel = () => {
    setActiveTab('list');
    setEditingProvider(null);
  };

  return (
    <Modal
      title="智能体设置"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={800}
      destroyOnClose
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'list',
            label: '我的智能体',
            children: (
              <div>
                <div className="mb-4">
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={handleAdd}
                  >
                    添加智能体
                  </Button>
                </div>
                <AgentList
                  providers={providers}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onSetDefault={handleSetDefault}
                />
              </div>
            ),
          },
          {
            key: 'form',
            label: editingProvider ? '编辑智能体' : '添加智能体',
            children: (
              <AgentForm
                initialValues={editingProvider || undefined}
                onSubmit={handleSubmit}
                onCancel={handleCancel}
              />
            ),
          },
        ]}
      />
    </Modal>
  );
};

export default AgentSettings;
