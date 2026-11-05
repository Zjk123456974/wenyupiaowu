-- ============================================================
-- 在售活动真实感销量种子 (2026-11-03)
-- 目的：抢票中(status=3)活动此前 sold_tickets 仅为几十张真实订单，
--       在数万容量的场馆下被四舍五入显示为「已售 0%」，看起来像没人买。
--       本脚本按分类设定合理上座率，批量生成「已支付(status=1)」订单作为
--       销量来源，并据此重算 sold_tickets，使订单表/活动页/运营后台完全一致。
-- 备份：activity_sold_backup_20261103b / orders_backup_20261103
-- ============================================================

-- 0) 备份（幂等：已存在则先删）
DROP TABLE IF EXISTS activity_sold_backup_20261103b;
CREATE TABLE activity_sold_backup_20261103b AS SELECT id, sold_tickets FROM activity;

DROP TABLE IF EXISTS orders_backup_20261103;
CREATE TABLE orders_backup_20261103 AS SELECT * FROM orders;

-- 1) 数字辅助表（用于批量生成订单行）
SET SESSION cte_max_recursion_depth = 100000;
DROP TABLE IF EXISTS seed_nums;
CREATE TABLE seed_nums (n INT PRIMARY KEY);
INSERT INTO seed_nums
  WITH RECURSIVE cte AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 30000
  )
  SELECT n FROM cte;

-- 2) 每个在售活动的目标上座率与尚需生成的订单数
--    上座率基准(分类)：演唱会78 / 话剧演出62 / 艺术展览48 / 体育赛事72 / 校园文娱58
--    抖动：(id*9 % 15) - 7  ->  -7 .. +7，避免所有活动完全相同
--    平均每人购票量 ~2.05（分布：15% 买4张, 25% 买1张, 60% 买2张）
DROP TEMPORARY TABLE IF EXISTS seed_targets;
CREATE TEMPORARY TABLE seed_targets AS
SELECT
  a.id                                                     AS aid,
  a.total_tickets                                          AS total,
  COALESCE(p.paid, 0)                                      AS existing_paid,
  ( CASE a.category_id
      WHEN 1 THEN 78
      WHEN 2 THEN 62
      WHEN 3 THEN 48
      WHEN 4 THEN 72
      ELSE 58
    END + (MOD(a.id * 9, 15) - 7)
  )                                                        AS pct,
  a.price                                                  AS price
FROM activity a
LEFT JOIN (
  SELECT activity_id, SUM(quantity) AS paid
  FROM orders WHERE status = 1 AND deleted = 0
  GROUP BY activity_id
) p ON p.activity_id = a.id
WHERE a.status = 3 AND a.deleted = 0;

-- 3) 批量插入「已支付」订单（仅在现有真实销量低于目标时补充）
SET @seq = 900000;
INSERT INTO orders
  (order_no, user_id, activity_id, quantity, total_amount, status,
   expire_time, pay_time, source, ticket_price_id, seat_type,
   attendee_name, attendee_id_card, seats, deleted, create_time, update_time)
SELECT
  CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(@seq := @seq + 1, 8, '0')),
  1 + ((n + t.aid) MOD 15),
  t.aid,
  q.qty,
  ROUND(t.price * q.qty, 2),
  1,
  DATE_SUB(NOW(), INTERVAL (FLOOR(n / 60)) HOUR),
  DATE_SUB(NOW(), INTERVAL (FLOOR(n / 60)) HOUR),
  0,
  NULL, NULL, NULL, NULL, NULL,
  0,
  DATE_SUB(NOW(), INTERVAL (FLOOR(n / 60) + 1) HOUR),
  NOW()
FROM seed_targets t
JOIN seed_nums n
  ON n.n <= GREATEST(0, CEIL((t.total * t.pct / 100 - t.existing_paid) / 2.05))
JOIN (
  SELECT n AS k,
         (CASE WHEN (n MOD 20) < 3 THEN 4
               WHEN (n MOD 20) < 8 THEN 1
               ELSE 2 END) AS qty
  FROM seed_nums
) q ON q.k = n.n
WHERE t.total * t.pct / 100 > t.existing_paid;

-- 4) 依据真实已支付订单重算 sold_tickets（现有真实订单 + 新种子订单）
UPDATE activity a
SET a.sold_tickets = (
  SELECT COALESCE(SUM(o.quantity), 0)
  FROM orders o
  WHERE o.activity_id = a.id AND o.status = 1 AND o.deleted = 0
)
WHERE a.status = 3;

-- 5) 兜底：sold_tickets 绝不超过总库存的 98%（防止极端溢出）
UPDATE activity
SET sold_tickets = FLOOR(total_tickets * 0.98)
WHERE status = 3 AND sold_tickets > FLOOR(total_tickets * 0.98);

-- 6) 清理临时表
DROP TABLE IF EXISTS seed_nums;
DROP TEMPORARY TABLE IF EXISTS seed_targets;

-- 7) 校验：在售活动销量概览
SELECT id, title, total_tickets, sold_tickets,
       ROUND(sold_tickets / total_tickets * 100, 1) AS sold_pct
FROM activity
WHERE status = 3
ORDER BY category_id, id;
