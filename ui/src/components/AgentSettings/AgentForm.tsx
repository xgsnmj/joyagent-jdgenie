/**
 * 智能体配置表单组件
 * 用于创建和编辑智能体配置
 *
 * @author JDGenie Team
 * @since 2025-01-03
 */

import React, { useEffect } from 'react';
import { Form, Input, Select, Switch, Button, Space, message } from 'antd';
import type { AgentProviderFormData } from '@/types/agentProvider';
import { PROVIDER_TYPE_OPTIONS } from '@/types/agentProvider';

interface AgentFormProps {
  /** 初始值（编辑时使用）*/
  initialValues?: AgentProviderFormData;
  /** 提交回调 */
  onSubmit: (values: AgentProviderFormData) => Promise<void>;
  /** 取消回调 */
  onCancel?: () => void;
  /** 是否显示扩展字段（icon、description、category、isPublic）*/
  showExtendedFields?: boolean;
}

/**
 * 智能体配置表单
 */
export const AgentForm: React.FC<AgentFormProps> = ({
  initialValues,
  onSubmit,
  onCancel,
  showExtendedFields = false,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);

  // 监听平台类型变化，用于条件显示Bot ID字段
  const providerType = Form.useWatch('providerType', form);

  useEffect(() => {
    if (initialValues) {
      form.setFieldsValue(initialValues);
    } else {
      form.resetFields();
    }
  }, [initialValues, form]);

  const handleSubmit = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();
      await onSubmit(values);
      message.success('保存成功');
      form.resetFields();
    } catch (error: any) {
      if (error.errorFields) {
        // 表单校验错误
        console.log('表单校验失败:', error);
      } else {
        // 接口错误
        message.error(error.message || '保存失败');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{
        status: 1,
        isDefault: false,
        isPublic: true, // 默认公开
        ...initialValues,
      }}
    >
      <Form.Item
        name="providerType"
        label="平台类型"
        rules={[{ required: true, message: '请选择平台类型' }]}
      >
        <Select
          placeholder="请选择平台类型"
          options={PROVIDER_TYPE_OPTIONS}
          disabled={!!initialValues?.id} // 编辑时不允许修改平台类型
        />
      </Form.Item>

      <Form.Item
        name="providerName"
        label="应用名称"
        rules={[
          { required: true, message: '请输入应用名称' },
          { max: 10, message: '应用名称不能超过10个字' },
        ]}
      >
        <Input
          placeholder="请输入应用名称（最多10个字）"
          maxLength={10}
          showCount
        />
      </Form.Item>

      {/* 扩展字段：智能体图标 */}
      {showExtendedFields && (
        <Form.Item
          name="icon"
          label="智能体图标"
          tooltip="输入图标URL地址"
        >
          <Input placeholder="https://example.com/icon.png" />
        </Form.Item>
      )}

      {/* 扩展字段：智能体简介 */}
      {showExtendedFields && (
        <Form.Item
          name="description"
          label="智能体简介"
          rules={[{ max: 500, message: '简介不能超过500字符' }]}
          tooltip="简要描述智能体的功能和用途（最多500字符）"
        >
          <Input.TextArea
            rows={4}
            showCount
            maxLength={500}
            placeholder="请输入智能体简介，帮助用户了解其功能..."
          />
        </Form.Item>
      )}

      {/* 扩展字段：分类标签 */}
      {showExtendedFields && (
        <Form.Item
          name="category"
          label="分类标签"
          tooltip="选择智能体所属分类，便于用户筛选"
        >
          <Select placeholder="请选择分类">
            <Select.Option value="场外衍生品部">场外衍生品部</Select.Option>
            <Select.Option value="风险管理部">风险管理部</Select.Option>
            <Select.Option value="风险项目处置办公室">风险项目处置办公室</Select.Option>
            <Select.Option value="固定收益部">固定收益部</Select.Option>
            <Select.Option value="合规与法律事务部">合规与法律事务部</Select.Option>
            <Select.Option value="稽核审计部">稽核审计部</Select.Option>
            <Select.Option value="计划财务部">计划财务部</Select.Option>
            <Select.Option value="机构服务部">机构服务部</Select.Option>
            <Select.Option value="机构经纪部">机构经纪部</Select.Option>
            <Select.Option value="机构和干部管理部">机构和干部管理部</Select.Option>
            <Select.Option value="金融同业服务部">金融同业服务部</Select.Option>
            <Select.Option value="科技研发中心">科技研发中心</Select.Option>
            <Select.Option value="内核工作部">内核工作部</Select.Option>
            <Select.Option value="培训中心">培训中心</Select.Option>
            <Select.Option value="企业及资产并购部">企业及资产并购部</Select.Option>
            <Select.Option value="权益投资部">权益投资部</Select.Option>
            <Select.Option value="人力资源部">人力资源部</Select.Option>
            <Select.Option value="数字化运营部">数字化运营部</Select.Option>
            <Select.Option value="投资顾问部">投资顾问部</Select.Option>
            <Select.Option value="投资交易部">投资交易部</Select.Option>
            <Select.Option value="投资银行部">投资银行部</Select.Option>
            <Select.Option value="网络金融部">网络金融部</Select.Option>
            <Select.Option value="销售交易部">销售交易部</Select.Option>
            <Select.Option value="信息技术部">信息技术部</Select.Option>
            <Select.Option value="信用交易部">信用交易部</Select.Option>
            <Select.Option value="研究所">研究所</Select.Option>
            <Select.Option value="运维中心">运维中心</Select.Option>
            <Select.Option value="资产管理部">资产管理部</Select.Option>
            <Select.Option value="资产托管部">资产托管部</Select.Option>
            <Select.Option value="资金运营部">资金运营部</Select.Option>
            <Select.Option value="做市交易部">做市交易部</Select.Option>
          </Select>
        </Form.Item>
      )}

      {/* API地址字段 - 通义点金平台时显示为工作空间ID */}
      {providerType === 'tongyi' ? (
        <Form.Item
          name="apiEndpoint"
          label="工作空间ID"
          rules={[
            { required: true, message: '请输入通义点金的工作空间ID' },
            { max: 50, message: '工作空间ID不能超过50个字符' },
          ]}
          tooltip="通义点金平台的工作空间标识，用于API调用"
        >
          <Input
            placeholder="请输入通义点金的工作空间ID"
            maxLength={50}
          />
        </Form.Item>
      ) : (
        <Form.Item
          name="apiEndpoint"
          label="API地址"
          rules={[
            { required: providerType !== 'default', message: '请输入API地址' },
            { type: 'url', message: '请输入有效的URL' },
          ].filter(Boolean)}
        >
          <Input placeholder="https://api.example.com/v1/chat" />
        </Form.Item>
      )}

      <Form.Item
        name="apiKey"
        label="API密钥"
        rules={[
          { required: providerType !== 'default', message: '请输入API密钥' },
          providerType === 'tongyi' && {
            pattern: /^[^:]+:[^:]+$/,
            message: '通义点金的API密钥格式应为：accessKeyId:accessKeySecret'
          }
        ].filter(Boolean)}
        tooltip={
          providerType === 'tongyi'
            ? '请输入格式：accessKeyId:accessKeySecret（两个密钥用冒号分隔）'
            : '请输入对应平台的API密钥'
        }
      >
        <Input.Password
          placeholder={
            providerType === 'tongyi'
              ? '格式：accessKeyId:accessKeySecret'
              : '请输入API密钥'
          }
          autoComplete="off"
        />
      </Form.Item>

      {/* Bot ID字段 - 仅Coze平台显示 */}
      {providerType === 'coze' && (
        <Form.Item
          name="botId"
          label="Bot ID"
          rules={[
            { required: true, message: '请输入Coze平台的Bot ID' },
            { max: 100, message: 'Bot ID不能超过100个字符' },
          ]}
        >
          <Input
            placeholder="请输入Coze平台的Bot ID"
            maxLength={100}
          />
        </Form.Item>
      )}

  
      {/* Bot ID字段 - 仅通义点金平台显示 */}
      {providerType === 'tongyi' && (
        <Form.Item
          name="botId"
          label="Bot ID"
          rules={[
            { required: true, message: '请输入通义点金的Bot ID' },
            { max: 50, message: 'Bot ID不能超过50个字符' },
          ]}
          tooltip="通义点金平台的智能体Bot ID"
        >
          <Input
            placeholder="请输入通义点金的Bot ID"
            maxLength={50}
          />
        </Form.Item>
      )}

      {/* Bot ID字段 - 仅融汇平台显示 */}
      {providerType === 'ronghui' && (
        <Form.Item
          name="botId"
          label="应用ID"
          rules={[
            { required: true, message: '请输入融汇平台的应用ID（app_id）' },
            { max: 50, message: '应用ID不能超过50个字符' },
          ]}
          tooltip="融汇平台的应用ID，即app_id参数"
        >
          <Input
            placeholder="请输入融汇平台的应用ID"
            maxLength={50}
          />
        </Form.Item>
      )}

      {/* 融汇平台专属字段 */}
      {providerType === 'ronghui' && (
        <>
          <Form.Item
            name="tenantId"
            label="租户ID"
            rules={[
              { required: true, message: '请输入租户ID（tenantid）' },
              { max: 50, message: '租户ID不能超过50个字符' },
            ]}
            tooltip="融汇平台的租户标识，用于多租户隔离"
          >
            <Input
              placeholder="请输入租户ID，例如：rxhui"
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            name="loginUserId"
            label="登录用户ID"
            rules={[
              { required: true, message: '请输入登录用户ID' },
              { max: 50, message: '用户ID不能超过50个字符' },
            ]}
            tooltip="融汇平台的用户标识"
          >
            <Input
              placeholder="请输入登录用户ID，例如：1"
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            name="loginDeptId"
            label="登录部门ID"
            rules={[
              { required: true, message: '请输入登录部门ID' },
              { max: 50, message: '部门ID不能超过50个字符' },
            ]}
            tooltip="融汇平台的部门标识"
          >
            <Input
              placeholder="请输入登录部门ID，例如：100"
              maxLength={50}
            />
          </Form.Item>

          <Form.Item
            name="loginUsername"
            label="登录用户名"
            rules={[
              { required: true, message: '请输入登录用户名' },
              { max: 100, message: '用户名不能超过100个字符' },
            ]}
            tooltip="融汇平台的用户名标识"
          >
            <Input
              placeholder="请输入登录用户名，例如：admin"
              maxLength={100}
            />
          </Form.Item>
        </>
      )}

      {/* 扩展字段：公开/私有 */}
      {showExtendedFields && (
        <Form.Item
          name="isPublic"
          label="公开设置"
          valuePropName="checked"
          tooltip="公开后所有用户可在智能体社区查看和使用"
        >
          <Switch checkedChildren="公开" unCheckedChildren="私有" />
        </Form.Item>
      )}

      <Form.Item
        name="status"
        label="状态"
        valuePropName="checked"
        getValueFromEvent={(checked) => (checked ? 1 : 0)}
        getValueProps={(value) => ({ checked: value === 1 })}
      >
        <Switch checkedChildren="启用" unCheckedChildren="禁用" />
      </Form.Item>

      <Form.Item className="mb-0">
        <Space>
          <Button type="primary" onClick={handleSubmit} loading={loading}>
            保存
          </Button>
          {onCancel && (
            <Button onClick={onCancel}>
              取消
            </Button>
          )}
        </Space>
      </Form.Item>
    </Form>
  );
};

export default AgentForm;
