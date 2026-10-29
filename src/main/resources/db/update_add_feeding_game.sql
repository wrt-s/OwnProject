-- 喂食大作战游戏功能数据库迁移脚本
-- 创建游戏记录表

USE love_order_system;

-- 创建喂食大作战游戏记录表
CREATE TABLE IF NOT EXISTS `feeding_game_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `score` INT NOT NULL DEFAULT 0 COMMENT '游戏得分',
    `duration` INT NOT NULL DEFAULT 0 COMMENT '游戏时长（秒）',
    `food_count` INT NOT NULL DEFAULT 0 COMMENT '吃到的食物数量',
    `dark_food_count` INT NOT NULL DEFAULT 0 COMMENT '吃到的黑暗料理数量',
    `unlocked_effect` VARCHAR(200) COMMENT '解锁的特殊效果',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '游戏时间',
    INDEX idx_user (`user_id`),
    INDEX idx_created (`created_at`),
    INDEX idx_score (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='喂食大作战游戏记录表';

-- 验证表创建
SELECT '喂食大作战游戏记录表创建成功' AS status;
