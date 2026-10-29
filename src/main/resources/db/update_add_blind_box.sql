-- 盲盒点单功能 - 数据库更新脚本
-- 包含：用户不喜欢菜品表

USE love_order_system;

-- 1. 用户不喜欢菜品表
CREATE TABLE IF NOT EXISTS `dish_dislike` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    UNIQUE KEY uk_user_dish (`user_id`, `dish_id`),
    INDEX idx_user (`user_id`),
    INDEX idx_dish (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户不喜欢菜品表';

-- 2. 为dish表添加盲盒相关字段（可选，用于统计）
ALTER TABLE `dish` ADD COLUMN IF NOT EXISTS `blind_box_count` INT DEFAULT 0 COMMENT '被盲盒抽中次数';
