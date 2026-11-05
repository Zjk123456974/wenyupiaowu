package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM orders WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month")
    List<Map<String, Object>> countMonthlyOrders();

    @Select("SELECT IFNULL(SUM(total_amount), 0) FROM orders WHERE deleted = 0 AND status = 1")
    Long sumTotalAmount();

    @Select("SELECT COUNT(DISTINCT user_id) FROM orders WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)")
    Long countActiveUsers30d();
}
