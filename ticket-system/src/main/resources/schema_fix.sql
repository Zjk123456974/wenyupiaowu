-- ============================================================
-- 文娱票务管理系统 - 数据库修复与补全脚本（MySQL全版本兼容）
-- 修复：ticket_price/banner表缺失，orders/category字段缺失
-- 新增：电子票、核销记录、退票工单、评价、标签、广告位、日志等表
-- 使用前请先执行：USE ticket_system;
-- ============================================================

USE `ticket_system`;

-- ============================================================
-- 一、修复已有表字段缺失问题（使用存储过程安全添加列，避免重复列报错）
-- ============================================================

DROP PROCEDURE IF EXISTS `safe_add_column`;
DELIMITER $$
CREATE PROCEDURE `safe_add_column`(
    IN tableName VARCHAR(100),
    IN columnName VARCHAR(100),
    IN columnDef VARCHAR(500)
)
BEGIN
    DECLARE colCount INT DEFAULT 0;
    SELECT COUNT(*) INTO colCount 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = tableName 
      AND COLUMN_NAME = columnName;
    IF colCount = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 1. 修复 orders 表：补全 ticket_price_id、seat_type、attendee 字段
CALL `safe_add_column`('orders', 'ticket_price_id', 'BIGINT DEFAULT NULL COMMENT ''票档ID'' AFTER `source`');
CALL `safe_add_column`('orders', 'seat_type', 'VARCHAR(50) DEFAULT NULL COMMENT ''座位类型'' AFTER `ticket_price_id`');
CALL `safe_add_column`('orders', 'attendee_name', 'VARCHAR(50) DEFAULT NULL COMMENT ''观演人姓名'' AFTER `seat_type`');
CALL `safe_add_column`('orders', 'attendee_id_card', 'VARCHAR(18) DEFAULT NULL COMMENT ''观演人身份证号'' AFTER `attendee_name`');

-- 2. 修复 category 表：补全 icon、color、status 字段
CALL `safe_add_column`('category', 'icon', 'VARCHAR(100) DEFAULT NULL COMMENT ''分类图标'' AFTER `name`');
CALL `safe_add_column`('category', 'color', 'VARCHAR(20) DEFAULT NULL COMMENT ''分类主题色'' AFTER `icon`');
CALL `safe_add_column`('category', 'status', 'TINYINT DEFAULT 1 COMMENT ''状态 0-禁用 1-启用'' AFTER `color`');

-- 3. 扩展 announcement 表：支持定时发布/下线/定向推送
CALL `safe_add_column`('announcement', 'publish_time', 'DATETIME DEFAULT NULL COMMENT ''定时发布时间'' AFTER `status`');
CALL `safe_add_column`('announcement', 'offline_time', 'DATETIME DEFAULT NULL COMMENT ''定时下线时间'' AFTER `publish_time`');
CALL `safe_add_column`('announcement', 'target_role', 'TINYINT DEFAULT NULL COMMENT ''推送目标角色'' AFTER `offline_time`');

-- 清理存储过程
DROP PROCEDURE IF EXISTS `safe_add_column`;

-- ============================================================
-- 二、补建缺失的已有实体表
-- ============================================================

-- 4. 票档表（代码已引用，SQL缺失）
CREATE TABLE IF NOT EXISTS `ticket_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `activity_title` VARCHAR(200) DEFAULT NULL COMMENT '活动标题（冗余）',
  `seat_type` VARCHAR(50) NOT NULL COMMENT '座位类型',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售卖价格',
  `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
  `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '已售库存',
  `seat_index` INT DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票档设置表';

-- 5. 轮播图表（代码已引用，SQL缺失）
CREATE TABLE IF NOT EXISTS `banner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '轮播标题',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '简介描述',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID',
  `sort_order` INT DEFAULT 0 COMMENT '展示排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- ============================================================
-- 三、新增模块表 - 电子票核销与检票管理（模块7）
-- ============================================================

-- 6. 电子票表
CREATE TABLE IF NOT EXISTS `electronic_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_no` VARCHAR(32) NOT NULL COMMENT '电子票编号（唯一）',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '关联订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `ticket_price_id` BIGINT DEFAULT NULL COMMENT '票档ID',
  `seat_type` VARCHAR(50) DEFAULT NULL COMMENT '座位类型',
  `attendee_name` VARCHAR(50) DEFAULT NULL COMMENT '观演人姓名',
  `attendee_id_card` VARCHAR(18) DEFAULT NULL COMMENT '观演人身份证',
  `verify_code` VARCHAR(64) NOT NULL COMMENT '核销二维码内容',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0-未核销 1-已核销 2-已作废',
  `check_time` DATETIME DEFAULT NULL COMMENT '核销时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_verify_code` (`verify_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子票表';

