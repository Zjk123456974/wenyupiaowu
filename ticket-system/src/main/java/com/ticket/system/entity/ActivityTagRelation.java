package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_tag_relation")
public class ActivityTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long tagId;

    private LocalDateTime createTime;
}
