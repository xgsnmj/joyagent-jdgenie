-- ============================================
-- 华创证券智能体底座 - 数据库初始化脚本
-- ============================================
-- 数据库类型: MySQL 8.0+
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 系统用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
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
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 2. 聊天会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话记录ID（主键）',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话唯一标识符（UUID）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `agent_type` VARCHAR(50) DEFAULT NULL COMMENT 'Agent工作类型（plansolve/react/router/workflow/comprehensive）',
  `agent_provider_id` BIGINT DEFAULT NULL COMMENT '关联的智能体服务商ID',
  `external_session_id` VARCHAR(200) DEFAULT NULL COMMENT '外部平台返回的会话ID（用于多轮对话）',
  `output_style` VARCHAR(50) DEFAULT NULL COMMENT '输出样式',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`),
  KEY `idx_agent_provider` (`agent_provider_id`),
  KEY `idx_external_session` (`external_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ============================================
-- 3. 聊天消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID（主键）',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '消息角色（user/assistant/system）',
  `content` TEXT COMMENT '消息内容',
  `files` TEXT COMMENT '附件文件信息（JSON格式）',
  `thought` TEXT COMMENT '思考过程（AI的思维链）',
  `tasks` LONGTEXT COMMENT '任务详情（JSON数组，包含所有任务执行过程）',
  `plan` TEXT COMMENT '计划信息（JSON对象，包含计划标题和步骤）',
  `metadata` LONGTEXT COMMENT '其他元数据（JSON对象，存储额外信息）',
  `message_format` VARCHAR(50) DEFAULT 'default' COMMENT '消息格式类型（default/coze/ronghui/tongyi）',
  `raw_content` TEXT COMMENT '原始响应内容（JSON格式，用于调试）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ============================================
-- 4. 数据模型信息表
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_model_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '模型编码',
  `type` VARCHAR(10) NOT NULL COMMENT '模型类型（TABLE/SQL）',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `content` TEXT NOT NULL COMMENT '模型内容（表名或SQL）',
  `use_prompt` TEXT COMMENT '模型使用说明',
  `business_prompt` TEXT COMMENT '模型业务限定提示词',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据模型表信息';

