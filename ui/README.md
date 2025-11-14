# 华创证券智能体底座 - 前端应用

## 项目简介

华创证券智能体底座前端应用是基于 React 19 和 TypeScript 构建的现代化企业级Web应用，为用户提供直观、高效的智能体交互界面。本应用支持原生多智能体任务执行过程可视化，同时兼容 Coze、通义千问、融汇等第三方智能体平台的接入展示。

**开发单位**: 华创证券科技研发中心
**项目定位**: 企业内部智能体基础设施平台前端

## 技术栈

### 核心框架
- **React 19**: 最新版本React，支持并发渲染和自动批处理
- **TypeScript**: 类型安全的JavaScript超集，提升代码质量
- **Vite 6.1.0**: 新一代前端构建工具，提供极速的开发体验

### UI组件库
- **Ant Design 5.26.3**: 企业级UI组件库，提供丰富的组件
- **Tailwind CSS 4.1.11**: 原子化CSS框架，快速构建响应式界面
- **Lucide React**: 现代化图标库

### 状态管理与路由
- **Zustand 5.0.3**: 轻量级状态管理库，替代Redux
- **React Router 7.6.2**: 声明式路由管理

### 工具库
- **Axios**: HTTP客户端，支持拦截器和请求取消
- **Day.js**: 轻量级日期处理库
- **React Markdown**: Markdown渲染组件
- **Prism React Renderer**: 代码高亮显示
- **React Syntax Highlighter**: 高级代码语法高亮

### 开发工具
- **ESLint**: 代码质量检查工具
- **Prettier**: 代码格式化工具
- **TypeScript ESLint**: TypeScript专用ESLint插件

## 项目结构

```
ui/
├── src/
│   ├── api/                          # API接口层
│   │   ├── chat.ts                   # 聊天相关API
│   │   ├── auth.ts                   # 认证相关API
│   │   ├── session.ts                # 会话管理API
│   │   ├── agentProvider.ts          # 智能体配置API
│   │   └── request.ts                # Axios封装和拦截器
│   ├── components/                   # 组件库
│   │   ├── ChatView/                 # 主聊天界面组件
│   │   │   ├── ChatView.tsx          # 聊天视图主组件
│   │   │   ├── Dialogue.tsx          # 对话气泡组件
│   │   │   ├── MessageList.tsx       # 消息列表组件
│   │   │   └── InputArea.tsx         # 输入区域组件
│   │   ├── Sidebar/                  # 侧边栏组件
│   │   │   ├── Sidebar.tsx           # 侧边栏主组件
│   │   │   ├── SessionList.tsx       # 会话列表组件
│   │   │   └── UserProfile.tsx       # 用户信息组件
│   │   ├── ActionPanel/              # 操作面板组件
│   │   │   ├── ActionPanel.tsx       # 主面板组件
│   │   │   ├── FileViewer.tsx        # 文件查看器
│   │   │   ├── SearchResult.tsx      # 搜索结果展示
│   │   │   └── DataTable.tsx         # 数据表格展示
│   │   ├── PlanView/                 # 计划视图组件
│   │   │   ├── PlanView.tsx          # 任务计划可视化
│   │   │   ├── TaskNode.tsx          # 任务节点组件
│   │   │   └── TaskFlow.tsx          # 任务流程图
│   │   ├── FileUploader/             # 文件上传组件
│   │   │   └── FileUploader.tsx      # 支持拖拽上传
│   │   └── common/                   # 通用组件
│   │       ├── Loading.tsx           # 加载状态组件
│   │       ├── ErrorBoundary.tsx     # 错误边界组件
│   │       └── MarkdownRenderer.tsx  # Markdown渲染器
│   ├── adapters/                     # 智能体平台适配器
│   │   ├── types.ts                  # 适配器类型定义
│   │   ├── DefaultAdapter.tsx        # 原生智能体适配器
│   │   ├── CozeAdapter.tsx           # Coze平台适配器
│   │   ├── TongyiAdapter.tsx         # 通义千问适配器
│   │   └── RonghuiAdapter.tsx        # 融汇平台适配器
│   ├── pages/                        # 页面组件
│   │   ├── Chat/                     # 聊天页面
│   │   │   └── index.tsx
│   │   ├── Login/                    # 登录页面
│   │   │   └── index.tsx
│   │   └── Settings/                 # 设置页面
│   │       └── index.tsx
│   ├── store/                        # 状态管理
│   │   ├── session.ts                # 会话状态
│   │   ├── user.ts                   # 用户状态
│   │   ├── chat.ts                   # 聊天状态
│   │   └── agent.ts                  # 智能体状态
│   ├── utils/                        # 工具函数
│   │   ├── request.ts                # 请求工具
│   │   ├── querySSE.ts               # SSE流式请求工具
│   │   ├── storage.ts                # 本地存储工具
│   │   └── format.ts                 # 格式化工具
│   ├── types/                        # TypeScript类型定义
│   │   ├── chat.ts                   # 聊天相关类型
│   │   ├── session.ts                # 会话相关类型
│   │   ├── user.ts                   # 用户相关类型
│   │   └── agent.ts                  # 智能体相关类型
│   ├── styles/                       # 样式文件
│   │   ├── index.css                 # 全局样式
│   │   └── tailwind.css              # Tailwind配置
│   ├── App.tsx                       # 应用根组件
│   ├── main.tsx                      # 应用入口
│   └── router.tsx                    # 路由配置
├── public/                           # 静态资源
│   ├── favicon.ico                   # 网站图标
│   └── logo.png                      # 应用Logo
├── index.html                        # HTML模板
├── vite.config.ts                    # Vite配置文件
├── tsconfig.json                     # TypeScript配置
├── tailwind.config.js                # Tailwind配置
├── eslint.config.js                  # ESLint配置
├── .prettierrc                       # Prettier配置
├── package.json                      # 项目依赖配置
└── README.md
```

