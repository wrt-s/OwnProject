-- 盲盒抽取历史记录表
USE love_order_system;

CREATE TABLE IF NOT EXISTS `blind_box_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dish_id` BIGINT NOT NULL COMMENT '抽中菜品ID',
    `dish_name` VARCHAR(128) DEFAULT NULL COMMENT '菜品名称冗余',
    `dish_price` INT DEFAULT NULL COMMENT '菜品价格冗余',
    `cost_coins` INT DEFAULT 0 COMMENT '消耗爱心币',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽取时间',
    INDEX `idx_user` (`user_id`),
    INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盲盒抽取记录';
