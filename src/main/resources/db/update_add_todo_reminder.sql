-- 为 todo_item 表添加提醒相关字段
ALTER TABLE `todo_item` 
ADD COLUMN `reminder_enabled` TINYINT DEFAULT 0 COMMENT '是否开启提醒',
ADD COLUMN `reminder_time` DATETIME COMMENT '提醒时间',
ADD COLUMN `reminder_status` VARCHAR(20) DEFAULT 'pending' COMMENT '提醒状态：pending/sent/cancelled';

-- 添加索引
ALTER TABLE `todo_item` ADD INDEX idx_reminder_status (`reminder_status`);
ALTER TABLE `todo_item` ADD INDEX idx_reminder_time (`reminder_time`);
