package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.TransactionFlow;
import com.ticket.system.service.FinanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/finance")
public class FinanceController {

    @Resource
    private FinanceService financeService;

    @Resource
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 财务汇总
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        checkAdmin();
        return Result.success(financeService.getFinanceSummary(startTime, endTime));
    }

    /**
     * 每日对账数据
     */
    @GetMapping("/daily")
    public Result<Map<String, Object>> daily() {
        checkAdmin();
        return Result.success(financeService.getDailyReconciliation());
    }

    /**
     * 交易流水分页查询
     */
    @GetMapping("/flows")
    public Result<IPage<TransactionFlow>> flows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer flowType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        checkAdmin();
        return Result.success(financeService.getFlowPage(page, size, flowType, startTime, endTime));
    }

    /**
     * 导出订单数据Excel
     */
    @GetMapping("/export/orders")
    public void exportOrders(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) throws Exception {
        checkAdmin();
        financeService.exportOrders(response, startTime, endTime);
    }

    /**
     * 导出用户数据Excel
     */
    @GetMapping("/export/users")
    public void exportUsers(HttpServletResponse response) throws Exception {
        checkAdmin();
        financeService.exportUsers(response);
    }

    /**
     * 导出活动数据Excel
     */
    @GetMapping("/export/activities")
    public void exportActivities(HttpServletResponse response) throws Exception {
        checkAdmin();
        financeService.exportActivities(response);
    }

    /**
     * 导出财务流水Excel
     */
    @GetMapping("/export/finance")
    public void exportFinance(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) throws Exception {
        checkAdmin();
        financeService.exportFinance(response, startTime, endTime);
    }
}