## 核心功能模块

### 1. 智能体交互界面 (ChatView)

**主要特性**:
- 实时流式对话显示（基于SSE技术）
- 支持Markdown格式消息渲染
- 代码块语法高亮显示
- 支持表格、图表等富文本内容
- 文件上传与预览功能
- 消息复制、重新生成等操作

**流式响应处理**:
```typescript
// 使用querySSE工具处理服务器推送事件
querySSE('/api/chat/stream', {
  query: userInput,
  sessionId: currentSessionId,
  agentType: 'plansolve'
}, {
  onMessage: (data) => {
    // 实时更新消息内容
    updateMessage(data);
  },
  onComplete: () => {
    // 对话完成处理
    finalizeMessage();
  },
  onError: (error) => {
    // 错误处理
    handleError(error);
  }
});
```

### 2. 多智能体适配器系统 (adapters/)

**设计模式**: 策略模式
**功能**: 根据智能体平台类型动态选择渲染策略

**支持的平台**:
- **DefaultAdapter**: 原生智能体（PlanSolve、ReAct）
  - 显示完整的思考过程
  - 可视化任务执行计划
  - 展示工具调用详情
  - 支持多步骤任务流程图

- **CozeAdapter**: Coze平台智能体
  - 适配Coze平台消息格式
  - 展示Coze特有的交互元素

- **TongyiAdapter**: 通义千问平台智能体
  - 适配通义千问响应格式
  - 支持通义特有的功能展示

- **RonghuiAdapter**: 融汇平台智能体
  - 适配融汇平台消息结构
  - 展示融汇平台特定功能

**使用示例**:
```typescript
// 根据agentProviderType动态渲染
const adapter = getAdapter(agentProviderType);
return <adapter.MessageComponent message={message} />;
```

### 3. 会话管理系统 (Sidebar & SessionStore)

**功能特性**:
- 会话列表展示（支持分页加载）
- 创建新会话
- 删除会话（软删除）
- 修改会话标题
- 会话搜索与过滤
- 会话切换与历史恢复

**状态管理** (Zustand):
```typescript
interface SessionStore {
  sessions: Session[];
  currentSessionId: string | null;

  // 操作方法
  fetchSessions: () => Promise<void>;
  createSession: (title?: string) => Promise<Session>;
  deleteSession: (sessionId: string) => Promise<void>;
  updateSessionTitle: (sessionId: string, title: string) => Promise<void>;
  setCurrentSession: (sessionId: string) => void;
}
```

### 4. 任务可视化组件 (PlanView)

**原生智能体专属功能**:
- 任务计划树形展示
- 任务执行状态实时更新
- 子任务依赖关系可视化
- 工具调用详情展示
- 执行时间线展示

