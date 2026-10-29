-- 为操作日志表添加新索引以提升查询性能
-- 执行时间：2026-05-14

-- 添加状态索引（用于快速筛选成功/失败的操作）
ALTER TABLE `operation_log` ADD INDEX `idx_status` (`status`);

-- 添加方法索引（用于高频操作统计）
ALTER TABLE `operation_log` ADD INDEX `idx_method` (`method`(100));

-- 添加用户和时间联合索引（用于用户操作趋势分析）
ALTER TABLE `operation_log` ADD INDEX `idx_user_time` (`user_id`, `operation_time`);
