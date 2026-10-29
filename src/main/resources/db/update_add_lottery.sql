-- 添加转盘抽奖功能
USE love_order_system;

-- 15. 转盘奖品配置表
CREATE TABLE IF NOT EXISTS `lottery_prize` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '奖品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '奖品名称',
    `description` VARCHAR(500) COMMENT '奖品描述',
    `icon` VARCHAR(50) COMMENT '图标emoji',
    `prize_type` VARCHAR(20) DEFAULT 'coins' COMMENT '奖品类型：coins/blank/thanks',
    `value` INT DEFAULT 0 COMMENT '奖品值（爱心币数量或谢谢惠顾）',
    `probability` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '中奖概率（百分比）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转盘奖品配置表';

-- 16. 抽奖记录表
CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `prize_id` BIGINT NOT NULL COMMENT '奖品ID',
    `prize_name` VARCHAR(100) NOT NULL COMMENT '奖品名称（冗余）',
    `prize_type` VARCHAR(20) NOT NULL COMMENT '奖品类型',
    `prize_value` INT DEFAULT 0 COMMENT '奖品值',
    `cost_coins` INT DEFAULT 10 COMMENT '消耗爱心币数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
    INDEX idx_user (`user_id`),
    INDEX idx_created (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖记录表';

-- 插入初始奖品配置（总概率100%）
INSERT INTO `lottery_prize` (`name`, `description`, `icon`, `prize_type`, `value`, `probability`, `sort_order`) VALUES
('特等奖：100爱心币', '运气爆棚！获得100个爱心币', '🏆', 'coins', 100, 2.00, 1),
('一等奖：50爱心币', '太棒了！获得50个爱心币', '🥇', 'coins', 50, 5.00, 2),
('二等奖：30爱心币', '恭喜！获得30个爱心币', '🥈', 'coins', 30, 10.00, 3),
('三等奖：20爱心币', '不错哦！获得20个爱心币', '🥉', 'coins', 20, 15.00, 4),
('小奖：10爱心币', '加油！获得10个爱心币', '🎁', 'coins', 10, 25.00, 5),
('安慰奖：5爱心币', '继续努力！获得5个爱心币', '💝', 'coins', 5, 28.00, 6),
('谢谢惠顾', '很遗憾，下次再来吧', '😊', 'thanks', 0, 15.00, 7);
