-- 2026-10-28：新增“想看”社交计数（对标大麦“X人想看”）
-- 活动表增加想看人数缓存列
ALTER TABLE `activity`
  ADD COLUMN `wish_count` INT NOT NULL DEFAULT 0 COMMENT '想看人数（轻量社交计数，区别于预售预约）';

-- 想看记录表：用于去重与记录“当前用户是否已想看”
CREATE TABLE IF NOT EXISTS `activity_wish` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `activity_id` BIGINT       NOT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
  KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动想看记录';
