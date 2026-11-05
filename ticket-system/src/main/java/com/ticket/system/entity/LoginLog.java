package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体类
 */
@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 类型 1-登录 2-退出 */
    private Integer loginType;

    /** 登录IP */
    private String ip;

    /** 设备信息 */
    private String device;

    /** 浏览器 */
    private String browser;

    /** 登录地点 */
    private String location;

    /** 状态 1-成功 0-失败 */
    private Integer status;

    /** 提示消息 */
    private String message;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
