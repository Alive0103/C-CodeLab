-- C-CodeLab 初始化数据
-- 注意：生产环境请修改默认密码和配置

-- 1. 插入系统用户
INSERT IGNORE INTO `user` (`id`, `username`, `password_hash`, `email`, `role`, `enabled`) 
VALUES 
(1, 'Anonymous_Guest', '', '', 'ROLE_ANONYMOUS', TRUE),
(2, 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/8KzKz2K', 'admin@codelab.com', 'ROLE_USER', TRUE);

-- 2. 插入示例代码片段（可选）
INSERT IGNORE INTO `code_snippet` (`id`, `user_id`, `title`, `code_content`, `language`, `is_public`, `created_at`) 
VALUES 
(1, 2, 'Hello World', '#include <stdio.h>\nint main() {\n    printf("Hello, World!\\n");\n    return 0;\n}', 'c', TRUE, NOW()),
(2, 2, '计算器', '#include <stdio.h>\nint main() {\n    int a, b;\n    printf("请输入两个数字: ");\n    scanf("%d %d", &a, &b);\n    printf("和: %d\\n", a + b);\n    return 0;\n}', 'c', TRUE, NOW());

-- 3. 插入示例执行记录（可选）
INSERT IGNORE INTO `execution_record` (`id`, `user_id`, `title`, `code`, `output`, `error`, `exit_code`, `created_at`) 
VALUES 
(1, 2, 'Hello World', '#include <stdio.h>\nint main() {\n    printf("Hello, World!\\n");\n    return 0;\n}', 'Hello, World!\n', '', 0, NOW()),
(2, 2, '编译错误示例', '#include <stdio.h>\nint main() {\n    printf("Hello, World!\\n"  // 缺少右括号\n    return 0;\n}', '', 'test.c:3:5: error: expected \')\' before \'return\'\n    3 |     return 0;\n      |     ^~~~~~', 1, NOW());
