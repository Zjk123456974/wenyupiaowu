package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.ActivityTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityTagMapper extends BaseMapper<ActivityTag> {

    @Select("SELECT t.id, t.name, t.color, t.sort_order, t.status " +
            "FROM activity_tag t WHERE t.deleted = 0 AND t.status = 1 ORDER BY t.sort_order")
    List<ActivityTag> listEnabledTags();
}
