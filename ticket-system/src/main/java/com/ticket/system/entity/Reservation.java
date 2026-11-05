package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    private Integer queueNumber;

    /** 预约座位类型（票档名称，非数据库字段兼容用） */
    private String seatType;

    /** 预约票档ID：开抢后按该票档出票 */
    private Long ticketPriceId;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;

    /** 开抢时间（非数据库字段，用于预约倒计时提醒） */
    @TableField(exist = false)
    private LocalDateTime grabStartTime;

    /** 排队总人数（非数据库字段） */
    @TableField(exist = false)
    private Integer totalQueueCount;
}
