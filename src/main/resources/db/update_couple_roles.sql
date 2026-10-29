-- Love 情侣双角色迁移：ORDERER(点单方) / CHEF(做餐方)
-- 目标库：love_order_system
-- 说明：phone/password/role/last_login_time 若已存在则无需再 ALTER

-- 1) 旧角色 → 新角色
UPDATE `user` SET `role` = 'ORDERER'
WHERE `role` IS NULL OR `role` = '' OR UPPER(`role`) IN ('USER', 'ORDERER');

UPDATE `user` SET `role` = 'CHEF'
WHERE UPPER(`role`) IN ('ADMIN', 'CHEF');

-- 2) 情侣演示账号（密码 MD5(123456) = e10adc3949ba59abbe56e057f20f883e）
UPDATE `user`
SET `nickname` = '佳宝',
    `phone` = '17743761205',
    `password` = 'e10adc3949ba59abbe56e057f20f883e',
    `role` = 'ORDERER'
WHERE `id` = 1;

UPDATE `user`
SET `nickname` = '做餐方',
    `phone` = '18233008281',
    `password` = 'e10adc3949ba59abbe56e057f20f883e',
    `role` = 'CHEF'
WHERE `id` = 2;

ALTER TABLE `user` COMMENT = '用户表（情侣：ORDERER点单方 / CHEF做餐方）';

-- 校验
SELECT id, nickname, phone, role, LEFT(password, 8) AS pwd_prefix FROM `user`;
