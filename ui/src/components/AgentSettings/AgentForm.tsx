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
}

/**
 * 智能体配置表单
 */
export const AgentForm: React.FC<AgentFormProps> = ({
  initialValues,
  onSubmit,
  onCancel,
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

      <Form.Item
        name="apiEndpoint"
        label="API地址"
        rules={[
          { required: true, message: '请输入API地址' },
          { type: 'url', message: '请输入有效的URL' },
        ]}
      >
        <Input placeholder="https://api.example.com/v1/chat" />
      </Form.Item>

      <Form.Item
        name="apiKey"
        label="API密钥"
        rules={[{ required: true, message: '请输入API密钥' }]}
      >
        <Input.Password
          placeholder="请输入API密钥"
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
