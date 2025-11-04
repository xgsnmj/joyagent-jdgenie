# 数据库设计文档

## 概述

本文档记录JD Genie项目的数据库表结构设计，包含系统用户、会话管理等核心功能模块。

## 数据库信息

- **数据库类型**: MySQL 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci

---

## 表结构设计

### 1. sys_user（系统用户表）

用于存储系统用户的基本信息，包括账号、密码、个人信息等。

**表名**: `sys_user`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 用户ID（主键） |
| username | VARCHAR | 50 | NO | - | NO | NO | 用户名（登录账号，唯一，2-20字符，支持中文、英文、下划线） |
| password | VARCHAR | 200 | NO | - | NO | NO | 密码（BCrypt加密，6-20字符） |
| nickname | VARCHAR | 50 | YES | NULL | NO | NO | 用户昵称（默认使用用户名） |
| email | VARCHAR | 100 | NO | - | NO | NO | 电子邮箱（必填，唯一，支持用户名/邮箱登录） |
| phone | VARCHAR | 20 | YES | NULL | NO | NO | 手机号码 |
| avatar | VARCHAR | 500 | YES | NULL | NO | NO | 头像URL |
| status | TINYINT | - | YES | 0 | NO | NO | 用户状态（0-正常，1-停用，2-禁用） |
| is_admin | TINYINT | - | YES | 0 | NO | NO | 是否管理员（0-否，1-是） |
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |
| update_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 更新时间 |
| last_login_time | DATETIME | - | YES | NULL | NO | NO | 最后登录时间 |
| yn | TINYINT | - | YES | 0 | NO | NO | 逻辑删除（0-未删除，1-已删除） |

**索引**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_username` (`username`) - 用户名唯一索引
- UNIQUE KEY `uk_email` (`email`) - 邮箱唯一索引
- KEY `idx_phone` (`phone`)
- KEY `idx_status` (`status`)

**建表SQL**:
```sql
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号，唯一，2-20字符，支持中文、英文、下划线）',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密，6-20字符）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称（默认使用用户名）',
  `email` VARCHAR(100) NOT NULL COMMENT '电子邮箱（必填，唯一，支持用户名/邮箱登录）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT DEFAULT 0 COMMENT '用户状态（0-正常，1-停用，2-禁用）',
  `is_admin` TINYINT DEFAULT 0 COMMENT '是否管理员（0-否，1-是）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';
```

---

### 2. chat_session（聊天会话表）

用于存储用户的聊天会话信息，每个会话代表一次完整的对话历史。

**表名**: `chat_session`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 会话记录ID（主键） |
| session_id | VARCHAR | 100 | NO | - | NO | NO | 会话唯一标识符（UUID） |
| user_id | BIGINT | - | NO | - | NO | NO | 用户ID（关联sys_user.id） |
| title | VARCHAR | 200 | YES | NULL | NO | NO | 会话标题 |
| agent_type | VARCHAR | 50 | YES | NULL | NO | NO | Agent类型 |
| output_style | VARCHAR | 50 | YES | NULL | NO | NO | 输出样式 |
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |
| update_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 更新时间 |
| yn | TINYINT | - | YES | 0 | NO | NO | 逻辑删除（0-未删除，1-已删除） |

**索引**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_session_id` (`session_id`)
- KEY `idx_user_id` (`user_id`)
- KEY `idx_create_time` (`create_time`)
- KEY `idx_update_time` (`update_time`)

**建表SQL**:
```sql
CREATE TABLE `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话记录ID（主键）',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话唯一标识符（UUID）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `agent_type` VARCHAR(50) DEFAULT NULL COMMENT 'Agent类型',
  `output_style` VARCHAR(50) DEFAULT NULL COMMENT '输出样式',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';
