package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long activityId;

    private Integer quantity;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private Integer source;

    private Long ticketPriceId;

    private String seatType;

    /** 观演人姓名 */
    private String attendeeName;

    /** 观演人身份证号 */
    private String attendeeIdCard;

    /** 选座座位编码（JSON 数组，仅选座场次使用，如 ["A-1-2","A-1-3"]） */
    private String seats;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;

    /** 活动封面（非数据库字段） */
    @TableField(exist = false)
    private String coverImage;

    /** 活动时间（非数据库字段） */
    @TableField(exist = false)
    private LocalDateTime activityStartTime;

    /** 活动地点（非数据库字段） */
    @TableField(exist = false)
    private String venue;
}
