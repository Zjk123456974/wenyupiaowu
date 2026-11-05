-- ============================================================
-- 运营分级迁移脚本
-- 说明：把运营（role=1）细分为「普通运营」与「活动运营」两种类型
--   0 普通运营：原有功能不变，不开放电子票核销端
--   1 活动运营：额外获得「电子票核销」菜单，仅能核销本人负责的活动
-- 管理员（role=2）不受影响，可核销全部活动
-- 执行：mysql -uroot -p ticket_system < migration_operator_type.sql
-- 注意：ALTER 语句只需执行一次
-- ============================================================

ALTER TABLE sys_user
    ADD COLUMN operator_type TINYINT NOT NULL DEFAULT 0
        COMMENT '运营细分类型：0普通运营 1活动运营（仅 role=1 有效）' AFTER role;

-- 现有两个运营账号升级为活动运营（operator1 名下已有 22 场活动，yunyin 为演示账号）
UPDATE sys_user SET operator_type = 1 WHERE id IN (4, 1593);

-- 演示数据：把 3 个尚未分配运营的已发布活动归到活动运营 yunyin(1593) 名下，
-- 便于验证「活动运营只能核销自己活动」的权限隔离效果
UPDATE activity SET operator_id = 1593
WHERE operator_id IS NULL AND status = 3
ORDER BY id LIMIT 3;
