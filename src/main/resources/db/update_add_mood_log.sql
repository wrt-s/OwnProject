-- 心情记录表（情侣家庭共享可见；每人每天一条 upsert）
CREATE TABLE IF NOT EXISTS `mood_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '心情记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `family_id` BIGINT NOT NULL DEFAULT 1 COMMENT '家庭/情侣共同空间ID',
    `mood` VARCHAR(32) NOT NULL COMMENT '心情标签：开心/平静/想你/累了/低落/生气/想吃/无聊/焦虑/睡意',
    `note` VARCHAR(200) NULL COMMENT '可选短备注',
    `mood_date` DATE NOT NULL COMMENT '心情所属日期（按 Asia/Shanghai 自然日）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_mood_user_date` (`user_id`, `mood_date`),
    INDEX `idx_mood_family_date` (`family_id`, `mood_date`),
    INDEX `idx_mood_family_user` (`family_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心情记录 - 情侣共享';
