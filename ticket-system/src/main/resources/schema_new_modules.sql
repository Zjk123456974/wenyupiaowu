-- ============================================================
-- 文娱票务管理系统 - 新增模块数据库表结构
-- 模块14: 实名信息与观演人管理
-- 模块15: 财务对账与数据导出
-- 模块16: 系统基础配置
-- ============================================================

-- ============================================================
-- 模块14: 观演人实名信息表
-- ============================================================
DROP TABLE IF EXISTS `attendee`;
CREATE TABLE `attendee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '观演人姓名',
  `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号码',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认观演人 0-否 1-是',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观演人实名信息表';

-- ============================================================
-- 模块15: 交易流水记录表
-- ============================================================
DROP TABLE IF EXISTS `transaction_flow`;
CREATE TABLE `transaction_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `flow_no` VARCHAR(32) NOT NULL COMMENT '流水编号',
  `order_no` VARCHAR(32) NOT NULL COMMENT '关联订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `activity_id` BIGINT DEFAULT NULL COMMENT '活动ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额',
  `flow_type` TINYINT NOT NULL COMMENT '流水类型 1-收入(支付) 2-退款 3-手续费',
  `pay_time` DATETIME DEFAULT NULL COMMENT '交易时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_no` (`flow_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_pay_time` (`pay_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水记录表';

-- ============================================================
-- 模块16: 系统基础配置表
-- ============================================================
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `config_group` VARCHAR(50) DEFAULT 'system' COMMENT '配置分组 system-系统参数 upload-文件上传 task-定时任务',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统基础配置表';

-- ============================================================
-- 初始化系统配置数据
-- ============================================================
INSERT INTO `system_config` (`config_key`, `config_value`, `config_name`, `config_group`, `remark`) VALUES
('system.name', '智能文娱票务管理系统', '系统名称', 'system', '平台显示名称'),
('system.logo', '', '网站LOGO', 'system', '网站Logo图片地址'),
('system.icp', '京ICP备00000000号', '备案信息', 'system', '网站备案号'),
('system.service_phone', '400-888-8888', '客服电话', 'system', '平台客服联系电话'),
('order.timeout', '15', '订单超时时间(分钟)', 'system', '待支付订单自动取消时间'),
('stock.warn_threshold', '100', '库存预警阈值', 'system', '票档剩余库存低于此值触发预警'),
('ticket.rate_limit_interval', '3', '抢票限流间隔(秒)', 'system', '同一用户抢票最小间隔时间'),
('upload.max_size', '10', '图片上传大小限制(MB)', 'upload', '单张图片最大上传大小'),
('upload.allowed_types', 'jpg,jpeg,png,gif,webp', '允许上传的图片格式', 'upload', '图片文件允许的扩展名'),
('upload.path', 'uploads/', '文件存储路径', 'upload', '上传文件存储目录'),
('task.order_timeout_check', '60', '订单超时检查间隔(秒)', 'task', '定时任务检查待支付订单超时的间隔'),
('task.status_auto_update', '300', '状态自动更新间隔(秒)', 'task', '定时任务自动更新活动状态的间隔');

-- ============================================================
-- 扩展orders表：增加实名观演人字段
-- ============================================================
ALTER TABLE `orders` ADD COLUMN `attendee_name` VARCHAR(50) DEFAULT NULL COMMENT '观演人姓名' AFTER `seat_type`;
ALTER TABLE `orders` ADD COLUMN `attendee_id_card` VARCHAR(18) DEFAULT NULL COMMENT '观演人身份证号' AFTER `attendee_name`;
