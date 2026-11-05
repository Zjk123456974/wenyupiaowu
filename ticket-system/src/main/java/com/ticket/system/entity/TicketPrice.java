package com.ticket.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_price")
public class TicketPrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private String activityTitle;

    private String seatType;

    private BigDecimal price;

    private Integer totalStock;

    private Integer soldStock;

    private Integer seatIndex;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}