**展示内容**:
- **规划阶段**: 显示任务拆解结果
- **执行阶段**: 实时更新任务执行状态
- **总结阶段**: 展示最终结果汇总

### 5. 文件处理系统 (FileUploader & ActionPanel)

**支持的文件类型**:
- 文档类: PDF, Word, Excel, TXT, Markdown
- 图片类: PNG, JPG, JPEG, GIF, SVG
- 数据类: CSV, JSON, XML
- 代码类: 所有主流编程语言文件

**功能特性**:
- 拖拽上传
- 批量上传
- 文件预览
- 上传进度显示
- 文件类型校验
- 文件大小限制（默认10MB）

### 6. 认证与权限 (Auth)

**认证机制**: JWT Token
**存储方式**: LocalStorage

**主要流程**:
1. 用户登录后获取Token
2. 所有API请求自动携带Token
3. Token过期自动跳转登录页
4. 支持Token自动续期

**Axios拦截器配置**:
```typescript
// 请求拦截器：添加Token
axios.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理401错误
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // 跳转登录页
      navigateToLogin();
    }
    return Promise.reject(error);
  }
);
```

## 配置说明

### 环境变量配置

创建 `.env` 文件:
```env
# API服务地址
VITE_API_BASE_URL=http://localhost:8080

# 应用端口
VITE_PORT=5173

# 应用标题
VITE_APP_TITLE=华创证券智能体底座

# 上传文件大小限制（字节）
VITE_MAX_FILE_SIZE=10485760

# SSE超时时间（毫秒）
VITE_SSE_TIMEOUT=300000
```

### Vite配置 (vite.config.ts)

```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['antd', '@ant-design/icons'],
          'utils-vendor': ['axios', 'dayjs', 'zustand']
        }
      }
    }
  }
});
```

### Tailwind配置 (tailwind.config.js)

```javascript
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1890ff',
        success: '#52c41a',
        warning: '#faad14',
        error: '#ff4d4f',
      }
    },
  },
  plugins: [],
}
```

## 安装和运行

### 前置要求

- **Node.js**: 18.0.0 或更高版本
- **pnpm**: 8.0.0 或更高版本（推荐使用pnpm）
- **后端服务**: 确保后端服务已启动（端口8080）

### 本地开发

1. 安装依赖:
```bash
pnpm install
```

2. 配置环境变量:
```bash
# Windows
copy .env.example .env

# Linux/Mac
cp .env.example .env
```

3. 启动开发服务器:
```bash
pnpm run dev
```

4. 在浏览器中访问: `http://localhost:5173`

### 生产构建

1. 构建生产版本:
```bash
pnpm run build
```

2. 预览生产构建:
```bash
pnpm run preview
```

3. 部署到服务器:
```bash
# 将dist目录下的文件部署到Web服务器（如Nginx）
cp -r dist/* /var/www/html/
```

### Nginx部署配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/html;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # SSE支持
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding on;
    }
}
```

## 开发指南

### 代码规范

- 使用ESLint和Prettier保证代码质量
- 遵循Airbnb JavaScript Style Guide
- 组件命名使用PascalCase
- 文件命名使用kebab-case或PascalCase（组件文件）
- 所有导出的函数和组件必须添加JSDoc注释

### 可用脚本

```bash
# 启动开发服务器
pnpm run dev

# 构建生产版本
pnpm run build

# 预览生产构建
pnpm run preview

# 代码检查
pnpm run lint

# 自动修复代码问题
pnpm run lint:fix

# 格式化代码
pnpm run format

# 类型检查
pnpm run type-check
```

### 添加新的智能体平台适配器

1. 在 `src/adapters/` 目录下创建新适配器:
```typescript
// NewPlatformAdapter.tsx
import { AdapterComponent } from './types';

export const NewPlatformAdapter: AdapterComponent = {
  MessageComponent: ({ message }) => {
    // 实现消息渲染逻辑
    return <div>{/* 自定义渲染 */}</div>;
  },

  PlanComponent: ({ plan }) => {
    // 实现计划展示逻辑（如果需要）
    return <div>{/* 自定义渲染 */}</div>;
  }
};
```

2. 在适配器工厂中注册:
```typescript
// adapters/index.ts
export const getAdapter = (providerType: string) => {
  switch (providerType) {
    case 'new_platform':
      return NewPlatformAdapter;
    // ... 其他平台
    default:
      return DefaultAdapter;
  }
};
```

### 组件开发规范

**函数组件示例**:
```typescript
import React, { useState, useEffect } from 'react';

