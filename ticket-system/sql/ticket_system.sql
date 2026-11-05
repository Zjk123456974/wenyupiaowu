-- 智能文娱票务预约抢票系统 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS ticket_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ticket_system;

-- 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL COMMENT '账号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通用户 1运营 2管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0封禁 1正常',
    `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '封禁原因',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 活动分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动分类表';

-- 文娱活动表
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `venue` VARCHAR(200) NOT NULL COMMENT '举办地点',
    `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
    `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `description` TEXT COMMENT '活动详情介绍',
    `ticket_rule` VARCHAR(500) DEFAULT NULL COMMENT '票务规则',
    `purchase_limit` INT NOT NULL DEFAULT 1 COMMENT '单人限购数量',
    `total_tickets` INT NOT NULL COMMENT '总票数',
    `sold_tickets` INT NOT NULL DEFAULT 0 COMMENT '已售票数',
    `price` DECIMAL(10,2) NOT NULL COMMENT '票价',
    `grab_start_time` DATETIME NOT NULL COMMENT '抢票开始时间',
    `grab_end_time` DATETIME NOT NULL COMMENT '抢票结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已驳回 3已上架 4已下架',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `operator_id` BIGINT DEFAULT NULL COMMENT '运营人员ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_grab_time` (`grab_start_time`, `grab_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文娱活动表';

-- 预约表
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `queue_number` INT NOT NULL COMMENT '排队序号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0排队中 1已出票 2已取消',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`, `deleted`),
    KEY `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 订单表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `quantity` INT NOT NULL COMMENT '购票数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已取消 3已过期',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `source` TINYINT NOT NULL DEFAULT 0 COMMENT '来源：0抢票 1预约出票 2补票',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`),
    KEY `idx_activity` (`activity_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 活动收藏表
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动收藏表';

-- 消息表
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `title` VARCHAR(100) NOT NULL COMMENT '消息标题',
    `content` VARCHAR(500) NOT NULL COMMENT '消息内容',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '类型：0系统通知 1抢票提醒 2排队更新 3订单通知 4活动通知',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 系统公告表
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否 1是',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架 1上架',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 初始化账号 (密码均为: 123456, MD5: e10adc3949ba59abbe56e057f20f883e)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `status`, `phone`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 2, 1, NULL),
('user1', 'e10adc3949ba59abbe56e057f20f883e', '普通用户小王', 0, 1, '13800000001'),
('user2', 'e10adc3949ba59abbe56e057f20f883e', '普通用户小李', 0, 1, '13800000002'),
('operator1', 'e10adc3949ba59abbe56e057f20f883e', '运营人员小张', 1, 1, '13900000001');

-- 初始化活动分类
INSERT INTO `category` (`name`, `sort_order`) VALUES
('演唱会', 1),
('话剧演出', 2),
('艺术展览', 3),
('体育赛事', 4),
('校园文娱', 5);

-- 初始化示例活动 (operator_id对应operator1的用户ID，需根据实际插入后的ID调整)
INSERT INTO `activity` (`title`, `category_id`, `venue`, `start_time`, `end_time`, `description`, `purchase_limit`, `total_tickets`, `sold_tickets`, `price`, `grab_start_time`, `grab_end_time`, `status`, `operator_id`) VALUES
('2026周杰伦嘉年华世界巡回演唱会', 1, '北京国家体育场（鸟巢）', '2026-08-15 19:00:00', '2026-08-15 22:00:00', '周杰伦2026嘉年华世界巡回演唱会北京站，精彩演出不容错过！', 2, 50000, 32000, 880.00, '2026-07-10 10:00:00', '2026-07-25 23:59:59', 3, 4),
('话剧《雷雨》经典重现', 2, '上海大剧院', '2026-08-20 19:30:00', '2026-08-20 21:30:00', '曹禺经典话剧《雷雨》重新编排，实力派演员倾情演绎。', 4, 2000, 800, 280.00, '2026-07-18 10:00:00', '2026-07-22 23:59:59', 3, 4),
('莫奈印象派艺术大展', 3, '中国美术馆', '2026-09-01 09:00:00', '2026-10-31 17:00:00', '莫奈印象派经典作品首次来华大规模展出，感受光与色的魅力。', 1, 10000, 3500, 120.00, '2026-07-10 10:00:00', '2026-08-31 23:59:59', 3, 4),
('2026中超联赛 北京国安vs上海申花', 4, '北京工人体育场', '2026-08-10 19:35:00', '2026-08-10 21:35:00', '中超联赛焦点大战，北京国安主场迎战上海申花！', 6, 60000, 45000, 180.00, '2026-07-25 10:00:00', '2026-08-09 23:59:59', 3, 4),
('校园十佳歌手大赛总决赛', 5, '清华大学大礼堂', '2026-07-30 19:00:00', '2026-07-30 21:30:00', '年度校园音乐盛事，十佳歌手巅峰对决！', 3, 1500, 600, 50.00, '2026-07-10 10:00:00', '2026-07-28 23:59:59', 3, 4),
('林俊杰JJ20世界巡回演唱会', 1, '广州海心沙演艺中心', '2026-09-10 19:30:00', '2026-09-10 22:30:00', '林俊杰JJ20世界巡回演唱会广州站，经典金曲一网打尽！', 2, 30000, 5000, 680.00, '2026-08-01 10:00:00', '2026-08-15 23:59:59', 3, 4);

-- 初始化系统公告
INSERT INTO `announcement` (`title`, `content`, `is_top`, `status`) VALUES
('系统上线公告', '智能文娱票务预约抢票系统已正式上线！欢迎广大用户使用，如有问题请联系客服。', 1, 1),
('暑期演出季来袭', '2026暑期演出季即将开启，周杰伦、林俊杰等巨星演唱会门票即将开抢，敬请关注！', 0, 1),
('抢票攻略', '抢票小技巧：1.提前登录账号 2.开抢前5分钟进入页面等待 3.使用预约功能提前排队 4.保持网络稳定', 0, 1);

-- 初始化订单数据 (status: 0待支付 1已完成 2已取消 3已过期)
INSERT INTO `orders` (`order_no`, `user_id`, `activity_id`, `quantity`, `total_amount`, `status`, `expire_time`, `pay_time`, `source`, `create_time`) VALUES
('ORD2026071000001', 2, 1, 2, 1760.00, 1, '2026-07-10 10:15:00', '2026-07-10 10:12:00', 0, '2026-07-10 10:00:00'),
('ORD2026071100002', 3, 2, 3, 840.00, 1, '2026-07-11 15:15:00', '2026-07-11 14:45:00', 0, '2026-07-11 14:30:00'),
('ORD2026071200003', 2, 3, 1, 120.00, 0, '2026-07-12 11:15:00', NULL, 0, '2026-07-12 11:00:00'),
('ORD2026070900004', 3, 4, 2, 360.00, 2, '2026-07-09 16:15:00', NULL, 0, '2026-07-09 16:00:00'),
('ORD2026070800005', 2, 5, 2, 100.00, 3, '2026-07-08 09:15:00', NULL, 0, '2026-07-08 09:00:00'),
('ORD2026070700006', 3, 1, 1, 880.00, 1, '2026-07-07 14:15:00', '2026-07-07 14:05:00', 1, '2026-07-07 14:00:00'),
('ORD2026070600007', 2, 6, 2, 1360.00, 3, '2026-07-06 12:15:00', NULL, 0, '2026-07-06 12:00:00'),
('ORD2026070500008', 3, 2, 1, 280.00, 1, '2026-07-05 17:15:00', '2026-07-05 17:02:00', 0, '2026-07-05 17:00:00'),
('ORD2026071200009', 2, 4, 4, 720.00, 0, '2026-07-12 16:15:00', NULL, 0, '2026-07-12 16:00:00'),
('ORD2026071100010', 3, 3, 1, 120.00, 2, '2026-07-11 10:15:00', NULL, 0, '2026-07-11 10:00:00');

-- 初始化预约数据 (status: 0排队中 1已出票 2已取消)
INSERT INTO `reservation` (`user_id`, `activity_id`, `queue_number`, `status`, `create_time`) VALUES
(2, 1, 1, 1, '2026-07-05 10:00:00'),
(3, 1, 2, 1, '2026-07-05 10:05:00'),
(2, 6, 1, 0, '2026-07-10 11:00:00'),
(3, 6, 2, 0, '2026-07-10 11:05:00'),
(2, 4, 3, 2, '2026-07-08 09:00:00'),
(3, 2, 1, 1, '2026-07-06 14:00:00');

-- 初始化收藏数据
INSERT INTO `favorite` (`user_id`, `activity_id`, `create_time`) VALUES
(2, 1, '2026-07-05 10:00:00'),
(2, 6, '2026-07-10 11:00:00'),
(3, 2, '2026-07-06 14:00:00'),
(3, 3, '2026-07-07 09:00:00');

-- 初始化消息数据 (type: 0系统通知 1抢票提醒 2排队更新 3订单通知 4活动通知)
INSERT INTO `message` (`user_id`, `title`, `content`, `type`, `is_read`, `create_time`) VALUES
(2, '抢票成功', '恭喜您成功抢到「2026周杰伦嘉年华世界巡回演唱会」门票2张，请及时支付！', 3, 1, '2026-07-10 10:00:00'),
(2, '订单已支付', '您的订单ORD2026071000001已支付成功，期待您的观演！', 3, 1, '2026-07-10 10:12:00'),
(2, '预约成功', '您已成功预约「林俊杰JJ20世界巡回演唱会」，开抢时将优先为您匹配票源。', 2, 0, '2026-07-10 11:00:00'),
(3, '排队更新', '您在「周杰伦演唱会」的排队序号已更新，当前排名第2。', 2, 0, '2026-07-05 10:05:00'),
(3, '订单已取消', '您的订单ORD2026070900004已取消，票务库存已释放。', 3, 1, '2026-07-09 16:30:00'),
(2, '系统通知', '智能文娱票务系统已升级，新增预约排队功能！', 0, 0, '2026-07-01 00:00:00'),
(3, '抢票提醒', '「话剧《雷雨》」抢票即将开始，请提前做好准备！', 1, 1, '2026-07-18 09:30:00'),
(2, '活动通知', '「校园十佳歌手大赛总决赛」门票已上线，欢迎抢购！', 4, 0, '2026-07-15 10:00:00');
