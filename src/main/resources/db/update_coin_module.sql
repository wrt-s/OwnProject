-- 爱心币管理模块 - 数据库更新脚本
-- 执行此脚本以添加爱心币相关表

USE love_order_system;

-- 9. 爱心币余额表
CREATE TABLE IF NOT EXISTS `coin_balance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `balance` INT DEFAULT 0 COMMENT '当前余额',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user (`user_id`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爱心币余额表';

-- 10. 爱心币流水表
CREATE TABLE IF NOT EXISTS `coin_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` INT NOT NULL COMMENT '变动数量（正数为增加，负数为减少）',
    `balance_after` INT NOT NULL COMMENT '变动后余额',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：add_坏坏/use_order/use_task',
    `description` VARCHAR(500) COMMENT '描述',
    `related_id` BIGINT COMMENT '关联ID（订单ID或任务记录ID）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (`user_id`),
    INDEX idx_created (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爱心币流水表';

-- 为现有用户初始化爱心币余额（如果不存在）
INSERT INTO `coin_balance` (`user_id`, `balance`) 
SELECT id, 100 FROM `user` 
WHERE id NOT IN (SELECT user_id FROM coin_balance)
LIMIT 1;
