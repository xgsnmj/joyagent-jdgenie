# 数据库结构差异报告

生成时间: 2025-01-14

## 概述

本报告对比了实际数据库结构与文档(`database_schema.md`)中的描述,发现以下差异需要修正。

---

## 1. sys_user 表

### 差异1: email字段索引类型不一致

**文档描述**:
```sql
UNIQUE KEY `uk_email` (`email`)  -- 唯一索引
```

**实际结构**:
```sql
KEY `idx_email` (`email`)  -- 普通索引，不是唯一索引
```

**差异说明**:
- 文档声称email有唯一约束,但实际数据库中没有
- 这可能导致数据重复问题

**建议**: 需要确认业务需求,如果email应该唯一,需要在数据库中添加唯一索引

### 差异2: email字段NULL约束不一致

**文档描述**:
```sql
`email` VARCHAR(100) NOT NULL COMMENT '电子邮箱（必填，唯一，支持用户名/邮箱登录）'
```

**实际结构**:
```sql
`email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱'
```

**差异说明**:
- 文档说email是必填(NOT NULL),但实际允许NULL
- 字段注释也不一致

---

## 2. agent_provider 表

### 差异1: login_username字段长度不一致

**文档描述**:
```sql
`login_username` VARCHAR(100) COMMENT '登录用户名（仅ronghui类型使用）'
```

**实际结构**:
```sql
`login_username` VARCHAR(50) COMMENT '登录用户名（仅ronghui类型使用）'
```

**差异说明**: 文档中是100字符,实际数据库只有50字符

### 差异2: 唯一索引名称不一致

**文档描述**:
```sql
UNIQUE KEY `uk_user_provider_name` (`user_id`, `provider_name`)
```

**实际结构**:
```sql
UNIQUE KEY `uk_creator_provider_name` (`creator_id`, `provider_name`)
```

**差异说明**:
- 文档中索引基于user_id,实际基于creator_id
- 这是由于2025-01-05的智能体社区功能升级导致的

---

## 3. file_info 表

### 差异: description字段类型不一致

**文档描述**:
```sql
`description` VARCHAR(1000) DEFAULT NULL COMMENT '文件描述'
```

**实际结构**:
```sql
`description` TEXT COMMENT '文件描述'
```

**差异说明**: 文档中是VARCHAR(1000),实际是TEXT类型,可以存储更长内容

---

## 4. sse_message_cache 表（⚠️ 重要）

### 问题: 文档中声称已删除，但实际仍然存在

**文档描述**（变更记录2025-01-04）:
```
删除表:
  - `sse_message_cache`: SSE消息缓存表（已废弃）
删除原因:
  - 持久化逻辑未实现（TODO状态），表未发挥实际作用
  - 新架构采用拦截器 + ConversationDataCollector 直接收集数据
  - 不再需要中间缓存层，简化系统架构
```

**实际状态**: 该表仍然存在于数据库中!

**实际表结构**:
```sql
CREATE TABLE `sse_message_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `message_sequence` INT NOT NULL COMMENT '消息序号（从0开始递增）',
  `event_type` VARCHAR(50) DEFAULT NULL COMMENT 'SSE事件类型（如message、error、done）',
  `event_data` MEDIUMTEXT COMMENT '事件数据内容',
  `raw_data` MEDIUMTEXT COMMENT '原始数据（完整SSE消息）',
  `is_persisted` TINYINT(1) DEFAULT 0 COMMENT '是否已持久化到chat_message表（0-否 1-是）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `message_sequence`),
  KEY `idx_persisted` (`is_persisted`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='SSE消息缓存表（临时存储SSE流，待流结束后统一持久化）';
```

**差异说明**:
- 文档中明确记录该表已删除,但实际并未删除
- 需要确认:
  1. 该表是否仍在使用?
  2. 如果不使用,是否应该删除?
  3. 如果仍在使用,需要在文档中补充说明

---

## 5. chat_message 表

### 差异: 实际数据库缺少raw_content字段

**文档描述**:
```sql
`raw_content` TEXT DEFAULT NULL COMMENT '原始响应内容（JSON格式，用于调试）'
```

**实际结构**: 该字段不存在!

**差异说明**:
- 文档的V2.0更新记录中提到新增了raw_content字段
- 但实际数据库中并不存在这个字段
- 可能是迁移脚本未执行

---

## 差异统计

| 表名 | 字段名 | 差异类型 | 严重程度 |
|------|--------|----------|----------|
| sys_user | email | 索引类型(文档:唯一, 实际:普通) | 高 |
| sys_user | email | NULL约束(文档:NOT NULL, 实际:NULL) | 高 |
| agent_provider | login_username | 长度(文档:100, 实际:50) | 中 |
| agent_provider | 唯一索引 | 索引名称和字段不同 | 中 |
| file_info | description | 类型(文档:VARCHAR, 实际:TEXT) | 低 |
| sse_message_cache | 整个表 | 文档说已删除但实际存在 | 高 |
| chat_message | raw_content | 文档说新增但实际不存在 | 中 |

---

## 建议修正方案

### 方案1: 以实际数据库为准(推荐)

**优点**:
- 不会影响现有数据和运行的系统
- 修改量小,只需更新文档

**缺点**:
- 某些功能可能不完整(如email不唯一可能导致问题)

**操作**:
1. 更新 `database_schema.md` 文档,以实际结构为准
2. 为 `sse_message_cache` 表补充完整文档
3. 删除 `chat_message.raw_content` 字段的描述
4. 修正所有差异项的文档描述

### 方案2: 以文档为准,修改数据库

**优点**:
- 实现文档设计的完整功能
- 保证email唯一性等业务约束

**缺点**:
- 需要执行数据库迁移,有风险
- 需要检查现有数据是否有冲突(如重复email)

**操作**:
1. 为sys_user.email添加唯一索引(需先检查重复数据)
2. 删除 sse_message_cache 表(需确认无代码引用)
3. 为 chat_message 添加 raw_content 字段
4. 统一 agent_provider 的字段定义

---

## 推荐行动

**立即执行**:
1. 采用方案1,更新文档以匹配实际数据库
2. 补充 sse_message_cache 表的完整文档说明
3. 标记差异项,待后续版本修正

**后续版本考虑**:
1. 评估是否需要email唯一约束
2. 评估是否需要raw_content字段
3. 决定sse_message_cache表的去留
