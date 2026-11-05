package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.ActivityTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityTagRelationMapper extends BaseMapper<ActivityTagRelation> {

    @Select("SELECT tag_id FROM activity_tag_relation WHERE activity_id = #{activityId}")
    List<Long> selectTagIdsByActivityId(Long activityId);
}
