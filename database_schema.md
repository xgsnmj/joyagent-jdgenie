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

**注意**: 上述SQL中的密码已经过BCrypt加密，明文密码为 `admin123`。生产环境请务必修改默认密码。
