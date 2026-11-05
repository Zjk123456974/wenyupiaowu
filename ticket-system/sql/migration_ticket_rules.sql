-- ============================================================
-- 迁移脚本：活动票务规则增强（退票策略 / 实名模式 / 场馆地图 / 预约开关 / 注意事项）
-- 适用于 ticket_system 库（MySQL 8.x）。
-- 注意：本脚本为非幂等的一次性迁移，针对同一数据库只执行一次即可。
-- 执行建议：mysql --default-character-set=utf8mb4 -uroot -p < 本文件
-- ============================================================
SET NAMES utf8mb4;
USE ticket_system;

-- 1. activity 增加活动级票务规则字段
--    refund_policy       退票策略：1 可退票 / 0 不可退票（例如演唱会默认不可退）
--    real_name_mode      实名模式：0 免实名（可匿名）/ 1 一单一实名（一个实名可购多张，如电竞）/ 2 一票一实名（每张票都必须实名，如演唱会）
--    seat_map_enabled    场馆地图/座位定位：1 开启（购票后可直接定位到座位）/ 0 关闭
--    reservation_enabled 预约开关：1 开启（未开售可预约+倒计时提醒）/ 0 关闭
--    ticket_notice       活动注意事项（自由文本，页面“购票须知/注意事项”区展示）
ALTER TABLE `activity`
    ADD COLUMN `refund_policy` TINYINT NOT NULL DEFAULT 1 COMMENT '退票策略：1可退 0不可退' AFTER `sold_tickets`,
    ADD COLUMN `real_name_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '实名模式：0免实名 1一单一实名 2一票一实名' AFTER `refund_policy`,
    ADD COLUMN `seat_map_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '场馆地图座位定位：1开启 0关闭' AFTER `real_name_mode`,
    ADD COLUMN `reservation_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '预约开关：1开启 0关闭' AFTER `seat_map_enabled`,
    ADD COLUMN `ticket_notice` VARCHAR(1000) DEFAULT NULL COMMENT '活动注意事项' AFTER `description`;

-- 2. electronic_ticket 增加座位定位信息（场馆地图开启的场次生成具体座位号）
ALTER TABLE `electronic_ticket`
    ADD COLUMN `seat_info` VARCHAR(100) DEFAULT NULL COMMENT '座位定位信息（区/排/座）' AFTER `seat_type`;

-- 3. 存量示例活动按分类初始化默认票务规则：
--    演唱会(分类1) → 不可退 + 每票实名
--    体育赛事(分类4) → 可退 + 一单一实名
--    其余保持默认（可退 + 免实名）
UPDATE `activity`
   SET `refund_policy` = 0, `real_name_mode` = 2
 WHERE `category_id` = 1;

UPDATE `activity`
   SET `real_name_mode` = 1
 WHERE `category_id` = 4;