-- 7. 核销记录表
CREATE TABLE IF NOT EXISTS `ticket_check_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` BIGINT NOT NULL COMMENT '电子票ID',
  `ticket_no` VARCHAR(32) NOT NULL COMMENT '电子票编号',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `activity_title` VARCHAR(200) DEFAULT NULL COMMENT '活动标题',
  `attendee_name` VARCHAR(50) DEFAULT NULL COMMENT '观演人姓名',
  `check_user_id` BIGINT NOT NULL COMMENT '核销工作人员ID',
  `check_username` VARCHAR(50) DEFAULT NULL COMMENT '核销人员用户名',
  `check_device` VARCHAR(100) DEFAULT NULL COMMENT '核销设备信息',
  `check_ip` VARCHAR(50) DEFAULT NULL COMMENT '核销IP',
  `check_time` DATETIME NOT NULL COMMENT '核销时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检票核销记录表';

-- ============================================================
-- 四、新增模块表 - 退换票与售后审核（模块8）
-- ============================================================

-- 8. 退票工单表
CREATE TABLE IF NOT EXISTS `refund_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` VARCHAR(32) NOT NULL COMMENT '退票工单号',
  `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '关联订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `fee_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '手续费',
  `actual_amount` DECIMAL(10,2) NOT NULL COMMENT '实际到账金额',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '退票原因',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-审核通过 2-审核驳回 3-已退款',
  `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `auditor_name` VARCHAR(50) DEFAULT NULL COMMENT '审核人姓名',
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款完成时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退票工单表';

-- ============================================================
-- 五、新增模块表 - 用户评价（模块9）
-- ============================================================

-- 9. 活动评价表
CREATE TABLE IF NOT EXISTS `activity_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名（冗余）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称（冗余）',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像（冗余）',
  `rating` INT NOT NULL COMMENT '星级评分 1-5',
  `content` TEXT DEFAULT NULL COMMENT '评价内容',
  `images` VARCHAR(1000) DEFAULT NULL COMMENT '评价图片URL（逗号分隔）',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-已通过 2-已隐藏 3-已驳回',
  `reply` TEXT DEFAULT NULL COMMENT '官方回复内容',
  `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
  `replier_id` BIGINT DEFAULT NULL COMMENT '回复人ID',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动评价表';

-- ============================================================
-- 六、新增模块表 - 活动标签管理（模块3）
-- ============================================================

-- 10. 活动标签表
CREATE TABLE IF NOT EXISTS `activity_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `color` VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动标签表';

-- 11. 活动标签关联表
CREATE TABLE IF NOT EXISTS `activity_tag_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_tag` (`activity_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动标签关联表';

-- 初始化标签数据（INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `activity_tag` (`name`, `color`, `sort_order`) VALUES
('热门推荐', '#F56C6C', 1),
('限时抢票', '#E6A23C', 2),
('早鸟优惠', '#67C23A', 3),
('新人专享', '#409EFF', 4),
('口碑好剧', '#909399', 5);

-- ============================================================
-- 七、新增模块表 - 多广告位管理（模块10）
-- ============================================================

-- 12. 广告位表
CREATE TABLE IF NOT EXISTS `advertising` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '广告标题',
  `image_url` VARCHAR(500) NOT NULL COMMENT '广告图片',
  `link_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID',
  `position` VARCHAR(30) NOT NULL DEFAULT 'sidebar' COMMENT '广告位 popup-弹窗 sidebar-侧边栏 footer-底部',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `start_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '失效时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_position` (`position`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告位管理表';

-- ============================================================
-- 八、新增模块表 - 系统日志与安全审计（模块12）
-- ============================================================

-- 13. 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
  `device` VARCHAR(200) DEFAULT NULL COMMENT '设备信息',
  `result` TINYINT DEFAULT 1 COMMENT '操作结果 1-成功 0-失败',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `cost_time` BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 14. 登录日志表
CREATE TABLE IF NOT EXISTS `login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `login_type` TINYINT DEFAULT 1 COMMENT '类型 1-登录 2-退出',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
  `device` VARCHAR(200) DEFAULT NULL COMMENT '设备信息',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1-成功 0-失败',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '提示消息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================================
-- 执行完成
-- ============================================================
SELECT 'schema_fix.sql 执行完成！' AS result;
