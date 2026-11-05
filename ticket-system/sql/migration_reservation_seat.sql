-- 预约支持选择票档价位：预约时记录座位类型与票档ID，开抢后按预约票档出票
ALTER TABLE reservation
    ADD COLUMN seat_type VARCHAR(64) DEFAULT NULL COMMENT '预约座位类型（票档名称）' AFTER queue_number,
    ADD COLUMN ticket_price_id BIGINT DEFAULT NULL COMMENT '预约票档ID（开抢后按该票档出票）' AFTER seat_type;

-- 订单异常/人工处理记录表：记录管理员对异常订单的具体处理方式
CREATE TABLE IF NOT EXISTS order_handle_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    handle_type TINYINT NOT NULL COMMENT '处理方式：1恢复订单/2强制取消/3标记已解决',
    handle_remark VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
    operator_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '处理人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单异常处理记录表';
