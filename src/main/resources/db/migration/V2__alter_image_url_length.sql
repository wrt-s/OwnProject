-- 修复 image_url 字段长度问题
-- MinIO 预签名 URL 通常超过 255 字符，需要扩展到 1000

ALTER TABLE `dish` MODIFY COLUMN `image_url` VARCHAR(1000) COMMENT '图片URL';
