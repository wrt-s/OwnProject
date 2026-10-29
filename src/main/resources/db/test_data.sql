-- 浪漫点单系统 - 测试数据脚本
-- 用于快速填充更多测试数据

USE love_order_system;

-- 添加更多中餐菜品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(1, '宫保鸡丁', '经典川菜，香辣可口', '爱心币', 2, 0, 0, '川菜,微辣'),
(1, '鱼香肉丝', '酸甜适中，下饭神器', '抱抱', 2, 0, 0, '川菜'),
(1, '麻婆豆腐', '麻辣鲜香，入口即化', '亲亲', 1, 1, 0, '拿手菜,川菜,微辣'),
(1, '回锅肉', '肥瘦相间，香气四溢', '爱心币', 3, 0, 0, '川菜'),
(1, '番茄炒蛋', '家常美味，营养均衡', '抱抱', 1, 0, 1, '推荐,清淡'),
(1, '酸辣土豆丝', '爽脆可口，开胃下饭', '爱心币', 1, 0, 0, '素菜,微辣');

-- 添加更多西餐菜品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(2, '凯撒沙拉', '新鲜蔬菜，健康美味', '爱心币', 2, 0, 0, '沙拉,健康'),
(2, '奶油蘑菇汤', '浓郁香滑，温暖人心', '抱抱', 2, 0, 1, '推荐,热汤'),
(2, '烤鸡翅', '外酥里嫩，汁多味美', '亲亲', 2, 0, 0, ''),
(2, '海鲜烩饭', '鲜美海鲜，营养丰富', '爱心币', 5, 1, 0, '拿手菜,大餐'),
(2, '法式吐司', '香甜松软，早餐首选', '抱抱', 2, 0, 0, '早餐');

-- 添加更多甜品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(3, '草莓蛋糕', '甜蜜浪漫，颜值爆表', '亲亲', 4, 1, 1, '拿手菜,推荐,生日'),
(3, '巧克力慕斯', '丝滑浓郁，巧克力控最爱', '爱心币', 3, 0, 0, ''),
(3, '双皮奶', '嫩滑爽口，传统甜品', '抱抱', 2, 0, 0, '中式甜品'),
(3, '杨枝甘露', '清新甜美，夏日必备', '爱心币', 2, 0, 1, '推荐,港式'),
(3, '蛋挞', '酥脆香甜，一口一个', '亲亲', 1, 0, 0, '');

-- 添加更多饮品
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(4, '柠檬蜂蜜水', '清新解腻，美容养颜', '爱心币', 1, 0, 0, '养生,热饮'),
(4, '芒果冰沙', '清凉爽口，夏日解暑', '抱抱', 2, 0, 0, '冷饮'),
(4, '热可可', '香浓温暖，冬日必备', '亲亲', 2, 0, 0, '热饮'),
(4, '鲜榨橙汁', '维C满满，健康天然', '爱心币', 2, 0, 1, '推荐,果汁'),
(4, '玫瑰花茶', '花香四溢，舒缓心情', '抱抱', 1, 0, 0, '花茶,养生');

-- 添加更多水果
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(5, '哈密瓜切块', '香甜多汁，清爽可口', '爱心币', 2, 0, 0, ''),
(5, '葡萄串', '颗颗饱满，甜度爆表', '抱抱', 2, 0, 0, ''),
(5, '火龙果拼盘', '色彩鲜艳，营养丰富', '亲亲', 2, 0, 0, ''),
(5, '橙子切片', '维C丰富，清新爽口', '爱心币', 1, 0, 0, ''),
(5, '蓝莓盒装', '抗氧化神器，护眼佳品', '抱抱', 3, 0, 1, '推荐,进口');

-- 添加更多零食
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `is_recommended`, `tags`) VALUES
(6, '薯片', '咔嚓脆响，追剧必备', '爱心币', 1, 0, 0, ''),
(6, '巧克力棒', '能量补充，甜蜜时刻', '抱抱', 1, 0, 0, ''),
(6, '坚果混合', '健康零食，营养均衡', '亲亲', 2, 0, 0, '健康'),
(6, '饼干礼盒', '精致包装，送礼佳品', '爱心币', 3, 0, 0, ''),
(6, '果冻布丁', 'Q弹可爱，小朋友最爱', '抱抱', 1, 0, 0, '');

-- 添加生理期特供饮品（热饮）
INSERT INTO `dish` (`category_id`, `name`, `description`, `price_type`, `price`, `is_signature`, `tags`) VALUES
(4, '红枣桂圆茶', '补血养颜，暖心暖胃', '爱心币', 1, 0, '养生,热饮,生理期推荐'),
(4, '姜汁可乐', '驱寒暖身，缓解不适', '抱抱', 1, 0, '热饮,生理期推荐'),
(4, '热牛奶', '安神助眠，补充钙质', '亲亲', 1, 0, '热饮,生理期推荐');

-- 插入一些测试订单
INSERT INTO `orders` (`order_no`, `user_id`, `total_price`, `price_type`, `status`, `remark`, `created_at`) VALUES
('LOVE20260512000001abc12345', 1, 5, '爱心币', 'completed', '今天的晚餐', DATE_SUB(NOW(), INTERVAL 7 DAY)),
('LOVE20260512000002def67890', 1, 3, '抱抱', 'completed', '下午茶时间', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('LOVE20260512000003ghi11111', 1, 7, '亲亲', 'completed', '周末大餐', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('LOVE20260512000004jkl22222', 1, 2, '爱心币', 'cooking', '夜宵', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 为测试订单添加明细
INSERT INTO `order_item` (`order_id`, `dish_id`, `dish_name`, `quantity`, `price`, `remark`) VALUES
(1, 1, '红烧肉', 1, 3, ''),
(1, 8, '珍珠奶茶', 2, 1, '半糖去冰'),
(2, 6, '提拉米苏', 1, 3, ''),
(3, 5, '牛排', 1, 5, '七分熟'),
(3, 7, '芒果布丁', 2, 1, ''),
(4, 9, '红糖姜茶', 2, 1, '');

-- 查看数据统计
SELECT 
    '菜品总数' as item,
    COUNT(*) as count
FROM dish
UNION ALL
SELECT 
    '分类总数',
    COUNT(*)
FROM category
UNION ALL
SELECT 
    '订单总数',
    COUNT(*)
FROM orders
UNION ALL
SELECT 
    '今日订单',
    COUNT(*)
FROM orders
WHERE DATE(created_at) = CURDATE();

-- 查询最受欢迎的菜品（按订单数量）
SELECT 
    d.name as dish_name,
    COUNT(oi.id) as order_count,
    SUM(oi.quantity) as total_quantity
FROM order_item oi
JOIN dish d ON oi.dish_id = d.id
GROUP BY d.id, d.name
ORDER BY order_count DESC
LIMIT 10;
