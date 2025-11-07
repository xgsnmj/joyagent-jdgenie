/**
 * 智能体服务商类型定义
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

/**
 * 平台类型
 */
export type ProviderType = 'default' | 'coze' | 'ronghui' | 'tongyi' | 'dify';

/**
 * 智能体服务商配置
 */
export interface AgentProvider {
  /** 主键ID */
  id: number;
  /** 所属用户ID（保留向后兼容）*/
  userId: number;
  /** 创建者用户ID */
  creatorId: number;
  /** 平台类型 */
  providerType: ProviderType;
  /** 应用名称（最多10个字）*/
  providerName: string;
  /** 智能体简介（最多500字符）*/
  description?: string;
  /** 智能体图标URL */
  icon?: string;
  /** 分类标签 */
  category?: string;
  /** API请求地址 */
  apiEndpoint?: string;
  /** API密钥（脱敏后）*/
  apiKey?: string;
  /** Coze平台的Bot ID（仅coze类型必填）*/
  botId?: string;
  /** 通义点金的工作空间ID（仅tongyi类型必填）*/
  workspaceId?: string;
  /** 租户ID（仅ronghui类型使用）*/
  tenantId?: string;
  /** 登录用户ID（仅ronghui类型使用）*/
  loginUserId?: string;
  /** 登录部门ID（仅ronghui类型使用）*/
  loginDeptId?: string;
  /** 登录用户名（仅ronghui类型使用）*/
  loginUsername?: string;
  /** 是否为该用户的默认智能体 */
  isDefault: boolean;
  /** 是否公开（1-公开，0-私有）*/
  isPublic: boolean;
  /** 使用次数统计 */
  usageCount?: number;
  /** 状态：0-禁用 1-启用 */
  status: number;
  /** 额外配置（JSON格式）*/
  extraConfig?: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 创建/更新智能体配置请求
 */
export interface AgentProviderRequest {
  /** 平台类型 */
  providerType: ProviderType;
  /** 应用名称（最多10个字）*/
  providerName: string;
  /** 智能体简介（最多500字符）*/
  description?: string;
  /** 智能体图标URL */
  icon?: string;
  /** 分类标签 */
  category?: string;
  /** API请求地址 */
  apiEndpoint?: string;
  /** API密钥 */
  apiKey?: string;
  /** Coze平台的Bot ID（仅coze类型必填）*/
  botId?: string;
  /** 通义点金的工作空间ID（仅tongyi类型必填）*/
  workspaceId?: string;
  /** 租户ID（仅ronghui类型使用）*/
  tenantId?: string;
  /** 登录用户ID（仅ronghui类型使用）*/
  loginUserId?: string;
  /** 登录部门ID（仅ronghui类型使用）*/
  loginDeptId?: string;
  /** 登录用户名（仅ronghui类型使用）*/
  loginUsername?: string;
  /** 是否为该用户的默认智能体 */
  isDefault?: boolean;
  /** 是否公开（1-公开，0-私有）*/
  isPublic?: boolean;
  /** 状态：0-禁用 1-启用 */
  status?: number;
  /** 额外配置（JSON格式）*/
  extraConfig?: string;
}

/**
 * 智能体配置表单数据
 */
export interface AgentProviderFormData extends AgentProviderRequest {
  /** 主键ID（编辑时使用）*/
  id?: number;
}

/**
 * 平台类型选项
 */
export interface ProviderTypeOption {
  value: ProviderType;
  label: string;
  description?: string;
}

/**
 * 平台类型配置
 */
export const PROVIDER_TYPE_OPTIONS: ProviderTypeOption[] = [
  {
    value: 'coze',
    label: 'Coze',
    description: 'Coze智能体平台'
  },
  {
    value: 'ronghui',
    label: '融汇',
    description: '融汇智能体平台'
  },
  {
    value: 'tongyi',
    label: '通义点金',
    description: '阿里通义千问智能体平台'
  },
  {
    value: 'dify',
    label: 'Dify',
    description: 'Dify开源LLM应用平台'
  }
];

/**
 * 平台类型显示名称映射
 */
export const PROVIDER_TYPE_LABELS: Record<ProviderType, string> = {
  default: 'JDGenie（本地）',
  coze: 'Coze',
  ronghui: '融汇',
  tongyi: '通义点金',
  dify: 'Dify'
};
