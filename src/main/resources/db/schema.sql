-- 浪漫点单系统数据库设计
-- 数据库名称: love_order_system

CREATE DATABASE IF NOT EXISTS love_order_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE love_order_system;

-- 1. 用户表（存储她的信息和偏好）
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `nickname` VARCHAR(50) NOT NULL DEFAULT '公主' COMMENT '昵称',
    `avatar_url` VARCHAR(255) COMMENT '头像URL',
    `theme_color` VARCHAR(20) DEFAULT '#FFB6C1' COMMENT '主题颜色',
    `birthday` DATE COMMENT '生日',
    `period_start_date` DATE COMMENT '生理期开始日期',
    `period_cycle_days` INT DEFAULT 28 COMMENT '生理期周期天数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 菜品分类表
CREATE TABLE `category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(50) COMMENT '图标emoji',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- 3. 菜品表
CREATE TABLE `dish` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜品ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
    `description` VARCHAR(500) COMMENT '菜品描述',
    `image_url` VARCHAR(1000) COMMENT '图片URL',
    `price_type` VARCHAR(20) DEFAULT '爱心币' COMMENT '价格类型：爱心币/抱抱/亲亲等',
    `price` INT DEFAULT 1 COMMENT '价格数量',
    `is_signature` TINYINT DEFAULT 0 COMMENT '是否拿手菜',
    `is_new` TINYINT DEFAULT 0 COMMENT '是否新品',
    `is_recommended` TINYINT DEFAULT 0 COMMENT '是否今日推荐',
    `is_sold_out` TINYINT DEFAULT 0 COMMENT '是否售罄',
    `tags` VARCHAR(200) COMMENT '标签，逗号分隔',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (`category_id`),
    INDEX idx_recommended (`is_recommended`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- 4. 订单表
CREATE TABLE `orders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_price` INT DEFAULT 0 COMMENT '总价格',
    `price_type` VARCHAR(20) DEFAULT '爱心币' COMMENT '价格类型',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/received/cooking/delivered/completed',
    `remark` VARCHAR(500) COMMENT '备注',
    `special_message` VARCHAR(500) COMMENT '特殊消息（如生日祝福）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (`user_id`),
    INDEX idx_created (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 5. 订单明细表
CREATE TABLE `order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
    `dish_name` VARCHAR(100) NOT NULL COMMENT '菜品名称（冗余）',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `price` INT NOT NULL DEFAULT 1 COMMENT '单价',
    `remark` VARCHAR(200) COMMENT '备注（少冰、不放香菜等）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 6. 成就表
