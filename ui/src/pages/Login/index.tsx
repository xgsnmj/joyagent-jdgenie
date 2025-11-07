import { useState, memo } from 'react';
import { Form, Input, Button, Tabs, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { login as loginApi, register as registerApi } from '@/api/user';
import { useUserStore } from '@/store/user';
import './index.css';

type LoginProps = Record<string, never>;

/**
 * 登录注册页面组件
 * 包含登录和注册两个Tab切换
 * 使用蓝紫色主题风格
 */
const Login: GenieType.FC<LoginProps> = memo(() => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');

  const { setToken, setUserInfo } = useUserStore();

  /**
   * 处理登录表单提交
   */
  const handleLogin = async (values: any) => {
    try {
      setLoading(true);
      const response = await loginApi(values);

      // 保存token和用户信息
      setToken(response.token);
      setUserInfo(response.userInfo);

      message.success('登录成功');

      // 跳转到来源页面或首页
      const redirect = searchParams.get('redirect') || '/';
      navigate(redirect, { replace: true });
    } catch (error: any) {
      message.error(error.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 处理注册表单提交
   */
  const handleRegister = async (values: any) => {
    try {
      setLoading(true);
      const response = await registerApi(values);

      // 保存token和用户信息
      setToken(response.token);
      setUserInfo(response.userInfo);

      message.success('注册成功');

      // 跳转到首页
      navigate('/', { replace: true });
    } catch (error: any) {
      message.error(error.message || '注册失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 登录表单
   */
  const LoginForm = (
    <Form
      form={loginForm}
      name="login"
      onFinish={handleLogin}
      size="large"
      className="login-form"
    >
      <Form.Item
        name="account"
        rules={[
          { required: true, message: '请输入用户名或邮箱' },
        ]}
      >
        <Input
          prefix={<UserOutlined className="text-[#4040ff]" />}
          placeholder="用户名 / 邮箱"
          autoComplete="username"
        />
      </Form.Item>

      <Form.Item
        name="password"
        rules={[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码至少6个字符' },
        ]}
      >
        <Input.Password
          prefix={<LockOutlined className="text-[#4040ff]" />}
          placeholder="密码"
          autoComplete="current-password"
        />
      </Form.Item>

      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          className="login-form-button"
          loading={loading}
          block
        >
          登录
        </Button>
      </Form.Item>
    </Form>
  );

  /**
   * 注册表单
   */
  const RegisterForm = (
    <Form
      form={registerForm}
      name="register"
      onFinish={handleRegister}
      size="large"
      className="login-form"
    >
      <Form.Item
        name="username"
        rules={[
          { required: true, message: '请输入用户名' },
          { min: 2, max: 20, message: '用户名长度为2-20个字符' },
          { pattern: /^[\u4e00-\u9fa5a-zA-Z_]+$/, message: '用户名只能包含中文、英文字母和下划线' },
        ]}
      >
        <Input
          prefix={<UserOutlined className="text-[#4040ff]" />}
          placeholder="用户名"
          autoComplete="username"
        />
      </Form.Item>

      <Form.Item
        name="email"
        rules={[
          { required: true, message: '请输入邮箱' },
          { type: 'email', message: '请输入有效的邮箱地址' },
        ]}
      >
        <Input
          prefix={<MailOutlined className="text-[#4040ff]" />}
          placeholder="邮箱"
          autoComplete="email"
        />
      </Form.Item>

      <Form.Item
        name="password"
        rules={[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码至少6个字符' },
        ]}
      >
        <Input.Password
          prefix={<LockOutlined className="text-[#4040ff]" />}
          placeholder="密码"
          autoComplete="new-password"
        />
      </Form.Item>

      <Form.Item
        name="confirmPassword"
        dependencies={['password']}
        rules={[
          { required: true, message: '请确认密码' },
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue('password') === value) {
                return Promise.resolve();
              }
              return Promise.reject(new Error('两次输入的密码不一致'));
            },
          }),
        ]}
      >
        <Input.Password
          prefix={<LockOutlined className="text-[#4040ff]" />}
          placeholder="确认密码"
          autoComplete="new-password"
        />
      </Form.Item>

      <Form.Item>
        <Button
          type="primary"
          htmlType="submit"
          className="login-form-button"
          loading={loading}
          block
        >
          注册
        </Button>
      </Form.Item>
    </Form>
  );

  return (
    <div className="login-container">
      {/* 渐变背景 */}
      <div className="login-background"></div>

      {/* 登录卡片 */}
      <div className="login-card">
        {/* Logo和标题 */}
        <div className="login-header">
          <h1 className="login-title">华创证券智能问答助手</h1>
          <p className="login-subtitle">专业智能数据助手</p>
        </div>

        {/* 登录/注册表单 */}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          centered
          items={[
            {
              key: 'login',
              label: '登录',
              children: LoginForm,
            },
            {
              key: 'register',
              label: '注册',
              children: RegisterForm,
            },
          ]}
        />
      </div>
    </div>
  );
});

Login.displayName = 'Login';

export default Login;
