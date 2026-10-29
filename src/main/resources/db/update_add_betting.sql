-- 幸运币押注功能 - 数据库更新脚本
-- 执行此脚本以添加押注相关表

USE love_order_system;

-- 17. 幸运币押注记录表
CREATE TABLE IF NOT EXISTS `betting_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `bet_amount` INT NOT NULL COMMENT '下注金额',
    `bet_type` VARCHAR(10) NOT NULL COMMENT '押注类型：big/small',
    `dice_result` INT NOT NULL COMMENT '骰子结果（1-6）',
    `result_type` VARCHAR(10) NOT NULL COMMENT '结果类型：big/small',
    `is_win` TINYINT NOT NULL COMMENT '是否获胜：0-失败，1-胜利',
    `reward_amount` INT DEFAULT 0 COMMENT '奖励金额（获胜时）',
    `deduct_amount` INT NOT NULL COMMENT '扣除金额',
    `is_half_deduct` TINYINT DEFAULT 0 COMMENT '是否半扣：0-正常扣，1-只扣一半',
    `consecutive_loss` INT DEFAULT 0 COMMENT '连续输的次数',
    `daily_total_loss` INT DEFAULT 0 COMMENT '当日累计损失',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '押注时间',
    INDEX idx_user (`user_id`),
    INDEX idx_created (`created_at`),
    INDEX idx_date (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幸运币押注记录表';

-- 18. 用户每日押注统计表
CREATE TABLE IF NOT EXISTS `user_daily_betting_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `bet_date` DATE NOT NULL COMMENT '押注日期',
    `total_bets` INT DEFAULT 0 COMMENT '总押注次数',
    `win_count` INT DEFAULT 0 COMMENT '获胜次数',
    `loss_count` INT DEFAULT 0 COMMENT '失败次数',
    `total_bet_amount` INT DEFAULT 0 COMMENT '总下注金额',
    `total_reward` INT DEFAULT 0 COMMENT '总奖励金额',
    `total_loss` INT DEFAULT 0 COMMENT '总损失金额',
    `consecutive_loss` INT DEFAULT 0 COMMENT '当前连续输的次数',
    `is_locked` TINYINT DEFAULT 0 COMMENT '是否被锁定：0-未锁定，1-已锁定',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (`user_id`, `bet_date`),
    INDEX idx_user (`user_id`),
    INDEX idx_date (`bet_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日押注统计表';
