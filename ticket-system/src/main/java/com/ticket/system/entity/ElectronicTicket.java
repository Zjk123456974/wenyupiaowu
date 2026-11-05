package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子票实体类
 * 对应数据库表 electronic_ticket
 */
@Data
@TableName("electronic_ticket")
public class ElectronicTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 电子票号（唯一） */
    private String ticketNo;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 购票用户ID */
    private Long userId;

    /** 活动ID */
    private Long activityId;

    /** 票档ID */
    private Long ticketPriceId;

    /** 座位类型 */
    private String seatType;

    /** 座位定位信息（区/排/座），场馆地图开启的场次在出票时生成 */
    private String seatInfo;

    /** 座位编码（如 A-1-2），选座场次专用；非选座场次为 null */
    private String seatCode;

    /** 观演人姓名 */
    private String attendeeName;

    /** 观演人身份证号 */
    private String attendeeIdCard;

    /** 核销验证码（UUID） */
    private String verifyCode;

    /** 票据状态：0未核销 / 1已核销 / 2已作废 */
    private Integer status;

    /** 转赠状态：0 持有 / 1 转出中 / 2 已转出（借鉴大麦「转赠」） */
    private Integer transferStatus;

    /** 核销时间 */
    private LocalDateTime checkTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;

    /** 举办场馆（非数据库字段） */
    @TableField(exist = false)
    private String venue;

    /** 演出时间（非数据库字段） */
    @TableField(exist = false)
    private LocalDateTime startTime;

    /** 关联订单状态（非数据库字段，0待支付/1已支付/2已取消/3已超时/5已退款） */
    @TableField(exist = false)
    private Integer orderStatus;

    /** 最近一次退票工单状态（非数据库字段，null无申请/0待审核/1通过/2驳回/3已退款） */
    @TableField(exist = false)
    private Integer refundStatus;

    /** 核销二维码时效令牌（非数据库字段，由后端 HMAC 签名，供用户端生成真实可扫码图片） */
    @TableField(exist = false)
    private String qrToken;

    /** 令牌过期时间戳（非数据库字段，epoch 毫秒，前端据此刷新二维码） */
    @TableField(exist = false)
    private Long qrExpireAt;
}
