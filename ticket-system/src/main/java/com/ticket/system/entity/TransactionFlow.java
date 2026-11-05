package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction_flow")
public class TransactionFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowNo;

    private String orderNo;

    private Long userId;

    private Long activityId;

    private BigDecimal amount;

    /** 流水类型 1-收入(支付) 2-退款 3-手续费 */
    private Integer flowType;

    private LocalDateTime payTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;
}