interface MyComponentProps {
  title: string;
  onSubmit?: (value: string) => void;
}

/**
 * 我的组件描述
 * @param props - 组件属性
 */
export const MyComponent: React.FC<MyComponentProps> = ({
  title,
  onSubmit
}) => {
  const [value, setValue] = useState('');

  useEffect(() => {
    // 副作用逻辑
  }, []);

  const handleSubmit = () => {
    onSubmit?.(value);
  };

  return (
    <div>
      <h1>{title}</h1>
      <button onClick={handleSubmit}>提交</button>
    </div>
  );
};
```

### 状态管理规范

**Zustand Store示例**:
```typescript
import { create } from 'zustand';

interface MyStore {
  count: number;
  increase: () => void;
  decrease: () => void;
}

export const useMyStore = create<MyStore>((set) => ({
  count: 0,
  increase: () => set((state) => ({ count: state.count + 1 })),
  decrease: () => set((state) => ({ count: state.count - 1 })),
}));
```

### 调试技巧

**React DevTools**:
- 安装Chrome扩展: React Developer Tools
- 检查组件树和Props
- 分析组件渲染性能

**网络调试**:
- 使用浏览器开发者工具的Network面板
- 观察SSE事件流
- 检查API请求和响应

**VSCode调试配置** (.vscode/launch.json):
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Chrome",
      "url": "http://localhost:5173",
      "webRoot": "${workspaceFolder}/src"
    }
  ]
}
```

## 常见问题

### Q1: 开发服务器启动失败

**A**: 检查以下几点:
1. Node.js版本是否 >= 18
2. 端口5173是否被占用
3. 依赖是否正确安装 (`pnpm install`)
4. 删除 `node_modules` 和 `pnpm-lock.yaml` 后重新安装

### Q2: API请求失败（CORS错误）

**A**:
1. 确保后端服务已启动（端口8080）
2. 检查Vite的proxy配置是否正确
3. 后端需要配置CORS允许前端域名

### Q3: SSE流式响应中断

**A**:
1. 检查网络代理设置
2. 延长SSE超时时间（`.env`中的`VITE_SSE_TIMEOUT`）
3. 检查后端日志是否有异常

### Q4: 文件上传失败

**A**:
1. 检查文件大小是否超过限制
2. 检查文件类型是否被允许
3. 检查后端上传接口是否正常
4. 查看浏览器控制台的错误信息

### Q5: Token验证失败

**A**:
1. 检查LocalStorage中是否有有效Token
2. Token是否已过期
3. 清除浏览器缓存和LocalStorage后重新登录
4. 检查后端JWT配置

### Q6: 历史会话无法正确显示

**A**:
1. 检查 `getSessionMessages` API是否返回正确的 `agentProviderType` 字段
2. 确认对应平台的适配器已实现
3. 查看浏览器控制台是否有渲染错误

### Q7: Tailwind样式不生效

**A**:
1. 检查 `tailwind.config.js` 的 `content` 配置是否包含所有源文件
2. 确认 `src/styles/tailwind.css` 已在 `main.tsx` 中导入
3. 重启开发服务器

## 性能优化

### 打包优化
- 使用代码分割（Code Splitting）减小首屏加载体积
- 配置 `manualChunks` 分离第三方库
- 启用Gzip压缩
- 使用CDN加载大型依赖库

### 运行时优化
- 使用 `React.memo` 避免不必要的重渲染
- 合理使用 `useMemo` 和 `useCallback`
- 虚拟滚动处理长列表（react-window）
- 图片懒加载

### 网络优化
- API请求防抖和节流
- 实现请求缓存机制
- 使用Service Worker离线缓存

## 技术支持

**内部技术支持**:
- 联系人: 华创证券科技研发中心
- 技术文档: 见项目Wiki
- Issue追踪: 内部JIRA系统

**相关文档**:
- [后端服务文档](../genie-backend/README.md)
- [AI工具服务文档](../genie-tool/README.md)
- [数据库设计文档](../database_schema.md)
- [API接口文档](http://localhost:8080/swagger-ui.html)
