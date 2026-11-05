package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子票转赠记录（借鉴大麦「转赠」能力）
 * 状态：0 待接收 / 1 已接收 / 2 已取消 / 3 已过期
 */
@Data
@TableName("ticket_transfer")
public class TicketTransfer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源订单ID */
    private Long orderId;

    /** 活动ID */
    private Long activityId;

    /** 转赠发起人（电子票原持有者） */
    private Long senderUserId;

    /** 接收方手机号（领取时需用该手机号对应的账号接收） */
    private String recipientPhone;

    /** 接收方用户ID（领取后回填） */
    private Long recipientUserId;

    /** 转赠码（唯一，领取凭证） */
    private String transferCode;

    /** 转赠状态：0 待接收 / 1 已接收 / 2 已取消 / 3 已过期 */
    private Integer status;

    /** 本次转赠的电子票ID列表（逗号分隔） */
    private String ticketIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 领取时间 */
    private LocalDateTime acceptTime;
}