CREATE TABLE `achievement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成就ID',
    `name` VARCHAR(100) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(500) COMMENT '成就描述',
    `icon` VARCHAR(50) COMMENT '图标emoji',
    `condition_type` VARCHAR(50) COMMENT '条件类型：consecutive_days/try_all_dishes/order_count/special_dish',
    `condition_value` INT COMMENT '条件值',
    `reward` VARCHAR(200) COMMENT '奖励描述',
    `reward_coins` INT DEFAULT 0 COMMENT '奖励爱心币数量',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就表';

-- 7. 用户成就表
CREATE TABLE `user_achievement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `achievement_id` BIGINT NOT NULL COMMENT '成就ID',
    `unlocked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
    `is_unlocked` TINYINT DEFAULT 0 COMMENT '是否已解锁',
    UNIQUE KEY uk_user_achievement (`user_id`, `achievement_id`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就表';

-- 8. 统计数据表
CREATE TABLE `statistics` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `order_count` INT DEFAULT 0 COMMENT '订单数',
    `favorite_dish_id` BIGINT COMMENT '最爱菜品ID',
    `favorite_time_slot` VARCHAR(20) COMMENT '最爱时间段：morning/afternoon/evening/night',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (`user_id`, `stat_date`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计表';

-- 9. 爱心币余额表
CREATE TABLE `coin_balance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `balance` INT DEFAULT 0 COMMENT '当前余额',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user (`user_id`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爱心币余额表';

-- 10. 爱心币流水表
CREATE TABLE `coin_transaction` (
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

-- 11. 菜品收藏表
CREATE TABLE `dish_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_dish (`user_id`, `dish_id`),
    INDEX idx_user (`user_id`),
    INDEX idx_dish (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品收藏表';

-- 12. 纪念日表
CREATE TABLE `anniversary` (
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
CREATE TABLE `todo_item` (
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

-- 14. 每日签到记录表
CREATE TABLE `daily_checkin` (
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

-- 15. 转盘奖品配置表
CREATE TABLE `lottery_prize` (
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
CREATE TABLE `lottery_record` (
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

-- 插入初始数据

-- 初始用户
INSERT INTO `user` (`nickname`, `theme_color`, `birthday`) VALUES ('公主', '#FFB6C1', '2000-01-01');

-- 初始爱心币余额（用户ID为1）
INSERT INTO `coin_balance` (`user_id`, `balance`) VALUES (1, 100);

-- 菜品分类
INSERT INTO `category` (`name`, `icon`, `sort_order`) VALUES
('中餐', '🍜', 1),
('西餐', '🍝', 2),
('甜品', '🍰', 3),
('饮品', '🥤', 4),
('水果', '🍎', 5),
('零食', '🍪', 6);

-- 示例菜品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(1, '红烧肉', '肥而不腻，入口即化', '爱心币', 3, 1, 1, '拿手菜,经典'),
(1, '糖醋排骨', '酸甜可口，外酥里嫩', '抱抱', 2, 1, 0, '拿手菜'),
(1, '可乐鸡翅', '甜香入味，小朋友最爱', '亲亲', 2, 0, 1, '新品,推荐'),
(2, '意大利面', '番茄肉酱，浓郁美味', '爱心币', 2, 0, 0, ''),
(2, '牛排', '七分熟，配黑椒汁', '抱抱', 5, 1, 0, '拿手菜,大餐'),
(3, '提拉米苏', '甜蜜浪漫，入口即化', '亲亲', 3, 1, 1, '拿手菜,推荐'),
(3, '芒果布丁', 'Q弹爽滑，清新香甜', '爱心币', 1, 0, 0, ''),
(4, '珍珠奶茶', '半糖去冰，加波霸', '抱抱', 1, 0, 1, '推荐'),
(4, '红糖姜茶', '暖胃暖心，女生必备', '爱心币', 1, 0, 0, '养生,热饮'),
(5, '草莓拼盘', '新鲜草莓，甜蜜多汁', '亲亲', 2, 0, 0, ''),
(5, '西瓜切块', '清凉解暑，夏日必备', '爱心币', 1, 0, 0, '');

-- 成就
INSERT INTO `achievement` (`name`, `description`, `icon`, `condition_type`, `condition_value`, `reward`, `reward_coins`) VALUES
('初来乍到', '完成第一次点单', '🎉', 'order_count', 1, '解锁粉色主题', 10),
('吃货达人', '累计点单10次', '🍴', 'order_count', 10, '解锁专属称号', 30),
('连续宠爱的第7天', '连续7天点单', '💕', 'consecutive_days', 7, '解锁特殊皮肤', 50),
('甜品控', '尝试所有甜品', '🍰', 'try_all_dishes', 0, '解锁甜品师称号', 40),
('宠妻狂魔', '累计点单50次', '👑', 'order_count', 50, '解锁最高荣誉', 100);

-- 转盘奖品配置（总概率100%）
INSERT INTO `lottery_prize` (`name`, `description`, `icon`, `prize_type`, `value`, `probability`, `sort_order`) VALUES
('特等奖：100爱心币', '运气爆棚！获得100个爱心币', '🏆', 'coins', 100, 2.00, 1),
('一等奖：50爱心币', '太棒了！获得50个爱心币', '🥇', 'coins', 50, 5.00, 2),
('二等奖：30爱心币', '恭喜！获得30个爱心币', '🥈', 'coins', 30, 10.00, 3),
('三等奖：20爱心币', '不错哦！获得20个爱心币', '🥉', 'coins', 20, 15.00, 4),
('小奖：10爱心币', '加油！获得10个爱心币', '🎁', 'coins', 10, 25.00, 5),
('安慰奖：5爱心币', '继续努力！获得5个爱心币', '💝', 'coins', 5, 28.00, 6),
('谢谢惠顾', '很遗憾，下次再来吧', '😊', 'thanks', 0, 15.00, 7);
