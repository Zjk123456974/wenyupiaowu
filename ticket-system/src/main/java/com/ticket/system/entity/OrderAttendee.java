package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单观演人关联实体（一票一证：一张电子票对应一个观演人）
 */
@Data
@TableName("order_attendee")
public class OrderAttendee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 活动ID */
    private Long activityId;

    /** 观演人姓名 */
    private String attendeeName;

    /** 观演人身份证号 */
    private String attendeeIdCard;

    private LocalDateTime createTime;
}
