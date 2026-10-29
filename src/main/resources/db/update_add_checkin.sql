-- 添加每日签到功能
USE love_order_system;

-- 创建每日签到记录表
CREATE TABLE IF NOT EXISTS `daily_checkin` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '签到ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `checkin_date` DATE NOT NULL COMMENT '签到日期',
    `continuous_days` INT DEFAULT 1 COMMENT '连续签到天数',
    `coins_reward` INT DEFAULT 5 COMMENT '获得爱心币奖励',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date (`user_id`, `checkin_date`),
    INDEX idx_user (`user_id`),
    INDEX idx_date (`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录表';
