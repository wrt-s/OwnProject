-- 家人位置：逆地理结果（高德）。若列已存在可忽略报错后继续。
ALTER TABLE `family_member_location`
    ADD COLUMN `place_name` VARCHAR(128) NULL COMMENT '位置名称/POI' AFTER `accuracy`;
ALTER TABLE `family_member_location`
    ADD COLUMN `street` VARCHAR(255) NULL COMMENT '街道/门牌' AFTER `place_name`;
ALTER TABLE `family_member_location`
    ADD COLUMN `address` VARCHAR(512) NULL COMMENT '完整地址 formatted_address' AFTER `street`;