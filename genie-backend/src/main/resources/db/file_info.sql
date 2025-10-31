-- =====================================================
-- 文件信息表
-- 用于存储genie-tool上传的文件元数据
-- 作者: Claude
-- 日期: 2025-10-29
-- =====================================================

-- 删除已存在的表（仅开发环境使用，生产环境请注释此行）
-- DROP TABLE IF EXISTS `file_info`;

CREATE TABLE IF NOT EXISTS `file_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件记录ID（主键）',
  `file_id` VARCHAR(64) NOT NULL COMMENT '文件唯一标识符（MD5哈希值）',
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

-- =====================================================
-- 说明
-- =====================================================
-- 1. file_id: 通过 MD5(request_id + filename) 生成
-- 2. file_path: 实际文件存储路径，已清理Windows非法字符
-- 3. request_id: 关联到chat_session.session_id或单独的请求ID
-- 4. status: 软删除标识，0=正常，1=已删除
-- 5. 索引:
--    - uk_file_id: 文件ID唯一索引
--    - idx_request_id: 请求ID索引，用于查询某个会话的所有文件
--    - idx_create_time: 创建时间索引，用于按时间排序
-- =====================================================
