package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM sys_user WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month")
    List<Map<String, Object>> countMonthlyUsers();

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countByUsernameIncludingDeleted(@org.apache.ibatis.annotations.Param("username") String username);
}
