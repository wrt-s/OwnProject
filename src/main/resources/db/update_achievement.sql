-- 成就系统更新脚本
-- 用于添加reward_coins字段到已存在的achievement表

USE love_order_system;

-- 检查并添加 reward_coins 字段（兼容写法）
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'love_order_system'
      AND TABLE_NAME = 'achievement'
      AND COLUMN_NAME = 'reward_coins'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `achievement` ADD COLUMN `reward_coins` INT DEFAULT 0 COMMENT ''奖励爱心币数量'' AFTER `reward`',
    'SELECT "字段已存在，跳过添加" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有成就的奖励爱心币
UPDATE `achievement` SET `reward_coins` = 10 WHERE `name` = '初来乍到';
UPDATE `achievement` SET `reward_coins` = 30 WHERE `name` = '吃货达人';
UPDATE `achievement` SET `reward_coins` = 50 WHERE `name` = '连续宠爱的第7天';
UPDATE `achievement` SET `reward_coins` = 40 WHERE `name` = '甜品控';
UPDATE `achievement` SET `reward_coins` = 100 WHERE `name` = '宠妻狂魔';

-- 验证更新
SELECT * FROM `achievement`;