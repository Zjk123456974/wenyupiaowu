package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Long categoryId;

    private String venue;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String coverImage;

    private String description;

    /** 活动注意事项（购票须知补充文案，可选） */
    private String ticketNotice;

    private String ticketRule;

    /** 退票策略：1 可退票 / 0 不可退票（如演唱会默认不可退） */
    private Integer refundPolicy;

    /** 实名模式：0 免实名 / 1 一单一实名（一个实名可购多张，如电竞）/ 2 一票一实名（每张票都必须实名，如演唱会） */
    private Integer realNameMode;

    /** 场馆地图/座位定位开关：1 开启（购票后可直接定位座位）/ 0 关闭 */
    private Integer seatMapEnabled;

    /** 座位平面图布局（JSON，仅 seatMapEnabled=1 时生效）：{ sections:[{id,name,rows,cols,price}] } */
    private String seatLayout;

    /** 3D 场馆视野预览开关：1 开启（选座时展示 3D 场馆与座位视角）/ 0 关闭 */
    private Integer view3dEnabled;

    /** 3D 场馆模型预设：arena_bowl（通用碗形综合馆）等，默认 arena_bowl */
    private String view3dModel;

    /** 预约开关：1 开启（未开售可预约+倒计时提醒）/ 0 关闭 */
    private Integer reservationEnabled;

    private Integer purchaseLimit;

    private Integer totalTickets;

    private Integer soldTickets;

    private BigDecimal price;

    private LocalDateTime grabStartTime;

    private LocalDateTime grabEndTime;

    private Integer status;

    private String rejectReason;

    private Integer viewCount;

    private Long operatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动抢票状态（非数据库字段） */
    @TableField(exist = false)
    private String grabStatus;

    /** 分类名称（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 剩余票数（非数据库字段） */
    @TableField(exist = false)
    private Integer remainTickets;

    /** 已预约想看人数（非数据库字段） */
    @TableField(exist = false)
    private Integer reservationCount;

    /** 想看人数（轻量社交计数，对应大麦“X人想看”，数据库有独立列） */
    private Integer wishCount;

    /** 运营人员名称（非数据库字段） */
    @TableField(exist = false)
    private String operatorName;

    /** 活动标签列表（非数据库字段，详情/编辑回显用） */
    @TableField(exist = false)
    private java.util.List<ActivityTag> tags;

    /** 预计营收（非数据库字段，按票档实际分档价 × 分档销量汇总，多票档活动口径正确） */
    @TableField(exist = false)
    private java.math.BigDecimal revenue;
}
