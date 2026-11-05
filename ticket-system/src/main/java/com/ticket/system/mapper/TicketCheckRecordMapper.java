package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.TicketCheckRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 核销记录 Mapper
 */
@Mapper
public interface TicketCheckRecordMapper extends BaseMapper<TicketCheckRecord> {
}
