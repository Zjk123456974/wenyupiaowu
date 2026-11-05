package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 操作日志表不需要逻辑删除和更新时间，只有create_time
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作模块 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 操作IP */
    private String ip;

    /** 设备信息 */
    private String device;

    /** 操作结果 1-成功 0-失败 */
    private Integer result;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
