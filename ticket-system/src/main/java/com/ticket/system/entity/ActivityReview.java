package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动评价实体类
 * status: 0待审核 / 1通过 / 2隐藏 / 3驳回
 */
@Data
@TableName("activity_review")
public class ActivityReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private Long activityId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价图片（逗号分隔） */
    private String images;

    /** 状态 0待审核/1通过/2隐藏/3驳回 */
    private Integer status;

    /** 回复内容 */
    private String reply;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 回复人ID */
    private Long replierId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 活动标题（非数据库字段） */
    @TableField(exist = false)
    private String activityTitle;
}
