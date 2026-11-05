package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退票工单实体类
 * status: 0待审核 / 1通过 / 2驳回 / 3已退款
 */
@Data
@TableName("refund_order")
public class RefundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退票工单号 RF + 时间戳 */
    private String refundNo;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 活动ID */
    private Long activityId;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 手续费 */
    private BigDecimal feeAmount;

    /** 实退金额 */
    private BigDecimal actualAmount;

    /** 退款原因 */
    private String reason;

    /** 状态 0待审核/1通过/2驳回/3已退款 */
    private Integer status;

    /** 审核人ID */
    private Long auditorId;

    /** 审核人姓名 */
    private String auditorName;

    /** 审核备注 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 退款时间 */
    private LocalDateTime refundTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;

    /** 用户名（非数据库字段） */
    @TableField(exist = false)
    private String username;
}
