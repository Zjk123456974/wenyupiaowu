package com.ticket.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.system.entity.TransactionFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TransactionFlowMapper extends BaseMapper<TransactionFlow> {

    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS date, " +
            "IFNULL(SUM(CASE WHEN flow_type = 1 THEN amount ELSE 0 END), 0) AS income, " +
            "IFNULL(SUM(CASE WHEN flow_type = 2 THEN amount ELSE 0 END), 0) AS refund, " +
            "COUNT(CASE WHEN flow_type = 1 THEN 1 END) AS orderCount " +
            "FROM transaction_flow WHERE deleted = 0 AND pay_time IS NOT NULL " +
            "GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d') ORDER BY date DESC LIMIT 30")
    List<Map<String, Object>> dailyReconciliation();
}
