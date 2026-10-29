-- 优化抽奖模块的中奖净收益
-- 调整奖品概率和数值，使系统更加平衡

-- 更新现有奖品配置
UPDATE `lottery_prize` SET 
    `probability` = 1.00,
    `value` = 100,
    `name` = '特等奖：100爱心币',
    `description` = '运气爆棚！获得100个爱心币'
WHERE `name` LIKE '特等奖%';

UPDATE `lottery_prize` SET 
    `probability` = 3.00,
    `value` = 50,
    `name` = '一等奖：50爱心币',
    `description` = '太棒了！获得50个爱心币'
WHERE `name` LIKE '一等奖%';

UPDATE `lottery_prize` SET 
    `probability` = 8.00,
    `value` = 20,
    `name` = '二等奖：20爱心币',
    `description` = '恭喜！获得20个爱心币'
WHERE `name` LIKE '二等奖%';

UPDATE `lottery_prize` SET 
    `probability` = 15.00,
    `value` = 10,
    `name` = '三等奖：10爱心币',
    `description` = '不错哦！获得10个爱心币'
WHERE `name` LIKE '三等奖%';

UPDATE `lottery_prize` SET 
    `probability` = 25.00,
    `value` = 5,
    `name` = '小奖：5爱心币',
    `description` = '加油！获得5个爱心币'
WHERE `name` LIKE '小奖%';

UPDATE `lottery_prize` SET 
    `probability` = 33.00,
    `value` = 2,
    `name` = '安慰奖：2爱心币',
    `description` = '继续努力！获得2个爱心币'
WHERE `name` LIKE '安慰奖%';

UPDATE `lottery_prize` SET 
    `probability` = 15.00,
    `value` = 0,
    `name` = '谢谢惠顾',
    `description` = '很遗憾，下次再来吧'
WHERE `name` LIKE '谢谢惠顾%';

-- 验证总概率是否为100%
SELECT SUM(probability) as total_probability FROM lottery_prize;