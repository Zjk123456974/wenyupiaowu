package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核销记录实体类
 * 对应数据库表 ticket_check_record
 */
@Data
@TableName("ticket_check_record")
public class TicketCheckRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电子票ID */
    private Long ticketId;

    /** 电子票号 */
    private String ticketNo;

    /** 活动ID */
    private Long activityId;

    /** 活动标题 */
    private String activityTitle;

    /** 观演人姓名 */
    private String attendeeName;

    /** 核销人员ID */
    private Long checkUserId;

    /** 核销人员用户名 */
    private String checkUsername;

    /** 核销设备 */
    private String checkDevice;

    /** 核销IP */
    private String checkIp;

    /** 核销时间 */
    private LocalDateTime checkTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
