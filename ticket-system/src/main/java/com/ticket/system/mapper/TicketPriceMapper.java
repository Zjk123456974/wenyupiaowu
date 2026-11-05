package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.TicketPrice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketPriceMapper extends BaseMapper<TicketPrice> {

    List<TicketPrice> selectByActivityId(@Param("activityId") Long activityId);

    List<TicketPrice> selectAvailableByActivityId(@Param("activityId") Long activityId);
}