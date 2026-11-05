package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广告位实体类
 * position: popup-弹窗 sidebar-侧边栏 footer-底部
 * status: 0-禁用 1-启用
 */
@Data
@TableName("advertising")
public class Advertising {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String imageUrl;

    private String linkUrl;

    private Long activityId;

    /** 广告位位置 popup-弹窗 sidebar-侧边栏 footer-底部 */
    private String position;

    private Integer sortOrder;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
