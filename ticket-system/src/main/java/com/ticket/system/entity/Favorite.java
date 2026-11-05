package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long activityId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;

    /** 活动封面（非数据库字段） */
    @TableField(exist = false)
    private String coverImage;

    /** 活动开始时间（非数据库字段） */
    @TableField(exist = false)
    private LocalDateTime activityStartTime;

    /** 票价（非数据库字段） */
    @TableField(exist = false)
    private java.math.BigDecimal price;

    /** 活动场馆（非数据库字段） */
    @TableField(exist = false)
    private String venue;

    /** 活动分类名（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 活动状态：-1草稿/0待审核/2驳回/3已上架(抢票中)/4已下架（非数据库字段） */
    @TableField(exist = false)
    private Integer status;

    /** 总票数（非数据库字段） */
    @TableField(exist = false)
    private Integer totalTickets;

    /** 已售票数（非数据库字段） */
    @TableField(exist = false)
    private Integer soldTickets;

    /** 想看人数（非数据库字段） */
    @TableField(exist = false)
    private Integer wishCount;
}
