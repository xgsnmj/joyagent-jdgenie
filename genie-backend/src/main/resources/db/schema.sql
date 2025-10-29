CREATE TABLE `chat_model_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '模型编码',
  `type` VARCHAR(10) NOT NULL COMMENT '模型类型TABLE,SQL',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `content` TEXT NOT NULL COMMENT '模型内容，表或者sql',
  `use_prompt` TEXT COMMENT '模型使用说明',
  `business_prompt` TEXT COMMENT '模型业务限定提示词',
  `yn` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据模型表信息';


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
  `default_recall` TINYINT NOT NULL DEFAULT 0 COMMENT '默认召回',
  `analyze_suggest` TINYINT NOT NULL DEFAULT 0 COMMENT '分析建议0可选，-1禁止用于分析维度，1建议',
  `yn` TINYINT NOT NULL DEFAULT 1 COMMENT '是否有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据模型表信息';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售数据表';

-- 新增用户表
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-停用 2-禁用',
  `is_admin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否管理员：0-否 1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `yn` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除：0-已删除 1-正常',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员（密码：admin123，使用BCrypt加密）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `is_admin`, `status`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, 0);

-- 新增会话历史表
CREATE TABLE `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（UUID）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `agent_type` VARCHAR(50) DEFAULT NULL COMMENT '智能体类型',
  `output_style` VARCHAR(50) DEFAULT NULL COMMENT '输出样式',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `yn` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除：0-已删除 1-正常',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话历史表';

-- 新增会话消息表
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
  `content` LONGTEXT NOT NULL COMMENT '消息内容',
  `files` TEXT DEFAULT NULL COMMENT '关联文件（JSON格式）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `yn` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除：0-已删除 1-正常',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话消息表';