-- 数据库迁移脚本：为 email 字段添加唯一约束
-- 执行前请先检查是否有重复的邮箱数据

-- 1. 检查是否有重复的邮箱（NULL 值除外）
-- SELECT email, COUNT(*) as count 
-- FROM `user` 
-- WHERE email IS NOT NULL AND email != ''
-- GROUP BY email 
-- HAVING COUNT(*) > 1;

-- 2. 如果有重复的邮箱，需要先清理（保留 id 最小的记录，删除其他重复记录）
-- 注意：执行前请备份数据库！
-- DELETE u1 FROM `user` u1
-- INNER JOIN `user` u2 
-- WHERE u1.id > u2.id 
-- AND u1.email IS NOT NULL 
-- AND u1.email != '' 
-- AND u1.email = u2.email;

-- 3. 删除可能存在的旧索引（如果存在）
-- ALTER TABLE `user` DROP INDEX IF EXISTS `idx_email`;

-- 4. 为 email 字段添加唯一约束
-- 注意：如果 email 字段允许 NULL，MySQL 允许多个 NULL 值，但非 NULL 值必须唯一
ALTER TABLE `user` 
ADD UNIQUE INDEX `idx_email_unique` (`email`);

-- 5. 验证唯一约束是否创建成功
-- SHOW INDEX FROM `user` WHERE Column_name = 'email';