```

---

### 3. chat_message（聊天消息表）

用于存储会话中的每条消息记录，包括用户提问和AI回复。

**表名**: `chat_message`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 消息ID（主键） |
| session_id | VARCHAR | 100 | NO | - | NO | NO | 会话ID（关联chat_session.session_id） |
| role | VARCHAR | 20 | NO | - | NO | NO | 消息角色（user/assistant/system） |
| content | TEXT | - | YES | NULL | NO | NO | 消息内容 |
| files | TEXT | - | YES | NULL | NO | NO | 附件文件信息（JSON格式） |
| thought | TEXT | - | YES | NULL | NO | NO | 思考过程（AI的思维链） |
| tasks | LONGTEXT | - | YES | NULL | NO | NO | 任务详情（JSON数组，包含所有任务执行过程） |
| plan | TEXT | - | YES | NULL | NO | NO | 计划信息（JSON对象，包含计划标题和步骤） |
| metadata | LONGTEXT | - | YES | NULL | NO | NO | 其他元数据（JSON对象，存储额外信息） |
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |
| yn | TINYINT | - | YES | 0 | NO | NO | 逻辑删除（0-未删除，1-已删除） |

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_session_id` (`session_id`)
- KEY `idx_create_time` (`create_time`)

**建表SQL**:
```sql
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID（主键）',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '消息角色（user/assistant/system）',
  `content` TEXT COMMENT '消息内容',
  `files` TEXT COMMENT '附件文件信息（JSON格式）',
  `thought` TEXT COMMENT '思考过程（AI的思维链）',
  `tasks` LONGTEXT COMMENT '任务详情（JSON数组，包含所有任务执行过程）',
  `plan` TEXT COMMENT '计划信息（JSON对象，包含计划标题和步骤）',
  `metadata` LONGTEXT COMMENT '其他元数据（JSON对象，存储额外信息）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';
```

---

### 4. chat_model_info（聊天模型信息表）

用于存储数据模型的配置信息，包括表名、SQL查询、模型名称等。

**表名**: `chat_model_info`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 主键ID |
| code | VARCHAR | 50 | NO | - | NO | NO | 模型编码 |
| type | VARCHAR | 10 | NO | - | NO | NO | 模型类型（TABLE/SQL） |
| name | VARCHAR | 100 | YES | NULL | NO | NO | 模型名称 |
| content | TEXT | - | NO | - | NO | NO | 模型内容（表名或SQL） |
| use_prompt | TEXT | - | YES | NULL | NO | NO | 模型使用说明 |
| business_prompt | TEXT | - | YES | NULL | NO | NO | 模型业务限定提示词 |
| yn | TINYINT | - | YES | 0 | NO | NO | 逻辑删除（0-未删除，1-已删除） |

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_code` (`code`)
- KEY `idx_type` (`type`)

**建表SQL**:
```sql
CREATE TABLE `chat_model_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '模型编码',
  `type` VARCHAR(10) NOT NULL COMMENT '模型类型TABLE,SQL',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `content` TEXT NOT NULL COMMENT '模型内容，表或者sql',
  `use_prompt` TEXT COMMENT '模型使用说明',
  `business_prompt` TEXT COMMENT '模型业务限定提示词',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据模型表信息';
```

---

### 5. chat_model_schema（聊天模型字段表）

用于存储数据模型字段的元数据信息，包括字段名称、类型、同义词等。

