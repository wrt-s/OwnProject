/*
 Navicat Premium Dump SQL

 Source Server         : 47.102.84.75
 Source Server Type    : MySQL
 Source Server Version : 80027 (8.0.27)
 Source Host           : 47.102.84.75:3306
 Source Schema         : love_order_system

 Target Server Type    : MySQL
 Target Server Version : 80027 (8.0.27)
 File Encoding         : 65001

 Date: 15/05/2026 15:24:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for achievement
-- ----------------------------
DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成就ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成就描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标emoji',
  `condition_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '条件类型：consecutive_days/try_all_dishes/order_count/special_dish',
  `condition_value` int NULL DEFAULT NULL COMMENT '条件值',
  `reward` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '奖励描述',
  `reward_coins` int NULL DEFAULT 0 COMMENT '奖励爱心币数量',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成就表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of achievement
-- ----------------------------
INSERT INTO `achievement` VALUES (1, '初来乍到', '完成第一次点单', '🎉', 'order_count', 1, '解锁粉色主题', 10, 1, '2026-05-13 15:00:33');
INSERT INTO `achievement` VALUES (2, '吃货达人', '累计点单10次', '🍴', 'order_count', 10, '解锁专属称号', 30, 1, '2026-05-13 15:00:33');
INSERT INTO `achievement` VALUES (3, '连续宠爱的第7天', '连续7天点单', '💕', 'consecutive_days', 7, '解锁特殊皮肤', 50, 1, '2026-05-13 15:00:33');
INSERT INTO `achievement` VALUES (4, '甜品控', '尝试所有甜品', '🍰', 'try_all_dishes', 0, '解锁甜品师称号', 40, 1, '2026-05-13 15:00:33');
INSERT INTO `achievement` VALUES (5, '宠妻狂魔', '累计点单50次', '👑', 'order_count', 50, '解锁最高荣誉', 100, 1, '2026-05-13 15:00:33');

-- ----------------------------
-- Table structure for anniversary
-- ----------------------------
DROP TABLE IF EXISTS `anniversary`;
CREATE TABLE `anniversary`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '纪念日ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '纪念日名称',
  `date` date NOT NULL COMMENT '日期',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'custom' COMMENT '类型：birthday/first_date/wedding/custom',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `reminder_days` int NULL DEFAULT 3 COMMENT '提前提醒天数',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_date`(`date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '纪念日表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of anniversary
-- ----------------------------
INSERT INTO `anniversary` VALUES (1, 1, '我们的纪念日', '2024-09-21', 'first_date', '第一次在一起的日子', 7, 1, '2026-05-13 15:02:33', NULL);
INSERT INTO `anniversary` VALUES (2, 1, '佳宝的生日', '2027-01-12', 'birthday', '佳宝的生日', 7, 1, '2026-05-13 15:02:33', NULL);
INSERT INTO `anniversary` VALUES (3, 1, '臭布布的生日', '2026-12-17', 'birthday', '', 7, 1, NULL, NULL);
INSERT INTO `anniversary` VALUES (4, 1, '结婚纪念日', '2025-10-02', 'wedding', '', 10, 1, NULL, NULL);
INSERT INTO `anniversary` VALUES (5, 1, '领证', '2025-05-26', 'custom', '', 3, 1, NULL, NULL);

-- ----------------------------
-- Table structure for betting_record
-- ----------------------------
DROP TABLE IF EXISTS `betting_record`;
CREATE TABLE `betting_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bet_amount` int NOT NULL COMMENT '下注金额',
  `bet_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '押注类型：big/small',
  `dice_result` int NOT NULL COMMENT '骰子结果（1-6）',
  `result_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '结果类型：big/small',
  `is_win` tinyint NOT NULL COMMENT '是否获胜：0-失败，1-胜利',
  `reward_amount` int NULL DEFAULT 0 COMMENT '奖励金额（获胜时）',
  `deduct_amount` int NOT NULL COMMENT '扣除金额',
  `is_half_deduct` tinyint NULL DEFAULT 0 COMMENT '是否半扣：0-正常扣，1-只扣一半',
  `consecutive_loss` int NULL DEFAULT 0 COMMENT '连续输的次数',
  `daily_total_loss` int NULL DEFAULT 0 COMMENT '当日累计损失',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '押注时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  INDEX `idx_date`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '幸运币押注记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of betting_record
-- ----------------------------
INSERT INTO `betting_record` VALUES (1, 1, 15, 'big', 2, 'small', 0, 0, 15, 0, 1, 15, NULL);
INSERT INTO `betting_record` VALUES (2, 1, 15, 'big', 5, 'big', 1, 27, 15, 0, 0, 15, NULL);
INSERT INTO `betting_record` VALUES (3, 1, 15, 'big', 1, 'small', 0, 0, 15, 0, 1, 30, NULL);
INSERT INTO `betting_record` VALUES (4, 1, 100, 'big', 3, 'small', 0, 0, 100, 0, 1, 100, NULL);

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标emoji',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (1, '中餐', '🍜', 1, 1, '2026-05-13 15:00:33', '2026-05-13 15:00:33');
INSERT INTO `category` VALUES (2, '西餐', '🍝', 2, 1, '2026-05-13 15:00:33', '2026-05-13 15:00:33');
INSERT INTO `category` VALUES (3, '甜品', '🍰', 3, 1, '2026-05-13 15:00:33', '2026-05-13 15:00:33');
INSERT INTO `category` VALUES (4, '饮品', '🥤', 4, 1, '2026-05-13 15:00:33', '2026-05-13 15:00:33');
INSERT INTO `category` VALUES (5, '水果', '🍎', 5, 1, '2026-05-13 15:00:33', NULL);
INSERT INTO `category` VALUES (6, '零食', '🍪', 6, 1, '2026-05-13 15:00:33', NULL);

-- ----------------------------
-- Table structure for coin_balance
-- ----------------------------
DROP TABLE IF EXISTS `coin_balance`;
CREATE TABLE `coin_balance`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `balance` int NULL DEFAULT 0 COMMENT '当前余额',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '爱心币余额表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coin_balance
-- ----------------------------
INSERT INTO `coin_balance` VALUES (1, 1, 706, '2026-05-13 15:00:33', '2026-05-13 15:45:42');

-- ----------------------------
-- Table structure for coin_transaction
-- ----------------------------
DROP TABLE IF EXISTS `coin_transaction`;
CREATE TABLE `coin_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `amount` int NOT NULL COMMENT '变动数量（正数为增加，负数为减少）',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：add_坏坏/use_order/use_task',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `related_id` bigint NULL DEFAULT NULL COMMENT '关联ID（订单ID或任务记录ID）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 382 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '爱心币流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coin_transaction
-- ----------------------------
INSERT INTO `coin_transaction` VALUES (1, 1, -10, 90, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 15:10:49');
INSERT INTO `coin_transaction` VALUES (2, 1, 20, 110, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 15:10:49');
INSERT INTO `coin_transaction` VALUES (3, 1, 5, 115, 'add_签到', '每日签到奖励（连续1天）', NULL, '2026-05-13 15:37:58');
INSERT INTO `coin_transaction` VALUES (5, 1, -10, 510, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 15:57:47');
INSERT INTO `coin_transaction` VALUES (6, 1, 20, 530, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 15:57:47');
INSERT INTO `coin_transaction` VALUES (7, 1, -10, 520, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:01:41');
INSERT INTO `coin_transaction` VALUES (8, 1, 30, 550, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:01:41');
INSERT INTO `coin_transaction` VALUES (9, 1, -10, 540, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:04:42');
INSERT INTO `coin_transaction` VALUES (10, 1, 10, 550, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:04:42');
INSERT INTO `coin_transaction` VALUES (11, 1, -10, 540, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:04:49');
INSERT INTO `coin_transaction` VALUES (12, 1, 10, 550, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:04:49');
INSERT INTO `coin_transaction` VALUES (13, 1, -10, 540, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:04:56');
INSERT INTO `coin_transaction` VALUES (14, 1, 50, 590, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:04:56');
INSERT INTO `coin_transaction` VALUES (15, 1, -10, 580, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:04');
INSERT INTO `coin_transaction` VALUES (16, 1, 30, 610, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:05:04');
INSERT INTO `coin_transaction` VALUES (17, 1, -10, 600, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:12');
INSERT INTO `coin_transaction` VALUES (18, 1, 30, 630, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:05:12');
INSERT INTO `coin_transaction` VALUES (19, 1, -10, 620, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:18');
INSERT INTO `coin_transaction` VALUES (20, 1, 20, 640, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:05:18');
INSERT INTO `coin_transaction` VALUES (21, 1, -10, 630, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:24');
INSERT INTO `coin_transaction` VALUES (22, 1, 20, 650, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:05:24');
INSERT INTO `coin_transaction` VALUES (23, 1, -10, 640, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:44');
INSERT INTO `coin_transaction` VALUES (24, 1, 30, 670, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:05:44');
INSERT INTO `coin_transaction` VALUES (25, 1, -10, 660, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:05:50');
INSERT INTO `coin_transaction` VALUES (26, 1, 20, 680, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:05:50');
INSERT INTO `coin_transaction` VALUES (27, 1, -10, 670, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:06:03');
INSERT INTO `coin_transaction` VALUES (28, 1, 20, 690, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:06:03');
INSERT INTO `coin_transaction` VALUES (29, 1, -10, 680, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:06:09');
INSERT INTO `coin_transaction` VALUES (30, 1, 20, 700, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:06:09');
INSERT INTO `coin_transaction` VALUES (31, 1, -10, 690, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:06:15');
INSERT INTO `coin_transaction` VALUES (32, 1, 100, 790, 'add_抽奖', '转盘抽奖获得：特等奖：100爱心币', NULL, '2026-05-13 16:06:15');
INSERT INTO `coin_transaction` VALUES (33, 1, -10, 780, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:06:40');
INSERT INTO `coin_transaction` VALUES (34, 1, 5, 785, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:06:40');
INSERT INTO `coin_transaction` VALUES (35, 1, -10, 775, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:06:49');
INSERT INTO `coin_transaction` VALUES (36, 1, -10, 765, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:13:31');
INSERT INTO `coin_transaction` VALUES (37, 1, 10, 775, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:13:31');
INSERT INTO `coin_transaction` VALUES (38, 1, -10, 765, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:13:38');
INSERT INTO `coin_transaction` VALUES (39, 1, 10, 775, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:13:39');
INSERT INTO `coin_transaction` VALUES (40, 1, -10, 765, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:13:46');
INSERT INTO `coin_transaction` VALUES (41, 1, 5, 770, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:13:46');
INSERT INTO `coin_transaction` VALUES (42, 1, -10, 760, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:13:52');
INSERT INTO `coin_transaction` VALUES (43, 1, 5, 765, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:13:52');
INSERT INTO `coin_transaction` VALUES (44, 1, -10, 755, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:13:58');
INSERT INTO `coin_transaction` VALUES (45, 1, -10, 745, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:14:04');
INSERT INTO `coin_transaction` VALUES (46, 1, 20, 765, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:14:04');
INSERT INTO `coin_transaction` VALUES (47, 1, -10, 755, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:14:45');
INSERT INTO `coin_transaction` VALUES (48, 1, 10, 765, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:14:45');
INSERT INTO `coin_transaction` VALUES (49, 1, -10, 755, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:14:54');
INSERT INTO `coin_transaction` VALUES (50, 1, 10, 765, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:14:54');
INSERT INTO `coin_transaction` VALUES (51, 1, -10, 755, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:15:06');
INSERT INTO `coin_transaction` VALUES (52, 1, 5, 760, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:15:06');
INSERT INTO `coin_transaction` VALUES (53, 1, -10, 750, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:15:13');
INSERT INTO `coin_transaction` VALUES (54, 1, 30, 780, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:15:13');
INSERT INTO `coin_transaction` VALUES (55, 1, -10, 770, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:15:23');
INSERT INTO `coin_transaction` VALUES (56, 1, 5, 775, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:15:23');
INSERT INTO `coin_transaction` VALUES (57, 1, -10, 765, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:04');
INSERT INTO `coin_transaction` VALUES (58, 1, 5, 770, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:16:04');
INSERT INTO `coin_transaction` VALUES (59, 1, -10, 760, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:10');
INSERT INTO `coin_transaction` VALUES (60, 1, 20, 780, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:16:10');
INSERT INTO `coin_transaction` VALUES (61, 1, -10, 770, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:16');
INSERT INTO `coin_transaction` VALUES (62, 1, -10, 760, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:22');
INSERT INTO `coin_transaction` VALUES (63, 1, -10, 750, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:28');
INSERT INTO `coin_transaction` VALUES (64, 1, 30, 780, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:16:28');
INSERT INTO `coin_transaction` VALUES (65, 1, -10, 770, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:40');
INSERT INTO `coin_transaction` VALUES (66, 1, -10, 760, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:46');
INSERT INTO `coin_transaction` VALUES (67, 1, 10, 770, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:16:46');
INSERT INTO `coin_transaction` VALUES (68, 1, -10, 760, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:16:52');
INSERT INTO `coin_transaction` VALUES (69, 1, 50, 810, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:16:52');
INSERT INTO `coin_transaction` VALUES (70, 1, -10, 800, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:12');
INSERT INTO `coin_transaction` VALUES (71, 1, -10, 790, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:18');
INSERT INTO `coin_transaction` VALUES (72, 1, 30, 820, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:17:18');
INSERT INTO `coin_transaction` VALUES (73, 1, -10, 810, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:23');
INSERT INTO `coin_transaction` VALUES (74, 1, -10, 800, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:29');
INSERT INTO `coin_transaction` VALUES (75, 1, 50, 850, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:17:29');
INSERT INTO `coin_transaction` VALUES (76, 1, -10, 840, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:38');
INSERT INTO `coin_transaction` VALUES (77, 1, 20, 860, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:17:38');
INSERT INTO `coin_transaction` VALUES (78, 1, -10, 850, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:53');
INSERT INTO `coin_transaction` VALUES (79, 1, 10, 860, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:17:53');
INSERT INTO `coin_transaction` VALUES (80, 1, -10, 850, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:17:59');
INSERT INTO `coin_transaction` VALUES (81, 1, 50, 900, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:17:59');
INSERT INTO `coin_transaction` VALUES (82, 1, -10, 890, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:05');
INSERT INTO `coin_transaction` VALUES (83, 1, 5, 895, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:18:05');
INSERT INTO `coin_transaction` VALUES (84, 1, -10, 885, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:20');
INSERT INTO `coin_transaction` VALUES (85, 1, 10, 895, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:18:20');
INSERT INTO `coin_transaction` VALUES (86, 1, -10, 885, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:26');
INSERT INTO `coin_transaction` VALUES (87, 1, 10, 895, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:18:26');
INSERT INTO `coin_transaction` VALUES (88, 1, -10, 885, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:32');
INSERT INTO `coin_transaction` VALUES (89, 1, 5, 890, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:18:32');
INSERT INTO `coin_transaction` VALUES (90, 1, -10, 880, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:37');
INSERT INTO `coin_transaction` VALUES (91, 1, 10, 890, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:18:37');
INSERT INTO `coin_transaction` VALUES (92, 1, -10, 880, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:52');
INSERT INTO `coin_transaction` VALUES (93, 1, 5, 885, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:18:52');
INSERT INTO `coin_transaction` VALUES (94, 1, -10, 875, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:18:57');
INSERT INTO `coin_transaction` VALUES (95, 1, 30, 905, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:18:57');
INSERT INTO `coin_transaction` VALUES (96, 1, -10, 895, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:04');
INSERT INTO `coin_transaction` VALUES (97, 1, 30, 925, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:19:04');
INSERT INTO `coin_transaction` VALUES (98, 1, -10, 915, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:19');
INSERT INTO `coin_transaction` VALUES (99, 1, 10, 925, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:19:19');
INSERT INTO `coin_transaction` VALUES (100, 1, -10, 915, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:24');
INSERT INTO `coin_transaction` VALUES (101, 1, 5, 920, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:19:24');
INSERT INTO `coin_transaction` VALUES (102, 1, -10, 910, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:30');
INSERT INTO `coin_transaction` VALUES (103, 1, -10, 900, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:36');
INSERT INTO `coin_transaction` VALUES (104, 1, -10, 890, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:41');
INSERT INTO `coin_transaction` VALUES (105, 1, -10, 880, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:47');
INSERT INTO `coin_transaction` VALUES (106, 1, -10, 870, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:19:52');
INSERT INTO `coin_transaction` VALUES (107, 1, 10, 880, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:19:52');
INSERT INTO `coin_transaction` VALUES (108, 1, -10, 870, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:07');
INSERT INTO `coin_transaction` VALUES (109, 1, 10, 880, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:20:07');
INSERT INTO `coin_transaction` VALUES (110, 1, -10, 870, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:13');
INSERT INTO `coin_transaction` VALUES (111, 1, 20, 890, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:20:13');
INSERT INTO `coin_transaction` VALUES (112, 1, -10, 880, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:19');
INSERT INTO `coin_transaction` VALUES (113, 1, 5, 885, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:20:19');
INSERT INTO `coin_transaction` VALUES (114, 1, -10, 875, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:25');
INSERT INTO `coin_transaction` VALUES (115, 1, 20, 895, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:20:25');
INSERT INTO `coin_transaction` VALUES (116, 1, -10, 885, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:38');
INSERT INTO `coin_transaction` VALUES (117, 1, 5, 890, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:20:38');
INSERT INTO `coin_transaction` VALUES (118, 1, -10, 880, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:44');
INSERT INTO `coin_transaction` VALUES (119, 1, 20, 900, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:20:44');
INSERT INTO `coin_transaction` VALUES (120, 1, -10, 890, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:50');
INSERT INTO `coin_transaction` VALUES (121, 1, 5, 895, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:20:50');
INSERT INTO `coin_transaction` VALUES (122, 1, -10, 885, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:20:55');
INSERT INTO `coin_transaction` VALUES (123, 1, 50, 935, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:20:55');
INSERT INTO `coin_transaction` VALUES (124, 1, -10, 925, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:11');
INSERT INTO `coin_transaction` VALUES (125, 1, 10, 935, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:21:11');
INSERT INTO `coin_transaction` VALUES (126, 1, -10, 925, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:17');
INSERT INTO `coin_transaction` VALUES (127, 1, -10, 915, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:23');
INSERT INTO `coin_transaction` VALUES (128, 1, 10, 925, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:21:23');
INSERT INTO `coin_transaction` VALUES (129, 1, -10, 915, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:29');
INSERT INTO `coin_transaction` VALUES (130, 1, 10, 925, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:21:29');
INSERT INTO `coin_transaction` VALUES (131, 1, -10, 915, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:35');
INSERT INTO `coin_transaction` VALUES (132, 1, 30, 945, 'add_抽奖', '转盘抽奖获得：二等奖：30爱心币', NULL, '2026-05-13 16:21:35');
INSERT INTO `coin_transaction` VALUES (133, 1, -10, 935, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:21:46');
INSERT INTO `coin_transaction` VALUES (134, 1, 5, 940, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:21:46');
INSERT INTO `coin_transaction` VALUES (135, 1, -10, 930, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:03');
INSERT INTO `coin_transaction` VALUES (136, 1, 20, 950, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:22:03');
INSERT INTO `coin_transaction` VALUES (137, 1, -10, 940, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:11');
INSERT INTO `coin_transaction` VALUES (138, 1, 5, 945, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:22:11');
INSERT INTO `coin_transaction` VALUES (139, 1, -10, 935, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:17');
INSERT INTO `coin_transaction` VALUES (140, 1, 10, 945, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:22:17');
INSERT INTO `coin_transaction` VALUES (141, 1, -10, 935, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:28');
INSERT INTO `coin_transaction` VALUES (142, 1, 5, 940, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:22:28');
INSERT INTO `coin_transaction` VALUES (143, 1, -10, 930, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:34');
INSERT INTO `coin_transaction` VALUES (144, 1, -10, 920, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:40');
INSERT INTO `coin_transaction` VALUES (145, 1, -10, 910, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:45');
INSERT INTO `coin_transaction` VALUES (146, 1, 20, 930, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:22:45');
INSERT INTO `coin_transaction` VALUES (147, 1, -10, 920, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:22:53');
INSERT INTO `coin_transaction` VALUES (148, 1, 50, 970, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:22:53');
INSERT INTO `coin_transaction` VALUES (149, 1, -10, 960, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:09');
INSERT INTO `coin_transaction` VALUES (150, 1, 10, 970, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:23:09');
INSERT INTO `coin_transaction` VALUES (151, 1, -10, 960, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:16');
INSERT INTO `coin_transaction` VALUES (152, 1, 5, 965, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:23:16');
INSERT INTO `coin_transaction` VALUES (153, 1, -10, 955, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:22');
INSERT INTO `coin_transaction` VALUES (154, 1, -10, 945, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:27');
INSERT INTO `coin_transaction` VALUES (155, 1, 10, 955, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:23:27');
INSERT INTO `coin_transaction` VALUES (156, 1, -10, 945, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:33');
INSERT INTO `coin_transaction` VALUES (157, 1, 20, 965, 'add_抽奖', '转盘抽奖获得：三等奖：20爱心币', NULL, '2026-05-13 16:23:33');
INSERT INTO `coin_transaction` VALUES (158, 1, -10, 955, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:44');
INSERT INTO `coin_transaction` VALUES (159, 1, 10, 965, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:23:44');
INSERT INTO `coin_transaction` VALUES (160, 1, -10, 955, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:50');
INSERT INTO `coin_transaction` VALUES (161, 1, 5, 960, 'add_抽奖', '转盘抽奖获得：安慰奖：5爱心币', NULL, '2026-05-13 16:23:50');
INSERT INTO `coin_transaction` VALUES (162, 1, -10, 950, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:23:56');
INSERT INTO `coin_transaction` VALUES (163, 1, 10, 960, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 16:23:56');
INSERT INTO `coin_transaction` VALUES (164, 1, -10, 950, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 16:24:13');
INSERT INTO `coin_transaction` VALUES (165, 1, 50, 1000, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 16:24:13');
INSERT INTO `coin_transaction` VALUES (166, 1, -30, 970, 'use_task', '布布做事: 倒水', NULL, '2026-05-13 20:33:58');
INSERT INTO `coin_transaction` VALUES (167, 1, -10, 960, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 20:34:27');
INSERT INTO `coin_transaction` VALUES (168, 1, 10, 970, 'add_抽奖', '转盘抽奖获得：小奖：10爱心币', NULL, '2026-05-13 20:34:27');
INSERT INTO `coin_transaction` VALUES (169, 1, -10, 960, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-13 20:34:37');
INSERT INTO `coin_transaction` VALUES (170, 1, 50, 1010, 'add_抽奖', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-13 20:34:37');
INSERT INTO `coin_transaction` VALUES (171, 1, 5, 1015, 'add_签到', '每日签到奖励（连续2天）', NULL, '2026-05-14 07:42:37');
INSERT INTO `coin_transaction` VALUES (172, 1, -10, 1005, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 09:19:31');
INSERT INTO `coin_transaction` VALUES (173, 1, 10, 1015, 'add_抽奖', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 09:19:32');
INSERT INTO `coin_transaction` VALUES (174, 1, -30, 985, 'use_order', '点单消费: LOVE20260514151338479b1873', 5, '2026-05-14 15:13:39');
INSERT INTO `coin_transaction` VALUES (175, 1, -30, 985, 'use_order', '点单消费: LOVE2026051415134016f931de', 6, '2026-05-14 15:13:41');
INSERT INTO `coin_transaction` VALUES (176, 1, -30, 955, 'use_order', '点单消费: LOVE20260514151341084e4e8a', 7, '2026-05-14 15:13:42');
INSERT INTO `coin_transaction` VALUES (177, 1, -30, 955, 'use_order', '点单消费: LOVE202605141513419086eaaa', 8, '2026-05-14 15:13:44');
INSERT INTO `coin_transaction` VALUES (178, 1, 30, 985, 'refund_order', '订单取消退款: LOVE20260514151341084e4e8a', 7, '2026-05-14 15:32:02');
INSERT INTO `coin_transaction` VALUES (179, 1, 30, 1015, 'refund_order', '订单取消退款: LOVE202605141513419086eaaa', 8, '2026-05-14 15:32:19');
INSERT INTO `coin_transaction` VALUES (180, 1, 30, 1045, 'refund_order', '订单取消退款: LOVE2026051415134016f931de', 6, '2026-05-14 15:32:23');
INSERT INTO `coin_transaction` VALUES (181, 1, 30, 1075, 'refund_order', '订单取消退款: LOVE20260514151338479b1873', 5, '2026-05-14 15:32:28');
INSERT INTO `coin_transaction` VALUES (182, 1, -30, 1045, 'use_order', '点单消费: LOVE20260514153319a5d288ce', 9, '2026-05-14 15:33:20');
INSERT INTO `coin_transaction` VALUES (183, 1, -15, 1030, 'use_betting', '幸运币押注 - 押大', NULL, '2026-05-14 15:35:32');
INSERT INTO `coin_transaction` VALUES (184, 1, -15, 1015, 'use_betting', '幸运币押注 - 押大', NULL, '2026-05-14 15:35:50');
INSERT INTO `coin_transaction` VALUES (185, 1, 27, 1042, 'add_betting', '幸运币押注获胜 - 骰子点数：5（大）', NULL, '2026-05-14 15:35:50');
INSERT INTO `coin_transaction` VALUES (186, 1, -15, 1027, 'use_betting', '幸运币押注 - 押大', NULL, '2026-05-14 15:36:03');
INSERT INTO `coin_transaction` VALUES (187, 1, -10, 1017, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:36:31');
INSERT INTO `coin_transaction` VALUES (188, 1, 2, 1019, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:36:31');
INSERT INTO `coin_transaction` VALUES (189, 1, -10, 1009, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:37:21');
INSERT INTO `coin_transaction` VALUES (190, 1, 5, 1014, 'add_抽奖', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 15:37:21');
INSERT INTO `coin_transaction` VALUES (191, 1, -10, 1004, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:37:28');
INSERT INTO `coin_transaction` VALUES (192, 1, -10, 994, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:37:33');
INSERT INTO `coin_transaction` VALUES (193, 1, 5, 999, 'add_抽奖', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 15:37:33');
INSERT INTO `coin_transaction` VALUES (194, 1, -10, 989, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:37:44');
INSERT INTO `coin_transaction` VALUES (195, 1, 5, 994, 'add_抽奖', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 15:37:44');
INSERT INTO `coin_transaction` VALUES (196, 1, -10, 984, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:01');
INSERT INTO `coin_transaction` VALUES (197, 1, -10, 974, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:07');
INSERT INTO `coin_transaction` VALUES (198, 1, 10, 984, 'add_抽奖', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 15:38:07');
INSERT INTO `coin_transaction` VALUES (199, 1, -10, 974, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:14');
INSERT INTO `coin_transaction` VALUES (200, 1, 2, 976, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:38:14');
INSERT INTO `coin_transaction` VALUES (201, 1, -10, 966, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:20');
INSERT INTO `coin_transaction` VALUES (202, 1, 2, 968, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:38:20');
INSERT INTO `coin_transaction` VALUES (203, 1, -10, 958, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:27');
INSERT INTO `coin_transaction` VALUES (204, 1, 2, 960, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:38:27');
INSERT INTO `coin_transaction` VALUES (205, 1, -10, 950, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:38:33');
INSERT INTO `coin_transaction` VALUES (206, 1, 2, 952, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:38:33');
INSERT INTO `coin_transaction` VALUES (207, 1, -10, 942, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:42:09');
INSERT INTO `coin_transaction` VALUES (208, 1, 2, 944, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:42:09');
INSERT INTO `coin_transaction` VALUES (209, 1, -10, 934, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:42:17');
INSERT INTO `coin_transaction` VALUES (210, 1, 5, 939, 'add_抽奖', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 15:42:17');
INSERT INTO `coin_transaction` VALUES (211, 1, -10, 929, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:42:22');
INSERT INTO `coin_transaction` VALUES (212, 1, 2, 931, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:42:22');
INSERT INTO `coin_transaction` VALUES (213, 1, -10, 921, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:42:34');
INSERT INTO `coin_transaction` VALUES (214, 1, 2, 923, 'add_抽奖', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 15:42:34');
INSERT INTO `coin_transaction` VALUES (215, 1, -10, 913, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 15:43:58');
INSERT INTO `coin_transaction` VALUES (216, 1, 5, 918, 'add_抽奖', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 15:43:58');
INSERT INTO `coin_transaction` VALUES (217, 1, -50, 868, 'use_task', '布布做事: 修改为原来的爱心币抽奖😛', NULL, '2026-05-14 15:47:29');
INSERT INTO `coin_transaction` VALUES (218, 1, 300, 1168, 'add_奖励', '通过奖励方式增加 - 2026-05-14 晚上', NULL, '2026-05-14 22:39:18');
INSERT INTO `coin_transaction` VALUES (219, 1, -10, 1158, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:41:14');
INSERT INTO `coin_transaction` VALUES (220, 1, 2, 1160, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:41:14');
INSERT INTO `coin_transaction` VALUES (221, 1, -10, 1150, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:41:53');
INSERT INTO `coin_transaction` VALUES (222, 1, 2, 1152, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:41:53');
INSERT INTO `coin_transaction` VALUES (223, 1, -10, 1142, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:42:01');
INSERT INTO `coin_transaction` VALUES (224, 1, 5, 1147, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:42:01');
INSERT INTO `coin_transaction` VALUES (225, 1, -10, 1137, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:42:47');
INSERT INTO `coin_transaction` VALUES (226, 1, 5, 1142, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:42:47');
INSERT INTO `coin_transaction` VALUES (227, 1, -10, 1132, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:42:53');
INSERT INTO `coin_transaction` VALUES (228, 1, 10, 1142, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:42:53');
INSERT INTO `coin_transaction` VALUES (229, 1, -10, 1132, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:01');
INSERT INTO `coin_transaction` VALUES (230, 1, 20, 1152, 'add_lottery', '转盘抽奖获得：二等奖：20爱心币', NULL, '2026-05-14 22:43:01');
INSERT INTO `coin_transaction` VALUES (231, 1, -10, 1142, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:10');
INSERT INTO `coin_transaction` VALUES (232, 1, 2, 1144, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:43:10');
INSERT INTO `coin_transaction` VALUES (233, 1, -10, 1134, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:20');
INSERT INTO `coin_transaction` VALUES (234, 1, 2, 1136, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:43:20');
INSERT INTO `coin_transaction` VALUES (235, 1, -10, 1126, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:27');
INSERT INTO `coin_transaction` VALUES (236, 1, 2, 1128, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:43:28');
INSERT INTO `coin_transaction` VALUES (237, 1, -10, 1118, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:34');
INSERT INTO `coin_transaction` VALUES (238, 1, 10, 1128, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:43:34');
INSERT INTO `coin_transaction` VALUES (239, 1, -10, 1118, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:46');
INSERT INTO `coin_transaction` VALUES (240, 1, 50, 1168, 'add_lottery', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-14 22:43:46');
INSERT INTO `coin_transaction` VALUES (241, 1, -10, 1158, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:53');
INSERT INTO `coin_transaction` VALUES (242, 1, 2, 1160, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:43:53');
INSERT INTO `coin_transaction` VALUES (243, 1, -10, 1150, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:43:59');
INSERT INTO `coin_transaction` VALUES (244, 1, 2, 1152, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:43:59');
INSERT INTO `coin_transaction` VALUES (245, 1, -10, 1142, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:05');
INSERT INTO `coin_transaction` VALUES (246, 1, 2, 1144, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:44:05');
INSERT INTO `coin_transaction` VALUES (247, 1, -10, 1134, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:10');
INSERT INTO `coin_transaction` VALUES (248, 1, 10, 1144, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:44:10');
INSERT INTO `coin_transaction` VALUES (249, 1, -10, 1134, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:29');
INSERT INTO `coin_transaction` VALUES (250, 1, 20, 1154, 'add_lottery', '转盘抽奖获得：二等奖：20爱心币', NULL, '2026-05-14 22:44:29');
INSERT INTO `coin_transaction` VALUES (251, 1, -10, 1144, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:36');
INSERT INTO `coin_transaction` VALUES (252, 1, 2, 1146, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:44:36');
INSERT INTO `coin_transaction` VALUES (253, 1, -10, 1136, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:42');
INSERT INTO `coin_transaction` VALUES (254, 1, 10, 1146, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:44:42');
INSERT INTO `coin_transaction` VALUES (255, 1, -10, 1136, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:48');
INSERT INTO `coin_transaction` VALUES (256, 1, -10, 1126, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:44:54');
INSERT INTO `coin_transaction` VALUES (257, 1, 50, 1176, 'add_lottery', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-14 22:44:54');
INSERT INTO `coin_transaction` VALUES (258, 1, -10, 1166, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:01');
INSERT INTO `coin_transaction` VALUES (259, 1, 5, 1171, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:45:01');
INSERT INTO `coin_transaction` VALUES (260, 1, -10, 1161, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:10');
INSERT INTO `coin_transaction` VALUES (261, 1, 10, 1171, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:45:10');
INSERT INTO `coin_transaction` VALUES (262, 1, -10, 1161, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:16');
INSERT INTO `coin_transaction` VALUES (263, 1, 5, 1166, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:45:16');
INSERT INTO `coin_transaction` VALUES (264, 1, -10, 1156, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:22');
INSERT INTO `coin_transaction` VALUES (265, 1, 2, 1158, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:45:22');
INSERT INTO `coin_transaction` VALUES (266, 1, -10, 1148, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:29');
INSERT INTO `coin_transaction` VALUES (267, 1, -10, 1138, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:35');
INSERT INTO `coin_transaction` VALUES (268, 1, 20, 1158, 'add_lottery', '转盘抽奖获得：二等奖：20爱心币', NULL, '2026-05-14 22:45:35');
INSERT INTO `coin_transaction` VALUES (269, 1, -10, 1148, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:42');
INSERT INTO `coin_transaction` VALUES (270, 1, 5, 1153, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:45:42');
INSERT INTO `coin_transaction` VALUES (271, 1, -10, 1143, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:48');
INSERT INTO `coin_transaction` VALUES (272, 1, -10, 1133, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:54');
INSERT INTO `coin_transaction` VALUES (273, 1, 5, 1138, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:45:54');
INSERT INTO `coin_transaction` VALUES (274, 1, -10, 1128, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:45:59');
INSERT INTO `coin_transaction` VALUES (275, 1, 2, 1130, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:46:00');
INSERT INTO `coin_transaction` VALUES (276, 1, -10, 1120, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:05');
INSERT INTO `coin_transaction` VALUES (277, 1, -10, 1110, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:29');
INSERT INTO `coin_transaction` VALUES (278, 1, 2, 1112, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:46:29');
INSERT INTO `coin_transaction` VALUES (279, 1, -10, 1102, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:37');
INSERT INTO `coin_transaction` VALUES (280, 1, 5, 1107, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:46:37');
INSERT INTO `coin_transaction` VALUES (281, 1, -10, 1097, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:43');
INSERT INTO `coin_transaction` VALUES (282, 1, 2, 1099, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:46:43');
INSERT INTO `coin_transaction` VALUES (283, 1, -10, 1089, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:53');
INSERT INTO `coin_transaction` VALUES (284, 1, 5, 1094, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:46:53');
INSERT INTO `coin_transaction` VALUES (285, 1, -10, 1084, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:46:59');
INSERT INTO `coin_transaction` VALUES (286, 1, 5, 1089, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:46:59');
INSERT INTO `coin_transaction` VALUES (287, 1, -10, 1079, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:47:56');
INSERT INTO `coin_transaction` VALUES (288, 1, 2, 1081, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:47:56');
INSERT INTO `coin_transaction` VALUES (289, 1, -10, 1071, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:48:03');
INSERT INTO `coin_transaction` VALUES (290, 1, 5, 1076, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:48:03');
INSERT INTO `coin_transaction` VALUES (291, 1, -10, 1066, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:48:09');
INSERT INTO `coin_transaction` VALUES (292, 1, 5, 1071, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:48:09');
INSERT INTO `coin_transaction` VALUES (293, 1, -10, 1061, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:48:25');
INSERT INTO `coin_transaction` VALUES (294, 1, 10, 1071, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:48:25');
INSERT INTO `coin_transaction` VALUES (295, 1, -10, 1061, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:49:29');
INSERT INTO `coin_transaction` VALUES (296, 1, 2, 1063, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:49:29');
INSERT INTO `coin_transaction` VALUES (297, 1, -10, 1053, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:49:58');
INSERT INTO `coin_transaction` VALUES (298, 1, 2, 1055, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:49:58');
INSERT INTO `coin_transaction` VALUES (299, 1, -10, 1045, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:04');
INSERT INTO `coin_transaction` VALUES (300, 1, 5, 1050, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:50:04');
INSERT INTO `coin_transaction` VALUES (301, 1, -10, 1040, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:11');
INSERT INTO `coin_transaction` VALUES (302, 1, 2, 1042, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:50:11');
INSERT INTO `coin_transaction` VALUES (303, 1, -10, 1032, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:17');
INSERT INTO `coin_transaction` VALUES (304, 1, 5, 1037, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:50:17');
INSERT INTO `coin_transaction` VALUES (305, 1, -10, 1027, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:26');
INSERT INTO `coin_transaction` VALUES (306, 1, -10, 1017, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:35');
INSERT INTO `coin_transaction` VALUES (307, 1, -10, 1007, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:40');
INSERT INTO `coin_transaction` VALUES (308, 1, -10, 997, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:50:46');
INSERT INTO `coin_transaction` VALUES (309, 1, 5, 1002, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:50:46');
INSERT INTO `coin_transaction` VALUES (310, 1, -10, 992, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:51:17');
INSERT INTO `coin_transaction` VALUES (311, 1, -10, 982, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:51:27');
INSERT INTO `coin_transaction` VALUES (312, 1, 10, 992, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:51:27');
INSERT INTO `coin_transaction` VALUES (313, 1, -10, 982, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:51:35');
INSERT INTO `coin_transaction` VALUES (314, 1, 5, 987, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:51:35');
INSERT INTO `coin_transaction` VALUES (315, 1, -10, 977, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:52:20');
INSERT INTO `coin_transaction` VALUES (316, 1, 2, 979, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:52:20');
INSERT INTO `coin_transaction` VALUES (317, 1, -10, 969, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:52:58');
INSERT INTO `coin_transaction` VALUES (318, 1, 2, 971, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:52:58');
INSERT INTO `coin_transaction` VALUES (319, 1, -10, 961, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:53:05');
INSERT INTO `coin_transaction` VALUES (320, 1, 2, 963, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:53:05');
INSERT INTO `coin_transaction` VALUES (321, 1, -10, 953, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:53:11');
INSERT INTO `coin_transaction` VALUES (322, 1, -10, 943, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:53:18');
INSERT INTO `coin_transaction` VALUES (323, 1, -10, 933, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:02');
INSERT INTO `coin_transaction` VALUES (324, 1, -10, 923, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:07');
INSERT INTO `coin_transaction` VALUES (325, 1, 5, 928, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:54:08');
INSERT INTO `coin_transaction` VALUES (326, 1, -10, 918, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:15');
INSERT INTO `coin_transaction` VALUES (327, 1, 10, 928, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:54:15');
INSERT INTO `coin_transaction` VALUES (328, 1, -10, 918, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:21');
INSERT INTO `coin_transaction` VALUES (329, 1, 5, 923, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:54:21');
INSERT INTO `coin_transaction` VALUES (330, 1, -10, 913, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:27');
INSERT INTO `coin_transaction` VALUES (331, 1, 50, 963, 'add_lottery', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-14 22:54:27');
INSERT INTO `coin_transaction` VALUES (332, 1, -10, 953, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:33');
INSERT INTO `coin_transaction` VALUES (333, 1, 10, 963, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:54:33');
INSERT INTO `coin_transaction` VALUES (334, 1, -10, 953, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:39');
INSERT INTO `coin_transaction` VALUES (335, 1, 5, 958, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:54:39');
INSERT INTO `coin_transaction` VALUES (336, 1, -10, 948, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:45');
INSERT INTO `coin_transaction` VALUES (337, 1, 5, 953, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-14 22:54:45');
INSERT INTO `coin_transaction` VALUES (338, 1, -10, 943, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:50');
INSERT INTO `coin_transaction` VALUES (339, 1, 10, 953, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-14 22:54:50');
INSERT INTO `coin_transaction` VALUES (340, 1, -10, 943, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-14 22:54:56');
INSERT INTO `coin_transaction` VALUES (341, 1, 2, 945, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-14 22:54:56');
INSERT INTO `coin_transaction` VALUES (342, 1, 7, 952, 'add_signin', '每日签到奖励（连续3天）', NULL, '2026-05-15 08:37:37');
INSERT INTO `coin_transaction` VALUES (343, 1, -100, 852, 'use_betting', '幸运币押注 - 押大', NULL, '2026-05-15 10:04:45');
INSERT INTO `coin_transaction` VALUES (344, 1, -10, 842, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 10:05:24');
INSERT INTO `coin_transaction` VALUES (345, 1, 5, 847, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-15 10:05:24');
INSERT INTO `coin_transaction` VALUES (346, 1, -10, 837, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 13:25:37');
INSERT INTO `coin_transaction` VALUES (347, 1, 2, 839, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 13:25:37');
INSERT INTO `coin_transaction` VALUES (348, 1, -10, 829, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:04');
INSERT INTO `coin_transaction` VALUES (349, 1, 10, 839, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-15 15:08:04');
INSERT INTO `coin_transaction` VALUES (350, 1, -10, 829, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:11');
INSERT INTO `coin_transaction` VALUES (351, 1, 2, 831, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:08:11');
INSERT INTO `coin_transaction` VALUES (352, 1, -10, 821, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:17');
INSERT INTO `coin_transaction` VALUES (353, 1, 10, 831, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-15 15:08:17');
INSERT INTO `coin_transaction` VALUES (354, 1, -10, 821, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:23');
INSERT INTO `coin_transaction` VALUES (355, 1, -10, 811, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:29');
INSERT INTO `coin_transaction` VALUES (356, 1, 5, 816, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-15 15:08:29');
INSERT INTO `coin_transaction` VALUES (357, 1, -10, 806, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:34');
INSERT INTO `coin_transaction` VALUES (358, 1, 2, 808, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:08:34');
INSERT INTO `coin_transaction` VALUES (359, 1, -10, 798, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:08:55');
INSERT INTO `coin_transaction` VALUES (360, 1, -10, 788, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:09:00');
INSERT INTO `coin_transaction` VALUES (361, 1, 2, 790, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:09:00');
INSERT INTO `coin_transaction` VALUES (362, 1, -10, 780, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:09:06');
INSERT INTO `coin_transaction` VALUES (363, 1, 5, 785, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-15 15:09:06');
INSERT INTO `coin_transaction` VALUES (364, 1, -10, 775, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:09:11');
INSERT INTO `coin_transaction` VALUES (365, 1, 50, 825, 'add_lottery', '转盘抽奖获得：一等奖：50爱心币', NULL, '2026-05-15 15:09:11');
INSERT INTO `coin_transaction` VALUES (366, 1, -30, 795, 'use_blind_box', '盲盒点单费用', NULL, '2026-05-15 15:09:20');
INSERT INTO `coin_transaction` VALUES (367, 1, -30, 765, 'use_blind_box', '盲盒点单费用', NULL, '2026-05-15 15:09:27');
INSERT INTO `coin_transaction` VALUES (368, 1, -30, 735, 'use_blind_box', '盲盒点单费用', NULL, '2026-05-15 15:09:31');
INSERT INTO `coin_transaction` VALUES (369, 1, -10, 725, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:09:52');
INSERT INTO `coin_transaction` VALUES (370, 1, 2, 727, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:09:52');
INSERT INTO `coin_transaction` VALUES (371, 1, -10, 717, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:09:57');
INSERT INTO `coin_transaction` VALUES (372, 1, -10, 707, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:10:03');
INSERT INTO `coin_transaction` VALUES (373, 1, 2, 709, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:10:03');
INSERT INTO `coin_transaction` VALUES (374, 1, -10, 699, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:10:09');
INSERT INTO `coin_transaction` VALUES (375, 1, 20, 719, 'add_lottery', '转盘抽奖获得：二等奖：20爱心币', NULL, '2026-05-15 15:10:09');
INSERT INTO `coin_transaction` VALUES (376, 1, -10, 709, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:10:14');
INSERT INTO `coin_transaction` VALUES (377, 1, 10, 719, 'add_lottery', '转盘抽奖获得：三等奖：10爱心币', NULL, '2026-05-15 15:10:14');
INSERT INTO `coin_transaction` VALUES (378, 1, -10, 709, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:10:20');
INSERT INTO `coin_transaction` VALUES (379, 1, 2, 711, 'add_lottery', '转盘抽奖获得：安慰奖：2爱心币', NULL, '2026-05-15 15:10:20');
INSERT INTO `coin_transaction` VALUES (380, 1, -10, 701, 'use_lottery', '转盘抽奖消耗', NULL, '2026-05-15 15:10:25');
INSERT INTO `coin_transaction` VALUES (381, 1, 5, 706, 'add_lottery', '转盘抽奖获得：小奖：5爱心币', NULL, '2026-05-15 15:10:25');

-- ----------------------------
-- Table structure for daily_checkin
-- ----------------------------
DROP TABLE IF EXISTS `daily_checkin`;
CREATE TABLE `daily_checkin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `checkin_date` date NOT NULL COMMENT '签到日期',
  `continuous_days` int NULL DEFAULT 1 COMMENT '连续签到天数',
  `coins_reward` int NULL DEFAULT 5 COMMENT '获得爱心币奖励',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `checkin_date` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_date`(`checkin_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日签到记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of daily_checkin
-- ----------------------------
INSERT INTO `daily_checkin` VALUES (1, 1, '2026-05-13', 1, 5, NULL);
INSERT INTO `daily_checkin` VALUES (2, 1, '2026-05-14', 2, 5, NULL);
INSERT INTO `daily_checkin` VALUES (3, 1, '2026-05-15', 3, 7, NULL);

-- ----------------------------
-- Table structure for dish
-- ----------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜品名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜品描述',
  `image_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL',
  `price_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '爱心币' COMMENT '价格类型：爱心币/抱抱/亲亲等',
  `price` int NULL DEFAULT 1 COMMENT '价格数量',
  `is_signature` tinyint NULL DEFAULT 0 COMMENT '是否拿手菜',
  `is_new` tinyint NULL DEFAULT 0 COMMENT '是否新品',
  `is_recommended` tinyint NULL DEFAULT 0 COMMENT '是否今日推荐',
  `is_sold_out` tinyint NULL DEFAULT 0 COMMENT '是否售罄',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `blind_box_count` int NULL DEFAULT 0 COMMENT '被盲盒抽中次数',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_recommended`(`is_recommended` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 74 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish
-- ----------------------------
INSERT INTO `dish` VALUES (1, 1, '京酱肉丝', '酱汁浓郁，肉质Q弹', '', '亲亲', 3, 1, 0, 1, 0, '拿手菜,经典', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (2, 1, '糖醋排骨', '酸甜可口，外酥里嫩', NULL, '抱抱', 2, 1, 0, 0, 0, '拿手菜', 0, '2026-05-13 15:00:33', '2026-05-13 15:00:33', 0);
INSERT INTO `dish` VALUES (3, 1, '可乐鸡翅', '甜香入味，小朋友最爱', NULL, '亲亲', 3, 0, 0, 1, 0, '新品,推荐', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (4, 2, '意大利面', '番茄肉酱，浓郁美味', '', '爱心币', 2, 0, 1, 0, 0, '', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (5, 2, '牛排', '七分熟，配黑椒汁', NULL, '抱抱', 5, 1, 0, 0, 1, '拿手菜,大餐', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (6, 3, '提拉米苏', '甜蜜浪漫，入口即化', NULL, '亲亲', 3, 1, 0, 1, 0, '拿手菜,推荐', 0, '2026-05-13 15:00:33', '2026-05-13 15:00:33', 0);
INSERT INTO `dish` VALUES (7, 3, '半熟芝士', '', '', '爱心币', 10, 0, 0, 1, 0, '', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (8, 4, '肥宅快乐水', '', '', '爱心币', 5, 0, 0, 1, 0, '推荐', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (9, 4, '红糖姜茶', '暖胃暖心，女生必备', NULL, '爱心币', 1, 0, 0, 0, 0, '养生,热饮', 0, '2026-05-13 15:00:33', '2026-05-13 15:00:33', 0);
INSERT INTO `dish` VALUES (10, 5, '草莓', '新鲜草莓，甜蜜多汁', '', '亲亲', 2, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (11, 5, '西瓜切块', '清凉解暑，夏日必备', '', '爱心币', 8, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:33', NULL, 0);
INSERT INTO `dish` VALUES (12, 1, '宫保鸡丁', '经典川菜，香辣可口', NULL, '爱心币', 2, 0, 0, 0, 0, '川菜,微辣', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (13, 1, '鱼香肉丝', '酸甜适中，下饭神器', NULL, '抱抱', 2, 0, 0, 0, 0, '川菜', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (14, 1, '麻婆豆腐', '麻辣鲜香，入口即化', NULL, '亲亲', 1, 1, 0, 0, 0, '拿手菜,川菜,微辣', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (15, 1, '辣椒炒肉', '香辣入味，肉嫩椒脆', '', '爱心币', 3, 0, 0, 0, 0, '川菜', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (16, 1, '番茄炒蛋', '家常美味，营养均衡', NULL, '抱抱', 1, 0, 0, 1, 0, '推荐,清淡', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (17, 1, '酸辣土豆丝', '爽脆可口，开胃下饭', NULL, '爱心币', 1, 0, 0, 0, 0, '素菜,微辣', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (18, 2, '凯撒沙拉', '新鲜蔬菜，健康美味', NULL, '爱心币', 2, 0, 0, 0, 0, '沙拉,健康', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (19, 2, '奶油蘑菇汤', '浓郁香滑，温暖人心', NULL, '抱抱', 2, 0, 0, 1, 0, '推荐,热汤', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (20, 2, '烤鸡翅', '外酥里嫩，汁多味美', NULL, '亲亲', 2, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (21, 2, '炸食拼盘', '外酥内软，花样丰富', '', '爱心币', 30, 1, 0, 0, 0, '拿手菜', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (22, 2, '法式吐司', '香甜松软，早餐首选', NULL, '抱抱', 2, 0, 0, 0, 0, '早餐', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (23, 3, '奶油泡芙', '', '', '亲亲', 2, 0, 0, 1, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (24, 3, '肉松小贝', '绵软咸香，酥润清甜', '', '爱心币', 30, 0, 0, 1, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (25, 3, '双皮奶', '嫩滑爽口，传统甜品', NULL, '抱抱', 2, 0, 0, 0, 0, '中式甜品', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (26, 3, '草莓小蛋糕', '清新甜美', '', '爱心币', 20, 0, 0, 1, 0, '推荐,港式', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (27, 3, '蛋挞', '酥脆香甜，一口一个', '', '爱心币', 10, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (28, 4, '蜜雪柠檬水', '清新解腻，美容养颜', '', '爱心币', 1, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (29, 4, '瑞幸拿铁', '清凉爽口，提神抗疲劳', '', '抱抱', 2, 0, 0, 0, 0, '冷饮', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (30, 4, '抹茶星冰乐', '口感清新，甜度适中', '', '亲亲', 2, 0, 0, 0, 0, '热饮', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (31, 4, '鲜榨橙汁', '维C满满，健康天然', NULL, '爱心币', 2, 0, 0, 1, 0, '推荐,果汁', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (32, 4, '玫瑰花茶', '花香四溢，舒缓心情', NULL, '抱抱', 1, 0, 0, 0, 0, '花茶,养生', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (33, 5, '哈密瓜切块', '香甜多汁，清爽可口', '', '爱心币', 10, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (34, 5, '葡萄', '颗颗饱满，甜度爆表', '', '抱抱', 2, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (35, 5, '火龙果', '色彩鲜艳，营养丰富', '', '亲亲', 2, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (36, 5, '橙子切片', '维C丰富，清新爽口', '', '爱心币', 10, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (37, 5, '蓝莓', '抗氧化神器，护眼佳品', '', '抱抱', 3, 0, 0, 1, 0, '推荐,进口', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (38, 6, '薯片', '咔嚓脆响，追剧必备', '', '爱心币', 5, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (39, 6, '巧克力', '能量补充，甜蜜时刻', '', '爱心币', 5, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (40, 6, '果脯', '蔓越莓干，葡萄干，杏干，香蕉干', '', '爱心币', 20, 0, 0, 0, 0, '健康', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (41, 6, '奥利奥', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (42, 6, '果冻布丁', 'Q弹可爱，小朋友最爱', '', '爱心币', 10, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (43, 4, '微醺小酒', '', '', '爱心币', 20, 0, 0, 0, 0, '', 0, '2026-05-13 15:00:44', NULL, 0);
INSERT INTO `dish` VALUES (44, 4, '姜汁可乐', '驱寒暖身，缓解不适', NULL, '抱抱', 1, 0, 0, 0, 0, '热饮,生理期推荐', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (45, 4, '热牛奶', '安神助眠，补充钙质', NULL, '亲亲', 1, 0, 0, 0, 0, '热饮,生理期推荐', 0, '2026-05-13 15:00:44', '2026-05-13 15:00:44', 0);
INSERT INTO `dish` VALUES (46, 1, '香辣虾', '鲜辣醇厚，酥嫩兼备', '', '爱心币', 99, 1, 0, 1, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (47, 1, '宝宝鸡蛋羹', '', '', '爱心币', 10, 0, 0, 1, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (48, 1, '虎皮尖椒', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (49, 1, '粉丝娃娃菜', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (50, 6, '辣条', '', '', '爱心币', 5, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (51, 5, '荔枝', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (53, 1, '手撕包菜', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (54, 1, '青椒炒鸡蛋', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (55, 1, '炒西葫芦', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (56, 1, '香椿鸡蛋', '', '', '爱心币', 20, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (57, 1, '干锅土豆', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (58, 1, '蒜苔鸡蛋', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (59, 1, '煎饺子', '', '', '爱心币', 30, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (60, 1, '紫菜蛋花汤', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (61, 1, '火锅', '', '', '爱心币', 100, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (62, 1, '火腿拼盘', '', '', '爱心币', 20, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (63, 1, '凉拌菜', '', '', '爱心币', 1, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (64, 5, '香蕉', '', '', '爱心币', 3, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (65, 1, '红油桃', '', '', '爱心币', 1, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (66, 1, '炒饼', '', '', '爱心币', 10, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (67, 1, '蛋炒饭', '', 'http://47.102.84.75:9000/dishes/0c1f4261-2267-4f58-80d4-d42b84e7fcb0.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260514%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260514T105809Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=365a7f8ed1c2770b3bbe96e3228795e50ac4c17bc60bddc754f21def952caa42', '爱心币', 15, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (68, 5, '菠萝', '', 'http://47.102.84.75:9000/dishes/12e13318-289f-4bc8-91f4-d88c903c1a33.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260515%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260515T024127Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=a86f895833f33cddcc23d2b49e8058625b90903ed4a20c6f1e49d10b753ca20a', '爱心币', 5, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (69, 5, '梨', '', '', '爱心币', 5, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (70, 1, '西兰花虾仁', '', '', '爱心币', 20, 0, 0, 0, 0, '', 0, NULL, NULL, 0);
INSERT INTO `dish` VALUES (71, 1, '方便面荷包蛋', '', '', '爱心币', 5, 0, 0, 0, 0, '', 0, NULL, NULL, 0);

-- ----------------------------
-- Table structure for dish_dislike
-- ----------------------------
DROP TABLE IF EXISTS `dish_dislike`;
CREATE TABLE `dish_dislike`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_dish`(`user_id` ASC, `dish_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_dish`(`dish_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户不喜欢菜品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_dislike
-- ----------------------------

-- ----------------------------
-- Table structure for dish_favorite
-- ----------------------------
DROP TABLE IF EXISTS `dish_favorite`;
CREATE TABLE `dish_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_dish`(`user_id` ASC, `dish_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_dish`(`dish_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜品收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dish_favorite
-- ----------------------------

-- ----------------------------
-- Table structure for feeding_game_record
-- ----------------------------
DROP TABLE IF EXISTS `feeding_game_record`;
CREATE TABLE `feeding_game_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `score` int NOT NULL DEFAULT 0 COMMENT '游戏得分',
  `duration` int NOT NULL DEFAULT 0 COMMENT '游戏时长（秒）',
  `food_count` int NOT NULL DEFAULT 0 COMMENT '吃到的食物数量',
  `dark_food_count` int NOT NULL DEFAULT 0 COMMENT '吃到的黑暗料理数量',
  `unlocked_effect` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解锁的特殊效果',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '游戏时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  INDEX `idx_score`(`score` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '喂食大作战游戏记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feeding_game_record
-- ----------------------------
INSERT INTO `feeding_game_record` VALUES (1, 1, 375, 30, 39, 3, '💫 钻石级美食家 - 解锁星光闪烁', NULL);
INSERT INTO `feeding_game_record` VALUES (2, 1, 315, 30, 32, 1, '💫 钻石级美食家 - 解锁星光闪烁', NULL);

-- ----------------------------
-- Table structure for lottery_prize
-- ----------------------------
DROP TABLE IF EXISTS `lottery_prize`;
CREATE TABLE `lottery_prize`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '奖品ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '奖品描述',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标emoji',
  `prize_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'coins' COMMENT '奖品类型：coins/blank/thanks',
  `value` int NULL DEFAULT 0 COMMENT '奖品值（爱心币数量或谢谢惠顾）',
  `probability` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '中奖概率（百分比）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '转盘奖品配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lottery_prize
-- ----------------------------
INSERT INTO `lottery_prize` VALUES (1, '特等奖：100爱心币', '运气爆棚！获得100个爱心币', '🏆', 'coins', 100, 1.25, 1, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (2, '一等奖：50爱心币', '太棒了！获得50个爱心币', '🥇', 'coins', 50, 3.75, 2, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (3, '二等奖：20爱心币', '恭喜！获得20个爱心币', '🥈', 'coins', 20, 9.75, 3, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (4, '三等奖：10爱心币', '不错哦！获得10个爱心币', '🥉', 'coins', 10, 17.75, 4, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (5, '小奖：5爱心币', '加油！获得5个爱心币', '🎁', 'coins', 5, 27.75, 5, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (6, '安慰奖：2爱心币', '继续努力！获得2个爱心币', '💝', 'coins', 2, 34.25, 6, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');
INSERT INTO `lottery_prize` VALUES (7, '谢谢惠顾', '很遗憾，下次再来吧', '😊', 'thanks', 0, 5.50, 7, 1, '2026-05-13 15:00:33', '2026-05-15 14:59:29');

-- ----------------------------
-- Table structure for lottery_record
-- ----------------------------
DROP TABLE IF EXISTS `lottery_record`;
CREATE TABLE `lottery_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `prize_id` bigint NOT NULL COMMENT '奖品ID',
  `prize_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品名称（冗余）',
  `prize_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '奖品类型',
  `prize_value` int NULL DEFAULT 0 COMMENT '奖品值',
  `cost_coins` int NULL DEFAULT 10 COMMENT '消耗爱心币数量',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 195 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抽奖记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of lottery_record
-- ----------------------------
INSERT INTO `lottery_record` VALUES (1, 1, 11, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (2, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (3, 1, 10, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (4, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (5, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (6, 1, 9, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (7, 1, 10, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (8, 1, 10, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (9, 1, 11, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (10, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (11, 1, 10, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (12, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (13, 1, 11, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (14, 1, 11, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (15, 1, 8, '特等奖：100爱心币', 'coins', 100, 10, NULL);
INSERT INTO `lottery_record` VALUES (16, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (17, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (18, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (19, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (20, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (21, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (22, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (23, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (24, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (25, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (26, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (27, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (28, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (29, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (30, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (31, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (32, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (33, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (34, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (35, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (36, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (37, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (38, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (39, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (40, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (41, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (42, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (43, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (44, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (45, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (46, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (47, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (48, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (49, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (50, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (51, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (52, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (53, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (54, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (55, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (56, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (57, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (58, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (59, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (60, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (61, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (62, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (63, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (64, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (65, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (66, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (67, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (68, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (69, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (70, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (71, 1, 3, '二等奖：30爱心币', 'coins', 30, 10, NULL);
INSERT INTO `lottery_record` VALUES (72, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (73, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (74, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (75, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (76, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (77, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (78, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (79, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (80, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (81, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (82, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (83, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (84, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (85, 1, 4, '三等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (86, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (87, 1, 6, '安慰奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (88, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (89, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (90, 1, 5, '小奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (91, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (92, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (93, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (94, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (95, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (96, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (97, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (98, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (99, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (100, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (101, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (102, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (103, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (104, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (105, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (106, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (107, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (108, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (109, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (110, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (111, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (112, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (113, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (114, 1, 3, '二等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (115, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (116, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (117, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (118, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (119, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (120, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (121, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (122, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (123, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (124, 1, 3, '二等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (125, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (126, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (127, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (128, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (129, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (130, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (131, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (132, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (133, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (134, 1, 3, '二等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (135, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (136, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (137, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (138, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (139, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (140, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (141, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (142, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (143, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (144, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (145, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (146, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (147, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (148, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (149, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (150, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (151, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (152, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (153, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (154, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (155, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (156, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (157, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (158, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (159, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (160, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (161, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (162, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (163, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (164, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (165, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (166, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (167, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (168, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (169, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (170, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (171, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (172, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (173, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (174, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (175, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (176, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (177, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (178, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (179, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (180, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (181, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (182, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (183, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (184, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (185, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (186, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);
INSERT INTO `lottery_record` VALUES (187, 1, 2, '一等奖：50爱心币', 'coins', 50, 10, NULL);
INSERT INTO `lottery_record` VALUES (188, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (189, 1, 7, '谢谢惠顾', 'thanks', 0, 10, NULL);
INSERT INTO `lottery_record` VALUES (190, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (191, 1, 3, '二等奖：20爱心币', 'coins', 20, 10, NULL);
INSERT INTO `lottery_record` VALUES (192, 1, 4, '三等奖：10爱心币', 'coins', 10, 10, NULL);
INSERT INTO `lottery_record` VALUES (193, 1, 6, '安慰奖：2爱心币', 'coins', 2, 10, NULL);
INSERT INTO `lottery_record` VALUES (194, 1, 5, '小奖：5爱心币', 'coins', 5, 10, NULL);

-- ----------------------------
-- Table structure for notebook
-- ----------------------------
DROP TABLE IF EXISTS `notebook`;
CREATE TABLE `notebook`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '内容',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'default' COMMENT '分类标签',
  `is_pinned` tinyint NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_pinned`(`is_pinned` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '记事本表 - 一二的小本本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notebook
-- ----------------------------

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `module` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作类型（新增/修改/删除/查询等）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法（类名.方法名）',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法（GET/POST/PUT/DELETE）',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应结果',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `cost_time` bigint NULL DEFAULT NULL COMMENT '操作耗时（毫秒）',
  `status` tinyint NULL DEFAULT 1 COMMENT '操作状态（0-失败 1-成功）',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `operation_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_module`(`module` ASC) USING BTREE,
  INDEX `idx_operation_type`(`operation_type` ASC) USING BTREE,
  INDEX `idx_operation_time`(`operation_time` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_method`(`method`(100) ASC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `operation_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, NULL, NULL, '菜品管理', '新增', '添加新菜品', 'com.wrt.excel.order.controller.DishController.addDish', '/api/dish/add', 'POST', '{\"dish\":{\"id\":null,\"categoryId\":1,\"name\":\"测试菜品4\",\"description\":\"5\",\"imageUrl\":\"http://47.102.84.75:9000/dishes/eca36d30-95c0-4db8-b10a-3717562c66cc.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20260514%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20260514T054521Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=3d7fd7b038fd839514a80de2126a8928af29f462e548aec5faacaee4bb107c87\",\"priceType\":\"爱心币\",\"price\":1,\"isSignature\":0,\"isNew\":0,\"isRecommended\":0,\"isSoldOut\":0,\"tags\":\"\",\"sortOrder\":null,\"createdAt\":null,\"updatedAt\":null}}', NULL, '0:0:0:0:0:0:0:1', 660, 1, NULL, '2026-05-14 13:45:24', '2026-05-14 13:45:24');
INSERT INTO `operation_log` VALUES (2, NULL, NULL, '菜品管理', '新增', '添加新菜品', 'com.wrt.excel.order.controller.DishController.addDish', '/api/dish/add', 'POST', '{\"dish\":{\"id\":null,\"categoryId\":1,\"name\":\"测试菜品5\",\"description\":\"6\",\"imageUrl\":\"\",\"priceType\":\"爱心币\",\"price\":1,\"isSignature\":0,\"isNew\":0,\"isRecommended\":0,\"isSoldOut\":0,\"tags\":\"\",\"sortOrder\":null,\"createdAt\":null,\"updatedAt\":null}}', NULL, '0:0:0:0:0:0:0:1', 87, 1, NULL, '2026-05-14 13:46:09', '2026-05-14 13:46:09');
INSERT INTO `operation_log` VALUES (3, NULL, NULL, '菜品管理', '删除', '删除菜品', 'com.wrt.excel.order.controller.DishController.deleteDish', '/api/dish/delete/52', 'DELETE', '{\"id\":52}', NULL, '127.0.0.1', 240, 1, NULL, '2026-05-14 13:54:20', '2026-05-14 13:54:20');
INSERT INTO `operation_log` VALUES (4, NULL, NULL, '菜品管理', '删除', '删除菜品', 'com.wrt.excel.order.controller.DishController.deleteDish', '/api/dish/delete/73', 'DELETE', '{\"id\":73}', NULL, '218.12.21.86', 66, 1, NULL, '2026-05-14 14:00:24', '2026-05-14 14:00:24');
INSERT INTO `operation_log` VALUES (5, NULL, NULL, '菜品管理', '删除', '删除菜品', 'com.wrt.excel.order.controller.DishController.deleteDish', '/api/dish/delete/72', 'DELETE', '{\"id\":72}', NULL, '218.12.21.86', 9, 1, NULL, '2026-05-14 14:00:26', '2026-05-14 14:00:26');
INSERT INTO `operation_log` VALUES (6, NULL, NULL, '订单管理', '新增', '创建新订单', 'com.wrt.excel.order.controller.OrderController.createOrder', '/api/order/create', 'POST', '{\"request\":{\"userId\":1,\"items\":[{\"dishId\":67,\"quantity\":1,\"remark\":\"\"},{\"dishId\":60,\"quantity\":1,\"remark\":\"\"},{\"dishId\":68,\"quantity\":1,\"remark\":\"\"}],\"remark\":\"\"}}', NULL, '111.61.78.47', 2216, 1, NULL, '2026-05-14 15:13:41', '2026-05-14 15:13:41');
INSERT INTO `operation_log` VALUES (7, NULL, NULL, '订单管理', '新增', '创建新订单', 'com.wrt.excel.order.controller.OrderController.createOrder', '/api/order/create', 'POST', '{\"request\":{\"userId\":1,\"items\":[{\"dishId\":67,\"quantity\":1,\"remark\":\"\"},{\"dishId\":60,\"quantity\":1,\"remark\":\"\"},{\"dishId\":68,\"quantity\":1,\"remark\":\"\"}],\"remark\":\"\"}}', NULL, '111.61.78.47', 2142, 1, NULL, '2026-05-14 15:13:42', '2026-05-14 15:13:42');
INSERT INTO `operation_log` VALUES (8, NULL, NULL, '订单管理', '新增', '创建新订单', 'com.wrt.excel.order.controller.OrderController.createOrder', '/api/order/create', 'POST', '{\"request\":{\"userId\":1,\"items\":[{\"dishId\":67,\"quantity\":1,\"remark\":\"\"},{\"dishId\":60,\"quantity\":1,\"remark\":\"\"},{\"dishId\":68,\"quantity\":1,\"remark\":\"\"}],\"remark\":\"\"}}', NULL, '111.61.78.47', 2768, 1, NULL, '2026-05-14 15:13:44', '2026-05-14 15:13:44');
INSERT INTO `operation_log` VALUES (9, NULL, NULL, '订单管理', '新增', '创建新订单', 'com.wrt.excel.order.controller.OrderController.createOrder', '/api/order/create', 'POST', '{\"request\":{\"userId\":1,\"items\":[{\"dishId\":67,\"quantity\":1,\"remark\":\"\"},{\"dishId\":60,\"quantity\":1,\"remark\":\"\"},{\"dishId\":68,\"quantity\":1,\"remark\":\"\"}],\"remark\":\"\"}}', NULL, '111.61.78.47', 4585, 1, NULL, '2026-05-14 15:13:46', '2026-05-14 15:13:46');
INSERT INTO `operation_log` VALUES (10, NULL, NULL, '订单管理', '修改', '取消订单', 'com.wrt.excel.order.controller.OrderController.cancelOrder', '/api/order/cancel', 'POST', '{\"params\":{\"orderNo\":\"LOVE20260514151341084e4e8a\",\"userId\":1}}', NULL, '111.61.78.47', 2001, 1, NULL, '2026-05-14 15:32:04', '2026-05-14 15:32:04');
INSERT INTO `operation_log` VALUES (11, NULL, NULL, '订单管理', '修改', '取消订单', 'com.wrt.excel.order.controller.OrderController.cancelOrder', '/api/order/cancel', 'POST', '{\"params\":{\"orderNo\":\"LOVE202605141513419086eaaa\",\"userId\":1}}', NULL, '111.61.78.47', 1119, 1, NULL, '2026-05-14 15:32:20', '2026-05-14 15:32:20');
INSERT INTO `operation_log` VALUES (12, NULL, NULL, '订单管理', '修改', '取消订单', 'com.wrt.excel.order.controller.OrderController.cancelOrder', '/api/order/cancel', 'POST', '{\"params\":{\"orderNo\":\"LOVE2026051415134016f931de\",\"userId\":1}}', NULL, '111.61.78.47', 1050, 1, NULL, '2026-05-14 15:32:24', '2026-05-14 15:32:24');
INSERT INTO `operation_log` VALUES (13, NULL, NULL, '订单管理', '修改', '取消订单', 'com.wrt.excel.order.controller.OrderController.cancelOrder', '/api/order/cancel', 'POST', '{\"params\":{\"orderNo\":\"LOVE20260514151338479b1873\",\"userId\":1}}', NULL, '111.61.78.47', 1068, 1, NULL, '2026-05-14 15:32:29', '2026-05-14 15:32:29');
INSERT INTO `operation_log` VALUES (14, NULL, NULL, '订单管理', '新增', '创建新订单', 'com.wrt.excel.order.controller.OrderController.createOrder', '/api/order/create', 'POST', '{\"request\":{\"userId\":1,\"items\":[{\"dishId\":67,\"quantity\":1,\"remark\":\"\"},{\"dishId\":60,\"quantity\":1,\"remark\":\"\"},{\"dishId\":68,\"quantity\":1,\"remark\":\"\"}],\"remark\":\"\"}}', NULL, '111.61.78.47', 1475, 1, NULL, '2026-05-14 15:33:21', '2026-05-14 15:33:21');
INSERT INTO `operation_log` VALUES (15, NULL, NULL, '订单管理', '修改', '更新订单状态', 'com.wrt.excel.order.controller.OrderController.updateOrderStatus', '/api/order/status', 'PUT', '{\"orderNo\":\"LOVE20260514153319a5d288ce\",\"status\":\"received\"}', NULL, '110.249.179.146', 1331, 1, NULL, '2026-05-14 16:10:04', '2026-05-14 16:10:04');
INSERT INTO `operation_log` VALUES (16, NULL, NULL, '订单管理', '修改', '更新订单状态', 'com.wrt.excel.order.controller.OrderController.updateOrderStatus', '/api/order/status', 'PUT', '{\"orderNo\":\"LOVE20260514153319a5d288ce\",\"status\":\"cooking\"}', NULL, '110.241.18.7', 1380, 1, NULL, '2026-05-14 19:00:33', '2026-05-14 19:00:33');
INSERT INTO `operation_log` VALUES (17, NULL, NULL, '订单管理', '修改', '更新订单状态', 'com.wrt.excel.order.controller.OrderController.updateOrderStatus', '/api/order/status', 'PUT', '{\"orderNo\":\"LOVE20260514153319a5d288ce\",\"status\":\"delivered\"}', NULL, '110.241.18.7', 1149, 1, NULL, '2026-05-14 19:00:37', '2026-05-14 19:00:37');
INSERT INTO `operation_log` VALUES (18, NULL, NULL, '订单管理', '修改', '更新订单状态', 'com.wrt.excel.order.controller.OrderController.updateOrderStatus', '/api/order/status', 'PUT', '{\"orderNo\":\"LOVE20260514153319a5d288ce\",\"status\":\"completed\"}', NULL, '110.241.18.7', 1284, 1, NULL, '2026-05-14 22:58:50', '2026-05-14 22:58:50');

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `dish_id` bigint NOT NULL COMMENT '菜品ID',
  `dish_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜品名称（冗余）',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `price` int NOT NULL DEFAULT 1 COMMENT '单价',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注（少冰、不放香菜等）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------
INSERT INTO `order_item` VALUES (7, 5, 67, '蛋炒饭', 1, 15, '', '2026-05-14 15:13:39');
INSERT INTO `order_item` VALUES (8, 5, 60, '紫菜蛋花汤', 1, 10, '', '2026-05-14 15:13:39');
INSERT INTO `order_item` VALUES (9, 5, 68, '菠萝', 1, 5, '', '2026-05-14 15:13:39');
INSERT INTO `order_item` VALUES (10, 6, 67, '蛋炒饭', 1, 15, '', '2026-05-14 15:13:41');
INSERT INTO `order_item` VALUES (11, 6, 60, '紫菜蛋花汤', 1, 10, '', '2026-05-14 15:13:41');
INSERT INTO `order_item` VALUES (12, 6, 68, '菠萝', 1, 5, '', '2026-05-14 15:13:41');
INSERT INTO `order_item` VALUES (13, 7, 67, '蛋炒饭', 1, 15, '', '2026-05-14 15:13:42');
INSERT INTO `order_item` VALUES (14, 7, 60, '紫菜蛋花汤', 1, 10, '', '2026-05-14 15:13:42');
INSERT INTO `order_item` VALUES (15, 7, 68, '菠萝', 1, 5, '', '2026-05-14 15:13:43');
INSERT INTO `order_item` VALUES (16, 8, 67, '蛋炒饭', 1, 15, '', '2026-05-14 15:13:44');
INSERT INTO `order_item` VALUES (17, 8, 60, '紫菜蛋花汤', 1, 10, '', '2026-05-14 15:13:44');
INSERT INTO `order_item` VALUES (18, 8, 68, '菠萝', 1, 5, '', '2026-05-14 15:13:44');
INSERT INTO `order_item` VALUES (19, 9, 67, '蛋炒饭', 1, 15, '', '2026-05-14 15:33:20');
INSERT INTO `order_item` VALUES (20, 9, 60, '紫菜蛋花汤', 1, 10, '', '2026-05-14 15:33:20');
INSERT INTO `order_item` VALUES (21, 9, 68, '菠萝', 1, 5, '', '2026-05-14 15:33:20');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_price` int NULL DEFAULT 0 COMMENT '总价格',
  `price_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '爱心币' COMMENT '价格类型',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '状态：pending/received/cooking/delivered/completed',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `special_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '特殊消息（如生日祝福）',
  `scheduled_time` datetime NULL DEFAULT NULL COMMENT '预约时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_created`(`created_at` ASC) USING BTREE,
  INDEX `idx_scheduled_time`(`scheduled_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (5, 'LOVE20260514151338479b1873', 1, 30, '爱心币', 'cancelled', '', NULL, NULL, '2026-05-14 15:13:39', '2026-05-14 15:32:28');
INSERT INTO `orders` VALUES (6, 'LOVE2026051415134016f931de', 1, 30, '爱心币', 'cancelled', '', NULL, NULL, '2026-05-14 15:13:40', '2026-05-14 15:32:23');
INSERT INTO `orders` VALUES (7, 'LOVE20260514151341084e4e8a', 1, 30, '爱心币', 'cancelled', '', NULL, NULL, '2026-05-14 15:13:41', '2026-05-14 15:32:02');
INSERT INTO `orders` VALUES (8, 'LOVE202605141513419086eaaa', 1, 30, '爱心币', 'cancelled', '', NULL, NULL, '2026-05-14 15:13:41', '2026-05-14 15:32:19');
INSERT INTO `orders` VALUES (9, 'LOVE20260514153319a5d288ce', 1, 30, '爱心币', 'completed', '', NULL, NULL, '2026-05-14 15:33:20', '2026-05-14 22:58:49');

-- ----------------------------
-- Table structure for statistics
-- ----------------------------
DROP TABLE IF EXISTS `statistics`;
CREATE TABLE `statistics`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `order_count` int NULL DEFAULT 0 COMMENT '订单数',
  `favorite_dish_id` bigint NULL DEFAULT NULL COMMENT '最爱菜品ID',
  `favorite_time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最爱时间段：morning/afternoon/evening/night',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of statistics
-- ----------------------------

-- ----------------------------
-- Table structure for todo_item
-- ----------------------------
DROP TABLE IF EXISTS `todo_item`;
CREATE TABLE `todo_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细描述',
  `priority` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'medium' COMMENT '优先级：high/medium/low',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '状态：pending/in_progress/completed',
  `assignee` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人：布布/佳宝/一起',
  `due_date` date NULL DEFAULT NULL COMMENT '截止日期',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reminder_enabled` tinyint NULL DEFAULT 0 COMMENT '是否开启提醒',
  `reminder_time` datetime NULL DEFAULT NULL COMMENT '提醒时间',
  `reminder_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '提醒状态：pending/sent/cancelled',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_due_date`(`due_date` ASC) USING BTREE,
  INDEX `idx_reminder_status`(`reminder_status` ASC) USING BTREE,
  INDEX `idx_reminder_time`(`reminder_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '家庭待办事项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of todo_item
-- ----------------------------
INSERT INTO `todo_item` VALUES (3, 1, '看电影', '看最新的爱情电影', 'low', 'pending', '佳宝', '2026-05-20', NULL, '2026-05-13 15:02:33', '2026-05-13 15:02:33', 0, '2026-05-14 14:57:46', 'sent');
INSERT INTO `todo_item` VALUES (4, 1, '洗小瑞车', '晚上8点出门洗车\n', 'high', 'completed', '一起', '2026-05-14', '2026-05-14 20:51:02', NULL, NULL, 0, '2026-05-14 18:57:25', 'sent');
INSERT INTO `todo_item` VALUES (5, 1, '刷鞋', '晚上10点刷鞋', 'medium', 'pending', '布布', '2026-05-16', NULL, NULL, NULL, 0, NULL, 'pending');
INSERT INTO `todo_item` VALUES (6, 1, '体验采摘乐趣，摘樱桃', '天气不下雨的情况下', 'medium', 'pending', '一起', '2026-05-17', NULL, NULL, NULL, 0, NULL, 'pending');
INSERT INTO `todo_item` VALUES (7, 1, '拍一周年红底照', '', 'high', 'pending', '一起', '2026-05-23', NULL, NULL, NULL, 0, NULL, 'pending');
INSERT INTO `todo_item` VALUES (8, 1, '取花', '下午三点到', 'high', 'pending', '一起', '2026-05-16', NULL, NULL, NULL, 0, NULL, 'pending');
INSERT INTO `todo_item` VALUES (9, 1, '带上散热板', '早上上班的时候，装上散热板', 'low', 'pending', '布布', '2026-05-18', NULL, NULL, NULL, 0, NULL, 'pending');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '公主' COMMENT '昵称',
  `avatar_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `theme_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#FFB6C1' COMMENT '主题颜色',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码（加密存储）',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户，ADMIN-管理员',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `period_start_date` date NULL DEFAULT NULL COMMENT '生理期开始日期',
  `period_cycle_days` int NULL DEFAULT 28 COMMENT '生理期周期天数',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '佳宝', NULL, '#FFB6C1', '2000-01-01', '17743761205', 'e10adc3949ba59abbe56e057f20f883e', 'ADMIN', '2026-05-15 15:07:17', NULL, 28, '2026-05-13 15:00:33', NULL);
INSERT INTO `user` VALUES (2, '管理员', NULL, '#FFB6C1', NULL, '18233008281', 'e10adc3949ba59abbe56e057f20f883e', 'ADMIN', '2026-05-15 15:20:56', NULL, 28, '2026-05-15 11:14:00', NULL);

-- ----------------------------
-- Table structure for user_achievement
-- ----------------------------
DROP TABLE IF EXISTS `user_achievement`;
CREATE TABLE `user_achievement`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `achievement_id` bigint NOT NULL COMMENT '成就ID',
  `unlocked_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  `is_unlocked` tinyint NULL DEFAULT 0 COMMENT '是否已解锁',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_achievement`(`user_id` ASC, `achievement_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户成就表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_achievement
-- ----------------------------

-- ----------------------------
-- Table structure for user_daily_betting_stats
-- ----------------------------
DROP TABLE IF EXISTS `user_daily_betting_stats`;
CREATE TABLE `user_daily_betting_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bet_date` date NOT NULL COMMENT '押注日期',
  `total_bets` int NULL DEFAULT 0 COMMENT '总押注次数',
  `win_count` int NULL DEFAULT 0 COMMENT '获胜次数',
  `loss_count` int NULL DEFAULT 0 COMMENT '失败次数',
  `total_bet_amount` int NULL DEFAULT 0 COMMENT '总下注金额',
  `total_reward` int NULL DEFAULT 0 COMMENT '总奖励金额',
  `total_loss` int NULL DEFAULT 0 COMMENT '总损失金额',
  `consecutive_loss` int NULL DEFAULT 0 COMMENT '当前连续输的次数',
  `is_locked` tinyint NULL DEFAULT 0 COMMENT '是否被锁定：0-未锁定，1-已锁定',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `bet_date` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_date`(`bet_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户每日押注统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_daily_betting_stats
-- ----------------------------
INSERT INTO `user_daily_betting_stats` VALUES (1, 1, '2026-05-14', 3, 1, 2, 45, 27, 30, 1, 0, NULL, NULL);
INSERT INTO `user_daily_betting_stats` VALUES (2, 1, '2026-05-15', 1, 0, 1, 100, 0, 100, 1, 1, NULL, NULL);

-- ----------------------------
-- Table structure for user_online_status
-- ----------------------------
DROP TABLE IF EXISTS `user_online_status`;
CREATE TABLE `user_online_status`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话ID',
  `is_online` tinyint NULL DEFAULT 0 COMMENT '是否在线：0-离线，1-在线',
  `last_active_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  `login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  `logout_time` datetime NULL DEFAULT NULL COMMENT '登出时间',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_session`(`user_id` ASC, `session_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_online`(`is_online` ASC) USING BTREE,
  INDEX `idx_last_active`(`last_active_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户在线状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_online_status
-- ----------------------------
INSERT INTO `user_online_status` VALUES (1, 2, 'CFB1CFC7F95AC598484A3BCB5C33F795', 0, '2026-05-15 11:15:17', '2026-05-15 11:14:15', '2026-05-15 11:18:13', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (2, 2, '6A0D317D875A6CDE27C71128BB624505', 0, '2026-05-15 11:18:58', '2026-05-15 11:18:13', '2026-05-15 11:21:16', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (3, 2, '0989A9747C950A7B5FCFFD4D0D9968F8', 0, '2026-05-15 11:21:48', '2026-05-15 11:21:17', '2026-05-15 11:26:43', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (4, 2, '19F114CF7E1F22A2A4120FEF68832C0A', 0, '2026-05-15 11:27:03', '2026-05-15 11:26:43', '2026-05-15 11:27:07', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (5, 2, 'BB1007C821A834C87DFE65AE3199AD48', 0, '2026-05-15 11:40:13', '2026-05-15 11:36:28', '2026-05-15 11:43:17', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (6, 2, 'CBA744EE2931C22C82848AE89B28AD80', 0, '2026-05-15 11:43:22', '2026-05-15 11:43:20', '2026-05-15 13:29:38', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (7, 1, 'CBA744EE2931C22C82848AE89B28AD80', 0, '2026-05-15 11:44:04', '2026-05-15 11:44:04', '2026-05-15 13:30:30', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (11, 2, 'D684F6FDF98CD0ACE7280F60C528C092', 0, '2026-05-15 13:30:16', '2026-05-15 13:29:38', '2026-05-15 13:30:22', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (12, 1, '5D523EDDF2F026293CE97360B1832D05', 0, '2026-05-15 13:30:44', '2026-05-15 13:30:30', '2026-05-15 13:30:48', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (13, 1, '8DC65DDDDFB04853B00AB723F3866479', 0, '2026-05-15 13:45:24', '2026-05-15 13:39:16', '2026-05-15 13:52:49', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (14, 1, '0DB6E9F9AC3C9010FE39601111D0D736', 0, '2026-05-15 13:55:04', '2026-05-15 13:53:19', '2026-05-15 13:56:23', '110.249.179.146', NULL, NULL);
INSERT INTO `user_online_status` VALUES (15, 2, '8C31960820846823F1D2607E1A2550A6', 0, '2026-05-15 13:54:06', '2026-05-15 13:53:55', '2026-05-15 13:59:17', '218.12.19.212', NULL, NULL);
INSERT INTO `user_online_status` VALUES (16, 1, 'BF71C690A6A6A363A815753A6FB326D7', 0, '2026-05-15 13:56:34', '2026-05-15 13:56:23', '2026-05-15 14:01:53', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (17, 1, '0AE13BF91267B5E7B656274905702D92', 0, '2026-05-15 14:02:24', '2026-05-15 14:01:53', '2026-05-15 14:04:53', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (18, 1, '201D44A838BB50DA7C7CEA4A174CF74E', 0, '2026-05-15 14:05:01', '2026-05-15 14:04:54', '2026-05-15 14:12:47', '0:0:0:0:0:0:0:1', NULL, NULL);
INSERT INTO `user_online_status` VALUES (19, 1, 'B91F8054401652FEBCB3AB49297E2E76', 0, '2026-05-15 14:16:39', '2026-05-15 14:16:34', '2026-05-15 14:24:33', '110.249.179.146', NULL, NULL);
INSERT INTO `user_online_status` VALUES (20, 2, '5456030121D01D6E466210F4435B0DA0', 0, '2026-05-15 14:21:04', '2026-05-15 14:20:52', '2026-05-15 14:21:24', '218.12.19.212', NULL, NULL);
INSERT INTO `user_online_status` VALUES (21, 2, '283B1788AD57D7DFED54A04C50DC4731', 0, '2026-05-15 14:23:23', '2026-05-15 14:21:24', '2026-05-15 14:29:33', '218.12.19.212', NULL, NULL);
INSERT INTO `user_online_status` VALUES (22, 1, '775D2F1D8026913A3A5C52483E6A5872', 0, '2026-05-15 15:10:29', '2026-05-15 15:07:17', '2026-05-15 15:19:33', '111.61.78.47', NULL, NULL);
INSERT INTO `user_online_status` VALUES (23, 2, 'A716B80D6F349CEBD182CB45C2FB8412', 0, '2026-05-15 15:19:35', '2026-05-15 15:08:11', '2026-05-15 15:20:56', '110.249.179.146', NULL, NULL);
INSERT INTO `user_online_status` VALUES (24, 2, '84D9F3590180686C365A3DE8D0F6B081', 1, '2026-05-15 15:22:54', '2026-05-15 15:20:56', NULL, '0:0:0:0:0:0:0:1', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
