package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attendee")
public class Attendee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String idCard;

    private String phone;

    private Integer isDefault;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
