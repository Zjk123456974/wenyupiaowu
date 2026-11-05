package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.TransactionFlow;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;

public interface FinanceService {

    /**
     * 记录支付流水
     */
    void recordPayment(String orderNo, Long userId, Long activityId, java.math.BigDecimal amount, LocalDateTime payTime);

    /**
     * 记录退款流水
     */
    void recordRefund(String orderNo, Long userId, Long activityId, java.math.BigDecimal amount, LocalDateTime payTime);

    /**
     * 记录退票手续费流水
     */
    void recordFee(String orderNo, Long userId, Long activityId, java.math.BigDecimal amount, LocalDateTime payTime);

    /**
     * 财务汇总
     */
    Map<String, Object> getFinanceSummary(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 每日对账数据
     */
    Map<String, Object> getDailyReconciliation();

    /**
     * 流水分页查询
     */
    IPage<TransactionFlow> getFlowPage(Integer page, Integer size, Integer flowType,
                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出订单数据Excel
     */
    void exportOrders(HttpServletResponse response, LocalDateTime startTime, LocalDateTime endTime) throws Exception;

    /**
     * 导出用户数据Excel
     */
    void exportUsers(HttpServletResponse response) throws Exception;

    /**
     * 导出活动数据Excel
     */
    void exportActivities(HttpServletResponse response) throws Exception;

    /**
     * 导出财务流水Excel
     */
    void exportFinance(HttpServletResponse response, LocalDateTime startTime, LocalDateTime endTime) throws Exception;
}
