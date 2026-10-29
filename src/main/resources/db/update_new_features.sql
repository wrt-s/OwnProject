-- 新增功能模块 - 数据库更新脚本
-- 包含：菜品收藏、纪念日、家庭待办事项

USE love_order_system;

-- 11. 菜品收藏表
CREATE TABLE IF NOT EXISTS `dish_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_dish (`user_id`, `dish_id`),
    INDEX idx_user (`user_id`),
    INDEX idx_dish (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品收藏表';

-- 12. 纪念日表
CREATE TABLE IF NOT EXISTS `anniversary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '纪念日ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '纪念日名称',
    `date` DATE NOT NULL COMMENT '日期',
    `type` VARCHAR(20) DEFAULT 'custom' COMMENT '类型：birthday/first_date/wedding/custom',
    `description` VARCHAR(500) COMMENT '描述',
    `reminder_days` INT DEFAULT 3 COMMENT '提前提醒天数',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (`user_id`),
    INDEX idx_date (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纪念日表';

-- 13. 家庭待办事项表
CREATE TABLE IF NOT EXISTS `todo_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '待办ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` VARCHAR(1000) COMMENT '详细描述',
    `priority` VARCHAR(10) DEFAULT 'medium' COMMENT '优先级：high/medium/low',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/in_progress/completed',
    `assignee` VARCHAR(50) COMMENT '负责人：布布/佳宝/一起',
    `due_date` DATE COMMENT '截止日期',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (`user_id`),
    INDEX idx_status (`status`),
    INDEX idx_due_date (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭待办事项表';

-- 插入示例纪念日数据
INSERT INTO `anniversary` (`user_id`, `name`, `date`, `type`, `description`, `reminder_days`) VALUES
(1, '我们的纪念日', '2024-05-20', 'first_date', '第一次在一起的日子', 7),
(1, '佳宝的生日', '2000-01-01', 'birthday', '佳宝的生日', 7);

-- 插入示例待办事项
INSERT INTO `todo_item` (`user_id`, `title`, `description`, `priority`, `status`, `assignee`, `due_date`) VALUES
(1, '买牛奶', '去超市买纯牛奶', 'medium', 'pending', '布布', '2026-05-15'),
(1, '打扫卫生', '打扫客厅和卧室', 'high', 'pending', '一起', '2026-05-14'),
(1, '看电影', '看最新的爱情电影', 'low', 'pending', '佳宝', '2026-05-20');