**表名**: `chat_model_schema`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 主键ID |
| model_code | VARCHAR | 200 | NO | - | NO | NO | 模型编码 |
| column_id | VARCHAR | 1000 | NO | - | NO | NO | 字段唯一ID |
| column_name | VARCHAR | 200 | NO | - | NO | NO | 字段中文名 |
| column_comment | VARCHAR | 1000 | NO | - | NO | NO | 字段描述 |
| few_shot | TEXT | - | YES | NULL | NO | NO | 值枚举逗号分隔 |
| data_type | VARCHAR | 20 | YES | NULL | NO | NO | 字段值类型 |
| synonyms | VARCHAR | 300 | YES | NULL | NO | NO | 同义词 |
| vector_uuid | VARCHAR | 400 | YES | NULL | NO | NO | 向量库数据ID |
| default_recall | TINYINT | - | YES | 0 | NO | NO | 默认召回 |
| analyze_suggest | TINYINT | - | YES | 0 | NO | NO | 分析建议标识 |
| yn | TINYINT | - | YES | 0 | NO | NO | 逻辑删除（0-未删除，1-已删除） |

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_model_code` (`model_code`)
- KEY `idx_column_name` (`column_name`)

**建表SQL**:
```sql
CREATE TABLE `chat_model_schema` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_code` VARCHAR(200) NOT NULL COMMENT '模型编码',
  `column_id` VARCHAR(1000) NOT NULL COMMENT '字段唯一ID',
  `column_name` VARCHAR(200) NOT NULL COMMENT '字段中文名',
  `column_comment` VARCHAR(1000) NOT NULL COMMENT '字段描述',
  `few_shot` TEXT COMMENT '值枚举逗号分隔',
  `data_type` VARCHAR(20) DEFAULT NULL COMMENT '字段值类型',
  `synonyms` VARCHAR(300) DEFAULT NULL COMMENT '同义词',
  `vector_uuid` VARCHAR(400) DEFAULT NULL COMMENT '向量库数据id',
  `default_recall` TINYINT DEFAULT 0 COMMENT '默认召回',
  `analyze_suggest` TINYINT DEFAULT 0 COMMENT '分析建议0可选，-1禁止用于分析维度，1建议',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model_code` (`model_code`),
  KEY `idx_column_name` (`column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据模型表信息';
```

---

### 6. sales_data（销售数据表）

用于存储超市销售明细数据，包含订单信息、客户信息、产品信息等。

**表名**: `sales_data`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| row_id | INT | - | NO | - | YES | NO | 行ID（主键） |
| order_id | VARCHAR | 50 | YES | NULL | NO | NO | 订单ID |
| order_date | DATE | - | YES | NULL | NO | NO | 订单日期 |
| ship_date | DATE | - | YES | NULL | NO | NO | 发货日期 |
| ship_mode | VARCHAR | 50 | YES | NULL | NO | NO | 邮寄方式 |
| customer_id | VARCHAR | 50 | YES | NULL | NO | NO | 客户ID |
| customer_name | VARCHAR | 100 | YES | NULL | NO | NO | 客户名称 |
| segment | VARCHAR | 50 | YES | NULL | NO | NO | 细分 |
| city | VARCHAR | 100 | YES | NULL | NO | NO | 城市 |
| state_province | VARCHAR | 100 | YES | NULL | NO | NO | 省/自治区 |
| country | VARCHAR | 100 | YES | NULL | NO | NO | 国家 |
| region | VARCHAR | 50 | YES | NULL | NO | NO | 地区 |
| product_id | VARCHAR | 50 | YES | NULL | NO | NO | 产品ID |
| category | VARCHAR | 50 | YES | NULL | NO | NO | 产品类别 |
| sub_category | VARCHAR | 50 | YES | NULL | NO | NO | 产品子类别 |
| product_name | VARCHAR | 255 | YES | NULL | NO | NO | 产品名称 |
| sales | DECIMAL | 10,4 | YES | NULL | NO | NO | 销售额 |
| quantity | INT | - | YES | NULL | NO | NO | 销售数量 |
| discount | DECIMAL | 10,4 | YES | NULL | NO | NO | 折扣 |
| profit | DECIMAL | 10,4 | YES | NULL | NO | NO | 利润 |

**索引**:
- PRIMARY KEY (`row_id`)

**建表SQL**:
```sql
CREATE TABLE `sales_data` (
  `row_id` INT PRIMARY KEY COMMENT '行 ID',
  `order_id` VARCHAR(50) DEFAULT NULL COMMENT '订单 ID',
  `order_date` DATE COMMENT '订单日期',
  `ship_date` DATE COMMENT '发货日期',
  `ship_mode` VARCHAR(50) DEFAULT NULL COMMENT '邮寄方式',
  `customer_id` VARCHAR(50) DEFAULT NULL COMMENT '客户 ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
  `segment` VARCHAR(50) DEFAULT NULL COMMENT '细分',
  `city` VARCHAR(100) DEFAULT NULL COMMENT '城市',
  `state_province` VARCHAR(100) DEFAULT NULL COMMENT '省/自治区',
  `country` VARCHAR(100) DEFAULT NULL COMMENT '国家',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
  `product_id` VARCHAR(50) DEFAULT NULL COMMENT '产品 ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '产品类别',
  `sub_category` VARCHAR(50) DEFAULT NULL COMMENT '产品子类别',
  `product_name` VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
  `sales` DECIMAL(10, 4) DEFAULT NULL COMMENT '销售额',
  `quantity` INT DEFAULT NULL COMMENT '销售数量',
  `discount` DECIMAL(10, 4) DEFAULT NULL COMMENT '折扣',
  `profit` DECIMAL(10, 4) DEFAULT NULL COMMENT '利润'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售数据表';
```

---

### 7. file_info（文件信息表）

用于存储genie-tool上传的文件元数据，包括搜索结果、生成的报告、代码执行结果等文件。

**表名**: `file_info`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|--------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 文件记录ID（主键） |
| file_id | VARCHAR | 64 | NO | - | NO | NO | 文件唯一标识符（MD5哈希值） |
| filename | VARCHAR | 255 | NO | - | NO | NO | 文件名称 |
| file_path | VARCHAR | 500 | NO | - | NO | NO | 文件存储路径 |
| description | VARCHAR | 1000 | YES | NULL | NO | NO | 文件描述 |
| file_size | BIGINT | - | YES | NULL | NO | NO | 文件大小（字节） |
| status | TINYINT | - | YES | 0 | NO | NO | 文件状态（0-正常，1-已删除） |
| request_id | VARCHAR | 200 | YES | NULL | NO | NO | 请求ID/会话ID |
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |

**索引**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_file_id` (`file_id`)
- KEY `idx_request_id` (`request_id`)
- KEY `idx_create_time` (`create_time`)

**建表SQL**:
```sql
CREATE TABLE `file_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件记录ID（主键）',
  `file_id` VARCHAR(64) NOT NULL COMMENT '文件唯一标识符（MD5）',
  `filename` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '文件描述',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `status` TINYINT DEFAULT 0 COMMENT '文件状态（0-正常，1-已删除）',
  `request_id` VARCHAR(200) DEFAULT NULL COMMENT '请求ID/会话ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_id` (`file_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';
```

**字段说明**:
- `file_id`: 通过 `MD5(request_id + filename)` 生成，确保唯一性
- `file_path`: 实际存储路径，经过 `sanitize_path_name()` 清理非法字符
- `request_id`: 关联到会话ID（`chat_session.session_id`）或请求ID，用于文件分组
- `status`: 软删除标识，0表示正常，1表示已删除

---

## 表关系说明

1. **sys_user ↔ chat_session**: 一对多关系
   - 一个用户可以拥有多个会话
   - 通过 `chat_session.user_id` 关联 `sys_user.id`

2. **chat_session ↔ chat_message**: 一对多关系
   - 一个会话可以包含多条消息
   - 通过 `chat_message.session_id` 关联 `chat_session.session_id`

3. **chat_model_info ↔ chat_model_schema**: 一对多关系
   - 一个模型可以包含多个字段配置
   - 通过 `chat_model_schema.model_code` 关联 `chat_model_info.code`

4. **sales_data**: 独立业务数据表
   - 超市销售明细数据，支持DataAgent进行数据查询和分析
   - 不与其他表建立外键关系

---

## 数据字典

### sys_user.status（用户状态）
- `0`: 正常 - 用户可以正常登录和使用系统
- `1`: 停用 - 临时停用，可以恢复
- `2`: 禁用 - 永久禁用，不允许登录

### sys_user.is_admin（是否管理员）
- `0`: 否 - 普通用户
- `1`: 是 - 管理员用户，拥有更多权限

### chat_message.role（消息角色）
- `user`: 用户发送的消息（提问）
- `assistant`: AI助手的回复消息
- `system`: 系统消息（如提示、警告等）

### 通用字段.yn（逻辑删除标识）
- `0`: 未删除 - 数据正常存在
- `1`: 已删除 - 数据已被逻辑删除（但仍保留在数据库中）

---

## 变更记录

### 2025-01-27
- 创建 `sys_user` 表，用于用户认证和管理
- 创建 `chat_session` 表，用于会话管理
- 创建 `chat_message` 表，用于消息存储
- 所有表支持逻辑删除（yn字段）
- 添加必要的索引以优化查询性能

### 2025-10-27（Spring Security迁移后修复）
- 添加 `chat_model_info` 表，用于DataAgent模型配置管理
- 添加 `chat_model_schema` 表，用于存储模型字段元数据
- 添加 `sales_data` 表，用于存储超市销售明细数据
- 统一数据库连接配置（application.yml）
- 修复MyBatis-Plus逻辑删除配置不一致问题
- 更新表关系说明，包含所有6个表的完整关系图

### 2025-10-27（会话管理功能完善）
- **后端接口新增**：
  - 新增 `POST /api/chat/sessions` 创建新会话接口
  - 新增 `GET /api/chat/sessions/{sessionId}/messages` 获取会话消息列表接口
  - 完善 `SessionVO`，添加 `messageCount` 字段用于显示会话消息数量
  - 新增 `MessageVO` 用于返回消息数据
- **前端功能新增**：
  - 新增左侧边栏（Sidebar）组件，显示会话列表、用户信息
  - 新增会话状态管理（session store），支持会话CRUD操作
  - 支持快捷键 Ctrl+K 快速创建新会话
  - 会话列表实时显示消息数量和最后更新时间
  - 用户信息卡片移至侧边栏底部
  - 安装 date-fns 依赖用于时间格式化
- **数据库schema**：无变更，现有表结构已满足需求

### 2025-10-28（用户注册登录功能优化）
- **后端DTO优化**：
  - `UserRegisterDTO`：去除昵称字段，邮箱改为必填，用户名长度改为2-20字符，添加用户名格式校验（支持中文、英文、下划线）
  - `UserLoginDTO`：username字段改为account，支持用户名或邮箱登录
- **后端Service优化**：
  - 用户注册时强制校验用户名和邮箱唯一性
  - 用户登录支持用户名或邮箱两种方式
  - 新增 `getUserByAccount()` 方法支持多方式查询用户
  - 昵称默认使用用户名
- **前端优化**：
  - 注册页面：去除昵称字段，邮箱改为必填，添加用户名格式校验（中文、英文、下划线）
  - 登录页面：支持用户名/邮箱两种方式登录，输入框提示更改为"用户名 / 邮箱"
- **数据库schema变更**：
  - `sys_user.email` 字段改为 NOT NULL（必填）
  - 新增 `uk_email` 唯一索引确保邮箱唯一性
  - 更新用户名和邮箱字段注释，明确格式要求和登录方式

### 2025-10-30（完整会话数据保存功能）
- **数据库schema变更**：
  - `chat_message` 表新增 4 个字段用于保存完整会话数据：
    - `thought` TEXT：思考过程（AI的思维链）
    - `tasks` LONGTEXT：任务详情（JSON数组，包含所有任务执行过程）
    - `plan` TEXT：计划信息（JSON对象，包含计划标题和步骤）
    - `metadata` LONGTEXT：其他元数据（JSON对象，存储额外信息）
- **功能目标**：
  - 保存 AI 对话过程中的完整数据，包括思考过程、任务执行详情、计划信息等
  - 历史会话查看时能够完整还原对话过程，包括所有交互元素
  - 提升用户体验，让历史会话展示更加丰富和完整

### 2025-10-30（thought字段格式优化）
- **后端代码优化**：
  - 修改 `ConversationDataCollector.getThoughtJson()` 方法
  - 移除思考内容的类型标记前缀（`[plan]`、`[tool]`等）
  - 直接保存纯净的思考内容，用双换行符分隔不同思考片段
- **前端代码优化**：
  - 修改 `Dialogue/index.tsx` 组件的条件渲染逻辑
  - 移除 `thought` 和 `planList` 显示的 `deepThink` 限制
  - 只要数据存在就显示，保证历史会话与实时对话显示一致
- **优化效果**：
  - 历史会话的思考过程不再包含技术标记，显示更清晰
  - 实时对话和历史会话的显示体验完全一致
  - 保持数据向后兼容，旧数据依然可以正常显示

### 2025-10-30（会话消息收集功能重构）
- **架构变更**：
  - 从"后端重构前端数据"转变为"前端上报完整数据"的新架构
  - 优先使用 `metadata.multiAgent` 存储前端完整数据结构
  - 保留 `thought/tasks/plan` 字段作为fallback，确保向后兼容
- **后端新增功能**：
  - 新增 `UploadMultiAgentRequest` DTO用于接收前端上报的数据
  - 新增 `POST /api/chat/sessions/{sessionId}/multiagent` API接口
  - 新增 `updateMessageMetadata()` 方法更新消息的metadata字段
  - 新增 `findAssistantMessageByRequestId()` 方法根据requestId查找消息
  - 新增 `isSessionOwner()` 方法验证会话归属权限
  - 增强 `ConversationDataCollector` 添加 `rawMessages` 备份字段
- **前端新增功能**：
  - 新增 `uploadMultiAgentData()` API调用方法
  - 在对话完成时自动上报完整的 `multiAgent` 数据到后端
  - 修改 `convertHistoryMessages()` 优先从 `metadata.multiAgent` 恢复数据
  - 实现fallback机制：metadata不存在时从 `thought/tasks/plan` 重构数据
- **数据流程**：
  1. 实时对话：SSE流 → ConversationDataCollector收集 → 保存到thought/tasks/plan/metadata字段
  2. 对话完成：前端上报multiAgent数据 → 后端更新metadata字段
  3. 历史查询：优先使用metadata.multiAgent → 如无则从thought/tasks/plan重构（fallback）
- **优势**：
  - 100%准确保存前端渲染的所有信息（思考、任务、计划、工具调用等）
  - 历史会话完整恢复，显示效果与实时对话完全一致
  - 向后兼容旧数据，旧会话依然可以正常显示
  - 后端提供双重保障：rawMessages备份 + thought/tasks/plan字段

---

## 注意事项

1. **密码安全**: `sys_user.password` 字段使用BCrypt加密算法存储，长度为200字符足以容纳BCrypt哈希值
2. **逻辑删除**: 所有表都使用 `yn` 字段实现逻辑删除，MyBatis-Plus会自动处理
3. **字符集**: 使用 `utf8mb4` 字符集以支持完整的Unicode字符，包括emoji等特殊字符
4. **时间字段**: `create_time` 和 `update_time` 使用MySQL的自动时间戳功能
5. **索引优化**: 已为常用查询字段添加索引，注意定期分析和优化索引使用情况

---

## 初始化数据

### 创建默认管理员账号（可选）

```sql
-- 插入默认管理员账号
-- 密码: admin123 (BCrypt加密后的值)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`, `is_admin`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 0, 1);
```

**注意**: 上述SQL中的密码已经过BCrypt加密,明文密码为 `admin123`。生产环境请务必修改默认密码。

---

## V2.0 多智能体平台支持

### 8. agent_provider（智能体服务商配置表）

存储用户配置的智能体服务商信息，支持对接外部智能体平台。

**表名**: `agent_provider`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|-------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 主键ID |
| user_id | BIGINT | - | NO | - | NO | NO | 所属用户ID |
| provider_type | VARCHAR | 50 | NO | - | NO | NO | 平台类型：default/coze/ronghui |
| provider_name | VARCHAR | 10 | NO | - | NO | NO | 应用名称（最多10个字）|
| api_endpoint | VARCHAR | 500 | YES | NULL | NO | NO | API请求地址（default类型为空）|
| api_key | VARCHAR | 500 | YES | NULL | NO | NO | API密钥（明文存储）|
| bot_id | VARCHAR | 100 | YES | NULL | NO | NO | Coze平台的Bot ID（仅coze类型必填）|
| is_default | TINYINT | - | YES | 0 | NO | NO | 是否为该用户的默认智能体 |
| status | TINYINT | - | YES | 1 | NO | NO | 状态：0-禁用 1-启用 |
| extra_config | JSON | - | YES | NULL | NO | NO | 额外配置（平台特有参数）|
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |
| update_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 更新时间 |

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_user_id` (`user_id`) - 用户ID索引
- KEY `idx_provider_type` (`provider_type`) - 平台类型索引
- KEY `idx_bot_id` (`bot_id`) - Bot ID索引
- UNIQUE KEY `uk_user_provider_name` (`user_id`, `provider_name`) - 唯一索引

**建表SQL**:
```sql
CREATE TABLE `agent_provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `provider_type` VARCHAR(50) NOT NULL COMMENT '平台类型: default/coze/ronghui',
  `provider_name` VARCHAR(10) NOT NULL COMMENT '应用名称（最多10个字）',
  `api_endpoint` VARCHAR(500) COMMENT 'API请求地址（default类型为空）',
  `api_key` VARCHAR(500) COMMENT 'API密钥（明文存储，default类型为空）',
  `bot_id` VARCHAR(100) COMMENT 'Coze平台的Bot ID（仅coze类型必填）',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为该用户的默认智能体（0-否 1-是）',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态（0-禁用 1-启用）',
  `extra_config` JSON COMMENT '额外配置（JSON格式，存储平台特有参数）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_provider_type` (`provider_type`),
  KEY `idx_bot_id` (`bot_id`),
  UNIQUE KEY `uk_user_provider_name` (`user_id`, `provider_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体服务商配置表';
```

**业务规则**:
1. 每个用户只能有一个默认智能体（通过触发器保证）
2. 同一用户下智能体名称必须唯一
3. default类型的智能体在用户注册时自动创建
4. default类型的智能体不允许编辑和删除
5. 用户只能查看和管理自己创建的智能体配置

**支持的平台类型**:
- `default`: 本地MultiAgent体系（系统内置）
- `coze`: Coze智能体平台（需要配置bot_id）
- `ronghui`: 融汇（阿里点金）智能体平台

---

### 9. sse_message_cache（SSE消息缓存表）

临时存储SSE流消息，用于在流结束后统一持久化到chat_message表。

**表名**: `sse_message_cache`

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 主键 | 自增 | 说明 |
|-------|------|------|----------|--------|------|------|------|
| id | BIGINT | - | NO | - | YES | YES | 主键ID |
| session_id | BIGINT | - | NO | - | NO | NO | 会话ID |
| message_sequence | INT | - | NO | - | NO | NO | 消息序号（从0开始递增）|
| event_type | VARCHAR | 50 | YES | NULL | NO | NO | SSE事件类型（message/error/done）|
| event_data | MEDIUMTEXT | - | YES | NULL | NO | NO | 事件数据内容 |
| raw_data | MEDIUMTEXT | - | YES | NULL | NO | NO | 原始数据（完整SSE消息）|
| is_persisted | TINYINT | - | YES | 0 | NO | NO | 是否已持久化 |
| create_time | DATETIME | - | YES | CURRENT_TIMESTAMP | NO | NO | 创建时间 |

**索引**:
- PRIMARY KEY (`id`)
- KEY `idx_session` (`session_id`, `message_sequence`) - 联合索引
- KEY `idx_persisted` (`is_persisted`, `create_time`) - 联合索引

**建表SQL**:
```sql
CREATE TABLE `sse_message_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `message_sequence` INT NOT NULL COMMENT '消息序号（从0开始递增）',
  `event_type` VARCHAR(50) COMMENT 'SSE事件类型（如message、error、done）',
  `event_data` MEDIUMTEXT COMMENT '事件数据内容',
  `raw_data` MEDIUMTEXT COMMENT '原始数据（完整SSE消息）',
  `is_persisted` TINYINT(1) DEFAULT 0 COMMENT '是否已持久化到chat_message表（0-否 1-是）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `message_sequence`),
  KEY `idx_persisted` (`is_persisted`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSE消息缓存表（临时存储SSE流，待流结束后统一持久化）';
```

**生命周期**:
1. SSE流开始时：逐条写入缓存表
2. SSE流结束时：合并消息并写入chat_message表
3. 持久化后：标记is_persisted=1
4. 定期清理：调用存储过程清理已持久化的缓存

---

### chat_session 表新增字段（V2.0）

**新增字段**:

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 说明 |
|-------|------|------|----------|--------|------|
| agent_provider_id | BIGINT | - | YES | NULL | 关联的智能体服务商ID |
| external_session_id | VARCHAR | 200 | YES | NULL | 外部平台返回的会话ID（用于多轮对话）|

**新增索引**:
- KEY `idx_agent_provider` (`agent_provider_id`)
- KEY `idx_external_session` (`external_session_id`)

**业务说明**:
- `agent_provider_id`: 标识当前会话使用的智能体配置
- `external_session_id`: 存储外部平台（Coze、融汇）返回的会话ID，用于支持多轮对话
- 会话创建时如果未指定agent_provider_id，则使用用户的默认智能体
- 会话中不允许切换智能体，如需更换需创建新会话

---

### chat_message 表新增字段（V2.0）

**新增字段**:

| 字段名 | 类型 | 长度 | 允许NULL | 默认值 | 说明 |
|-------|------|------|----------|--------|------|
| message_format | VARCHAR | 50 | YES | 'default' | 消息格式类型：default/coze/ronghui |
| raw_content | TEXT | - | YES | NULL | 原始响应内容（JSON格式，用于调试）|

**业务说明**:
- `message_format`: 标识消息来自哪个平台，便于前端渲染和调试
- `raw_content`: 存储平台返回的原始JSON响应，便于问题排查
- 不同平台的消息通过适配器统一格式化为标准的content字段

---

### 数据库触发器（V2.0）

#### before_agent_provider_set_default

**触发时机**: 更新agent_provider表之前
**触发条件**: is_default从0变为1
**执行逻辑**: 自动将同一用户的其他智能体的is_default设为0
**作用**: 确保每个用户只有一个默认智能体

**触发器SQL**:
```sql
DELIMITER $$

CREATE TRIGGER before_agent_provider_set_default
BEFORE UPDATE ON agent_provider
FOR EACH ROW
BEGIN
    IF NEW.is_default = 1 AND OLD.is_default = 0 THEN
        UPDATE agent_provider
        SET is_default = 0
        WHERE user_id = NEW.user_id
        AND id != NEW.id
        AND is_default = 1;
    END IF;
END$$

DELIMITER ;
```

---

### 存储过程（V2.0）

#### clean_persisted_sse_cache()

**功能**: 清理已持久化的SSE缓存消息
**执行逻辑**: 删除is_persisted=1的所有记录
**返回值**: 删除的记录数量
**调用时机**: 定时任务或手动调用

**存储过程SQL**:
```sql
DELIMITER $$

CREATE PROCEDURE clean_persisted_sse_cache()
BEGIN
    DELETE FROM sse_message_cache
    WHERE is_persisted = 1;
    SELECT ROW_COUNT() AS deleted_count;
END$$

DELIMITER ;
```

**调用示例**:
```sql
CALL clean_persisted_sse_cache();
```

---

## 表关系说明（V2.0更新）

新增关系：

5. **sys_user ↔ agent_provider**: 一对多关系
   - 一个用户可以配置多个智能体服务商
   - 通过 `agent_provider.user_id` 关联 `sys_user.id`
   - 每个用户有且仅有一个默认智能体

6. **agent_provider ↔ chat_session**: 一对多关系
   - 一个智能体配置可以被多个会话使用
   - 通过 `chat_session.agent_provider_id` 关联 `agent_provider.id`
   - 会话创建后不允许切换智能体

7. **chat_session ↔ sse_message_cache**: 一对多关系
   - 一个会话可以有多条SSE缓存消息
   - 通过 `sse_message_cache.session_id` 关联 `chat_session.id`
   - 缓存消息在持久化后会被清理

---

## 数据流程图（V2.0）

### 用户创建智能体配置
```
用户注册
  → 自动创建default智能体配置（trigger）
  → 设为默认（is_default=1）

用户添加外部智能体
  → 配置Coze/融汇等平台信息
  → 可选择设为默认（触发器自动取消旧默认）
```

### 会话与智能体关联
```
创建新会话
  → 指定或使用默认agent_provider_id
  → 发送首条消息到外部平台
  → 外部平台返回external_session_id
  → 保存到chat_session表

继续对话（多轮）
  → 读取chat_session.external_session_id
  → 携带该ID发送到外部平台
  → 实现上下文连续性
```

### SSE消息处理流程
```
用户发送消息
  → 后端转发到智能体平台
  → 接收SSE流
    → 逐条写入sse_message_cache表
    → 同时转发给前端
  → SSE流结束
    → 合并缓存消息
    → 持久化到chat_message表
    → 标记is_persisted=1
    → 清理缓存
```

---

## 变更记录（V2.0）

### 2025-01-03（多智能体平台接入）
- **新增表**：
  - `agent_provider`: 智能体服务商配置表
  - `sse_message_cache`: SSE消息缓存表
- **修改表**：
  - `chat_session`: 新增 `agent_provider_id` 和 `external_session_id` 字段
  - `chat_message`: 新增 `message_format` 和 `raw_content` 字段
- **新增触发器**：
  - `before_agent_provider_set_default`: 确保每个用户只有一个默认智能体
- **新增存储过程**：
  - `clean_persisted_sse_cache()`: 清理已持久化的SSE缓存
- **功能特性**：
  - 支持对接Coze、融汇等外部智能体平台
  - 支持多轮对话（通过external_session_id）
  - 支持SSE流式响应缓存和持久化
  - 支持用户独立配置和管理智能体
  - 支持设置默认智能体
  - 历史会话禁止切换智能体

### 2025-01-04（Coze平台Bot ID支持）
- **修改表**：
  - `agent_provider`: 新增 `bot_id` 字段（VARCHAR(100)），用于存储Coze平台的Bot ID
  - 新增索引 `idx_bot_id` 用于快速查询
- **业务逻辑**：
  - Coze平台类型的智能体配置必须填写bot_id字段
  - 其他平台类型（default、ronghui）bot_id可为空
  - 前端根据平台类型条件显示bot_id输入字段
  - 后端在创建和更新时验证Coze平台的bot_id必填
- **迁移脚本**：`database/migration_v2.1_add_bot_id.sql`
