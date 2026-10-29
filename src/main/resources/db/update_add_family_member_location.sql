-- 家人位置：每人每家庭一条最新上报（upsert）
CREATE TABLE IF NOT EXISTS `family_member_location` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `family_id` BIGINT NOT NULL DEFAULT 1 COMMENT '家庭/情侣共同空间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(64) NULL COMMENT '上报时昵称快照',
    `role` VARCHAR(16) NULL COMMENT '上报时角色 ORDERER/CHEF',
    `lat` DOUBLE NOT NULL COMMENT '纬度',
    `lng` DOUBLE NOT NULL COMMENT '经度',
    `accuracy` DOUBLE NULL COMMENT '精度（米）',
    `place_name` VARCHAR(128) NULL COMMENT '位置名称/POI',
    `street` VARCHAR(255) NULL COMMENT '街道/门牌',
    `address` VARCHAR(512) NULL COMMENT '完整地址 formatted_address',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近上报时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次上报时间',
    UNIQUE KEY `uk_family_user_loc` (`family_id`, `user_id`),
    INDEX `idx_loc_family_updated` (`family_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家人位置 - 最新点位 upsert';