-- ============================================
-- 5. 数据模型字段表
-- ============================================
CREATE TABLE IF NOT EXISTS `chat_model_schema` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_code` VARCHAR(200) NOT NULL COMMENT '模型编码',
  `column_id` VARCHAR(1000) NOT NULL COMMENT '字段唯一ID',
  `column_name` VARCHAR(200) NOT NULL COMMENT '字段中文名',
  `column_comment` VARCHAR(1000) NOT NULL COMMENT '字段描述',
  `few_shot` TEXT COMMENT '值枚举（逗号分隔）',
  `data_type` VARCHAR(20) DEFAULT NULL COMMENT '字段值类型',
  `synonyms` VARCHAR(300) DEFAULT NULL COMMENT '同义词',
  `vector_uuid` VARCHAR(400) DEFAULT NULL COMMENT '向量库数据ID',
  `default_recall` TINYINT DEFAULT 0 COMMENT '默认召回',
  `analyze_suggest` TINYINT DEFAULT 0 COMMENT '分析建议（0-可选，-1-禁止，1-建议）',
  `yn` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_model_code` (`model_code`),
  KEY `idx_column_name` (`column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据模型字段信息表';

-- ============================================
-- 6. 销售数据表（示例数据）
-- ============================================
CREATE TABLE IF NOT EXISTS `sales_data` (
  `row_id` INT PRIMARY KEY COMMENT '行ID',
  `order_id` VARCHAR(50) DEFAULT NULL COMMENT '订单ID',
  `order_date` DATE COMMENT '订单日期',
  `ship_date` DATE COMMENT '发货日期',
  `ship_mode` VARCHAR(50) DEFAULT NULL COMMENT '邮寄方式',
  `customer_id` VARCHAR(50) DEFAULT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
  `segment` VARCHAR(50) DEFAULT NULL COMMENT '细分',
  `city` VARCHAR(100) DEFAULT NULL COMMENT '城市',
  `state_province` VARCHAR(100) DEFAULT NULL COMMENT '省/自治区',
  `country` VARCHAR(100) DEFAULT NULL COMMENT '国家',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
  `product_id` VARCHAR(50) DEFAULT NULL COMMENT '产品ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '产品类别',
  `sub_category` VARCHAR(50) DEFAULT NULL COMMENT '产品子类别',
  `product_name` VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
  `sales` DECIMAL(10, 4) DEFAULT NULL COMMENT '销售额',
  `quantity` INT DEFAULT NULL COMMENT '销售数量',
  `discount` DECIMAL(10, 4) DEFAULT NULL COMMENT '折扣',
  `profit` DECIMAL(10, 4) DEFAULT NULL COMMENT '利润'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售数据表';

-- ============================================
-- 7. 文件信息表
-- ============================================
CREATE TABLE IF NOT EXISTS `file_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件记录ID（主键）',
  `file_id` VARCHAR(64) NOT NULL COMMENT '文件唯一标识符（MD5哈希值）',
  `filename` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `description` TEXT COMMENT '文件描述',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `status` TINYINT DEFAULT 0 COMMENT '文件状态（0-正常，1-已删除）',
  `request_id` VARCHAR(200) DEFAULT NULL COMMENT '请求ID/会话ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_id` (`file_id`),
  KEY `idx_request_id` (`request_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- ============================================
-- 8. 智能体服务商配置表
-- ============================================
CREATE TABLE IF NOT EXISTS `agent_provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `creator_id` BIGINT NOT NULL COMMENT '创建者用户ID',
  `provider_type` VARCHAR(50) NOT NULL COMMENT '平台类型（default/coze/ronghui/tongyi/dify）',
  `provider_name` VARCHAR(10) NOT NULL COMMENT '应用名称（最多10个字）',
  `description` TEXT COMMENT '智能体简介（最多500字符）',
  `icon` VARCHAR(500) COMMENT '智能体图标URL',
  `category` VARCHAR(50) COMMENT '分类标签（教育/新零售/消费等）',
  `api_endpoint` VARCHAR(500) COMMENT 'API请求地址（default类型为空）',
  `api_key` VARCHAR(500) COMMENT 'API密钥（明文存储，default类型为空）',
  `bot_id` VARCHAR(100) COMMENT 'Bot ID（Coze/通义点金使用）',
  `workspace_id` VARCHAR(50) COMMENT '工作空间ID（通义点金使用）',
  `tenant_id` VARCHAR(50) COMMENT '租户ID（融汇使用）',
  `login_user_id` VARCHAR(50) COMMENT '登录用户ID（融汇使用）',
  `login_dept_id` VARCHAR(50) COMMENT '登录部门ID（融汇使用）',
  `login_username` VARCHAR(50) COMMENT '登录用户名（融汇使用）',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为该用户的默认智能体（0-否，1-是）',
  `is_public` TINYINT(1) DEFAULT 1 COMMENT '是否公开（1-公开，0-私有）',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数统计',
  `extra_config` JSON COMMENT '额外配置（JSON格式，存储平台特有参数）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_provider_type` (`provider_type`),
  KEY `idx_bot_id` (`bot_id`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_category` (`category`),
  UNIQUE KEY `uk_creator_provider_name` (`creator_id`, `provider_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体服务商配置表';

-- ============================================
-- 9. SSE消息缓存表
-- ============================================
CREATE TABLE IF NOT EXISTS `sse_message_cache` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSE消息缓存表（临时存储SSE流，待流结束后统一持久化）';

-- ============================================
-- 触发器：确保每个用户只有一个默认智能体
-- ============================================
DELIMITER $$

DROP TRIGGER IF EXISTS before_agent_provider_set_default$$

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

-- ============================================
-- 初始化数据：创建默认管理员账号
-- ============================================
-- 密码: admin123 (BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`, `is_admin`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@huachuang.com', 0, 1)
ON DUPLICATE KEY UPDATE id=id;

-- ============================================
-- 完成初始化
-- ============================================
SET FOREIGN_KEY_CHECKS = 1;

-- 显示成功信息
SELECT '数据库初始化完成！' AS message;
SELECT '默认管理员账号: admin' AS info;
SELECT '默认管理员密码: admin123' AS info;
SELECT '请在生产环境中务必修改默认密码！' AS warning;
