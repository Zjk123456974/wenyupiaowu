package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演出阵容 / 嘉宾模块。
 * 之前详情页「演出阵容」是空壳，本实体让运营在后台按活动逐条填写嘉宾信息，
 * 详情页按 sortOrder 顺序展示，形成可配置的真实内容（对标大麦「艺人阵容」）。
 */
@Data
@TableName("activity_lineup")
public class ActivityLineup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属活动 */
    private Long activityId;

    /** 嘉宾 / 艺人姓名 */
    private String name;

    /** 角色：主演 / 嘉宾 / 乐队 / 主持人 / 特邀 等 */
    private String role;

    /** 头像图片 URL（可空，前端展示默认占位） */
    private String avatar;

    /** 一句话简介 */
    private String description;

    /** 展示排序，越小越靠前 */
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
