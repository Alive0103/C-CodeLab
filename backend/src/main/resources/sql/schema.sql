-- C-CodeLab 数据库建表语句
-- 支持 MySQL/TiDB 和 H2 数据库

-- 1. 用户表 (user)
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名，唯一',
    `password_hash` VARCHAR(120) NOT NULL COMMENT '密码哈希值',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱地址',
    `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER' COMMENT '用户角色：ROLE_USER/ROLE_ANONYMOUS',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '账户是否启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 代码片段表 (code_snippet)
CREATE TABLE IF NOT EXISTS `code_snippet` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '片段唯一ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID，外键关联user表',
    `title` VARCHAR(128) NOT NULL COMMENT '代码片段标题',
    `code_content` LONGTEXT NOT NULL COMMENT '代码内容',
    `language` VARCHAR(16) NOT NULL DEFAULT 'c' COMMENT '编程语言',
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_title` (`title`),
    INDEX `idx_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_code_snippet_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码片段表';

-- 3. 执行记录表 (execution_record)
CREATE TABLE IF NOT EXISTS `execution_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录唯一ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID，外键关联user表',
    `title` VARCHAR(128) DEFAULT NULL COMMENT '执行标题',
    `code` LONGTEXT DEFAULT NULL COMMENT '执行的代码内容',
    `output` LONGTEXT DEFAULT NULL COMMENT '标准输出',
    `error` LONGTEXT DEFAULT NULL COMMENT '错误输出',
    `exit_code` INT DEFAULT NULL COMMENT '退出码：0=成功，非0=失败',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_created` (`user_id`, `created_at`),
    INDEX `idx_exit_code` (`exit_code`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_execution_record_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行记录表';

-- 4. 初始化数据（可选）
-- 插入匿名用户
INSERT IGNORE INTO `user` (`id`, `username`, `password_hash`, `role`, `enabled`) 
VALUES (1, 'Anonymous_Guest', '', 'ROLE_ANONYMOUS', TRUE);

-- 5. 创建索引优化查询性能
-- 用户表索引
CREATE INDEX IF NOT EXISTS `idx_user_role` ON `user`(`role`);
CREATE INDEX IF NOT EXISTS `idx_user_enabled` ON `user`(`enabled`);

-- 代码片段表索引
CREATE INDEX IF NOT EXISTS `idx_snippet_language` ON `code_snippet`(`language`);
CREATE INDEX IF NOT EXISTS `idx_snippet_public` ON `code_snippet`(`is_public`);
CREATE INDEX IF NOT EXISTS `idx_snippet_user_language` ON `code_snippet`(`user_id`, `language`);

-- 执行记录表索引
CREATE INDEX IF NOT EXISTS `idx_record_user_exit` ON `execution_record`(`user_id`, `exit_code`);
CREATE INDEX IF NOT EXISTS `idx_record_created_desc` ON `execution_record`(`created_at` DESC);

-- 6. 表注释和字段注释补充
ALTER TABLE `user` COMMENT = '用户表';
ALTER TABLE `code_snippet` COMMENT = '代码片段表';
ALTER TABLE `execution_record` COMMENT = '执行记录表';

-- 7. 分区表（可选，适用于大数据量场景）
-- 按创建时间分区执行记录表（MySQL 8.0+）
-- ALTER TABLE `execution_record` 
-- PARTITION BY RANGE (YEAR(created_at)) (
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );
