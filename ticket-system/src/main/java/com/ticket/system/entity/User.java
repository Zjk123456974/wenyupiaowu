package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String bio;

    private Integer role;

    /** 运营细分类型：0 普通运营（原功能不变）/ 1 活动运营（可进入核销端，仅核销本人负责的活动），仅 role=1 时有效 */
    private Integer operatorType;

    private Integer status;

    private String banReason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
