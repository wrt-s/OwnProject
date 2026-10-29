-- 头像地址与菜品图片对齐：MinIO 公网 URL 可能超过 255
-- 默认 public-read=false 时库存的是短 key（minio:uuid），本脚本仍建议执行一次
ALTER TABLE `user` MODIFY COLUMN `avatar_url` VARCHAR(1000) NULL COMMENT '头像URL';
