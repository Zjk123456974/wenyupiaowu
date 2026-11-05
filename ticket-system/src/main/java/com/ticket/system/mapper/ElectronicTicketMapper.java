package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.ElectronicTicket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子票 Mapper
 */
@Mapper
public interface ElectronicTicketMapper extends BaseMapper<ElectronicTicket> {
}
