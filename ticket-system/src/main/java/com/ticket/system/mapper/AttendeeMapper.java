package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.Attendee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendeeMapper extends BaseMapper<Attendee> {
}
