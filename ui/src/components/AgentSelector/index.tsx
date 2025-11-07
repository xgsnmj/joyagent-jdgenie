/**
 * 智能体选择器组件
 * 用于在对话框中选择要使用的智能体
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import React from 'react';
import { Select } from 'antd';
import { RobotOutlined } from '@ant-design/icons';
import type { AgentProvider } from '@/types/agentProvider';
import { PROVIDER_TYPE_LABELS } from '@/types/agentProvider';
import './index.css';

interface AgentSelectorProps {
  /** 当前选中的智能体ID */
  value?: number;
  /** 智能体配置列表 */
  providers: AgentProvider[];
  /** 选择变化回调 */
  onChange: (providerId: number) => void;
  /** 是否禁用（历史会话中禁用）*/
  disabled?: boolean;
  /** 尺寸 */
  size?: 'small' | 'middle' | 'large';
  /** 自定义样式 */
  style?: React.CSSProperties;
  /** 自定义类名 */
  className?: string;
}

/**
 * 获取平台类型对应的图标颜色
 */
const getProviderColor = (providerType: string): string => {
  switch (providerType) {
    case 'default':
      return '#4040ff';
    case 'coze':
      return '#29CC29';
    case 'ronghui':
      return '#FF860D';
    default:
      return '#666';
  }
};

/**
 * 获取平台类型对应的徽章样式类名
 */
const getProviderBadgeClass = (providerType: string): string => {
  switch (providerType) {
    case 'default':
      return 'agent-badge-default';
    case 'coze':
      return 'agent-badge-coze';
    case 'ronghui':
      return 'agent-badge-ronghui';
    default:
      return 'agent-badge-default';
  }
};

/**
 * 智能体选择器
 * 支持下拉选择，展示智能体名称和平台类型
 */
export const AgentSelector: React.FC<AgentSelectorProps> = ({
  value,
  providers,
  onChange,
  disabled = false,
  size = 'middle',
  style,
  className,
}) => {
  return (
    <Select
      value={value}
      onChange={onChange}
      disabled={disabled}
      size={size}
      style={{ width: 200, ...style }}
      className={`custom-agent-selector ${className || ''}`}
      // suffixIcon={<RobotOutlined style={{ color: '#4040ff' }} />}
      placeholder="选择智能体"
      optionFilterProp="children"
      showSearch
      filterOption={(input, option) =>
        (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
      }
      popupClassName="custom-agent-dropdown"
      getPopupContainer={(trigger) => trigger.parentElement || document.body}
    >
      {providers.map((provider) => (
        <Select.Option
          key={provider.id}
          value={provider.id}
          label={provider.providerName}
        >
          <div className="agent-option-content">
            {/* 平台类型图标（使用不同颜色）*/}
            <RobotOutlined
              className="agent-option-icon"
              style={{
                color: getProviderColor(provider.providerType)
              }}
            />

            {/* 智能体名称 */}
            <span className="agent-option-name">
              {provider.providerName}
            </span>

            {/* 平台标签 */}
            <span className={`agent-option-badge ${getProviderBadgeClass(provider.providerType)}`}>
              {PROVIDER_TYPE_LABELS[provider.providerType]}
            </span>

            {/* 默认标识 */}
            {provider.isDefault && (
              <span className="agent-option-badge agent-badge-is-default">
                默认
              </span>
            )}
          </div>
        </Select.Option>
      ))}
    </Select>
  );
};

export default AgentSelector;
