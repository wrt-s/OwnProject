-- 记事本表 - 一二的小本本
CREATE TABLE `notebook` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '笔记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `category` VARCHAR(50) DEFAULT 'default' COMMENT '分类标签',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (`user_id`),
    INDEX idx_category (`category`),
    INDEX idx_pinned (`is_pinned`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记事本表 - 一二的小本本';
