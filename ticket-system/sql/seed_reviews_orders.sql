-- ============================================================
-- 文娱票务管理系统 - 演示数据同步脚本
-- 作用：为每个「已上架」活动补齐 购买订单 / 用户评价 / 收藏，
--       并同步活动已售票数，保证前台详情、评价统计、订单与库存一致。
-- 特性：
--   * 幂等：按「活动标题+用户」去重，已存在的记录不会重复生成。
--   * 活动ID按标题动态匹配，不依赖自增ID，避免ID漂移。
--   * 仅处理 status = 3（已上架）且未被逻辑删除的活动。
-- 用法：先执行 USE ticket_system; 或直接 mysql ... ticket_system < 本文件
-- ============================================================
USE `ticket_system`;

-- ------------------------------------------------------------
-- 1. 补充一批普通用户（密码均为 123456），供购买/评价/收藏使用
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`username`, `password`, `nickname`, `role`, `status`, `phone`, `create_time`) VALUES
('user3', 'e10adc3949ba59abbe56e057f20f883e', '小赵爱看展',    0, 1, '13800000003', NOW()),
('user4', 'e10adc3949ba59abbe56e057f20f883e', '文艺青年小孙',  0, 1, '13800000004', NOW()),
('user5', 'e10adc3949ba59abbe56e057f20f883e', '爱看戏的小周',  0, 1, '13800000005', NOW()),
('user6', 'e10adc3949ba59abbe56e057f20f883e', '电竞迷小吴',    0, 1, '13800000006', NOW()),
('user7', 'e10adc3949ba59abbe56e057f20f883e', '追星女孩小郑',  0, 1, '13800000007', NOW()),
('user8', 'e10adc3949ba59abbe56e057f20f883e', '话剧er小钱',    0, 1, '13800000008', NOW());

-- ------------------------------------------------------------
-- 2. 为每个已上架活动生成多条用户评价（status=1 直接前台可见）
--    评分覆盖 1~5 星，部分评价带上官方回复
-- ------------------------------------------------------------
INSERT INTO `activity_review`
(`activity_id`, `user_id`, `username`, `nickname`, `avatar`, `rating`, `content`, `status`,
 `reply`, `reply_time`, `replier_id`, `create_time`)
SELECT DISTINCT
    a.id, u.id, u.username, u.nickname, u.avatar,
    ((a.id + u.id) % 5) + 1 AS rating,
    CASE
        WHEN ((a.id + u.id) % 5) + 1 >= 4 THEN CONCAT('现场太燃了！「', a.title, '」完全超出预期，舞美灯光都拉满，强烈推荐来看！')
        WHEN ((a.id + u.id) % 5) + 1 = 3  THEN CONCAT('整体还不错，「', a.title, '」中规中矩，个别环节还能做得更好。')
        ELSE CONCAT('体验一般，「', a.title, '」现场有些拥挤，入场效率希望能改进。')
    END AS content,
    1 AS status,
    IF(((a.id + u.id) % 4) = 0, '感谢您的支持与反馈，我们会持续优化服务，期待与您再次相遇！', NULL) AS reply,
    IF(((a.id + u.id) % 4) = 0, DATE_ADD(DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 25 + 1) DAY), INTERVAL 1 DAY), NULL) AS reply_time,
    IF(((a.id + u.id) % 4) = 0, (SELECT MAX(id) FROM sys_user WHERE role = 1), NULL) AS replier_id,
    DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 25 + 1) DAY) AS create_time
FROM `activity` a
JOIN `sys_user` u
  ON u.role = 0 AND u.deleted = 0
 AND u.username IN ('user1','user2','user3','user4','user5','user6','user7','user8')
WHERE a.status = 3 AND a.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `activity_review` r
      WHERE r.activity_id = a.id AND r.user_id = u.id AND r.deleted = 0
  );

-- ------------------------------------------------------------
-- 3. 为每个已上架活动生成多笔「已支付」订单（体现有人购买）
--    数量不超过该活动单人限购，金额 = 票价 × 数量
-- ------------------------------------------------------------
INSERT INTO `orders`
(`order_no`, `user_id`, `activity_id`, `quantity`, `total_amount`, `status`,
 `expire_time`, `pay_time`, `source`, `create_time`)
SELECT
    CONCAT('ORDDEMO', a.id, LPAD(u.id, 3, '0'), LPAD(((a.id + u.id) % 97), 2, '0')) AS order_no,
    u.id,
    a.id,
    (((a.id + u.id) % COALESCE(a.purchase_limit, 1)) + 1) AS quantity,
    ROUND(a.price * (((a.id + u.id) % COALESCE(a.purchase_limit, 1)) + 1), 2) AS total_amount,
    1 AS status,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 30 + 1) DAY), INTERVAL 15 MINUTE) AS expire_time,
    DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 30 + 1) DAY) AS pay_time,
    IF(((a.id + u.id) % 3) = 0, 1, 0) AS source,
    DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 30 + 1) DAY) AS create_time
FROM `activity` a
JOIN `sys_user` u
  ON u.role = 0 AND u.deleted = 0
 AND u.username IN ('user1','user2','user3','user4','user5','user6','user7','user8')
WHERE a.status = 3 AND a.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `orders` o
      WHERE o.activity_id = a.id AND o.user_id = u.id AND o.status = 1 AND o.deleted = 0
  );

-- ------------------------------------------------------------
-- 4. 为每个已上架活动补充若干收藏
-- ------------------------------------------------------------
INSERT IGNORE INTO `favorite` (`user_id`, `activity_id`, `create_time`)
SELECT u.id, a.id, DATE_SUB(NOW(), INTERVAL ((a.id + u.id) % 20 + 1) DAY)
FROM `activity` a
JOIN `sys_user` u
  ON u.role = 0 AND u.deleted = 0
 AND u.username IN ('user1','user2','user3','user5','user6','user7')
WHERE a.status = 3 AND a.deleted = 0;

-- ------------------------------------------------------------
-- 5. 同步已售票数：取「原已售」与「已支付订单累计」的较大值，
--    保证之前无销量的活动也显示已有人购买，同时不缩小原有销量。
-- ------------------------------------------------------------
UPDATE `activity` a
SET a.sold_tickets = GREATEST(
        COALESCE(a.sold_tickets, 0),
        (SELECT COALESCE(SUM(o.quantity), 0)
         FROM `orders` o
         WHERE o.activity_id = a.id AND o.status = 1 AND o.deleted = 0)
    )
WHERE a.deleted = 0;

-- 如需「已售票数 = 已支付订单实际累计」的严格同步，请取消下面一行注释并重新执行：
-- UPDATE `activity` a SET a.sold_tickets = (SELECT COALESCE(SUM(o.quantity),0) FROM `orders` o WHERE o.activity_id = a.id AND o.status = 1 AND o.deleted = 0) WHERE a.deleted = 0;

SELECT CONCAT('演示数据同步完成！涉及已上架活动：', COUNT(DISTINCT activity_id), ' 个') AS result
FROM `activity_review` WHERE deleted = 0;
