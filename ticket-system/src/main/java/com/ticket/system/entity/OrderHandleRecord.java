package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单异常/人工处理记录实体类
 * handleType: 1恢复订单 / 2强制取消 / 3标记已解决
 */
@Data
@TableName("order_handle_record")
public class OrderHandleRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 处理方式：1恢复订单/2强制取消/3标记已解决 */
    private Integer handleType;

    /** 处理备注 */
    private String handleRemark;

    /** 处理人ID */
    private Long operatorId;

    /** 处理人姓名 */
    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
