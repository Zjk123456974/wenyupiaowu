package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Category;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.CategoryMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 基础数据统计
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        checkAdmin();
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalActivities = activityMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalOrders = ordersMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalAmount = ordersMapper.sumTotalAmount();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<User> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(User::getCreateTime, todayStart);
        Long todayNewUsers = userMapper.selectCount(todayWrapper);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        // 活跃用户数：近 30 天内有下单的去重用户数（此前误用下单订单数，口径错误）
        Long activeUsers = ordersMapper.countActiveUsers30d();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<User> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(User::getCreateTime, monthStart);
        Long monthlyNewUsers = userMapper.selectCount(monthWrapper);

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalUsers", totalUsers);
        overview.put("totalActivities", totalActivities);
        overview.put("totalOrders", totalOrders);
        overview.put("totalAmount", totalAmount != null ? new BigDecimal(totalAmount) : BigDecimal.ZERO);
        overview.put("totalRevenue", totalAmount != null ? new BigDecimal(totalAmount) : BigDecimal.ZERO);
        overview.put("todayNewUsers", todayNewUsers);
        overview.put("monthlyNewUsers", monthlyNewUsers);
        overview.put("activeUsers", activeUsers);

        // 最近注册的5个用户
        LambdaQueryWrapper<User> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.orderByDesc(User::getCreateTime).last("LIMIT 5");
        List<User> recentUserList = userMapper.selectList(recentWrapper);
        recentUserList.forEach(u -> u.setPassword(null));
        overview.put("recentUsers", recentUserList);

        return Result.success(overview);
    }

    /**
     * 热门活动排行Top10
     */
    @GetMapping("/hot-activities")
    public Result<List<Activity>> getHotActivities() {
        checkAdmin();
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Activity::getSoldTickets)
               .orderByDesc(Activity::getViewCount)
               .last("LIMIT 10");
        List<Activity> hotList = activityMapper.selectList(wrapper);
        for (Activity activity : hotList) {
            if (activity.getCategoryId() != null) {
                Category category = categoryMapper.selectById(activity.getCategoryId());
                if (category != null) {
                    activity.setCategoryName(category.getName());
                }
            }
        }
        return Result.success(hotList);
    }

    /**
     * 月度订单趋势
     */
    @GetMapping("/monthly-orders")
    public Result<List<Map<String, Object>>> getMonthlyOrders() {
        checkAdmin();
        List<Map<String, Object>> dbData = ordersMapper.countMonthlyOrders();
        return Result.success(fillMonthlyData(dbData));
    }

    /**
     * 用户增长趋势
     */
    @GetMapping("/user-growth")
    public Result<List<Map<String, Object>>> getUserGrowth() {
        checkAdmin();
        List<Map<String, Object>> dbData = userMapper.countMonthlyUsers();
        return Result.success(fillMonthlyData(dbData));
    }

    /**
     * 活动分类占比统计
     */
    @GetMapping("/category-distribution")
    public Result<List<Map<String, Object>>> getCategoryDistribution() {
        checkAdmin();
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category category : categories) {
            LambdaQueryWrapper<Activity> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.eq(Activity::getCategoryId, category.getId());
            Long count = activityMapper.selectCount(catWrapper);
            Map<String, Object> item = new HashMap<>();
            item.put("name", category.getName());
            item.put("value", count);
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 票务资源利用率分析
     */
    @GetMapping("/ticket-utilization")
    public Result<Map<String, Object>> getTicketUtilization() {
        checkAdmin();
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<>());
        int totalTickets = 0;
        int soldTickets = 0;
        for (Activity act : activities) {
            totalTickets += act.getTotalTickets();
            soldTickets += act.getSoldTickets();
        }

        BigDecimal sellRate = totalTickets > 0
                ? new BigDecimal(soldTickets).divide(new BigDecimal(totalTickets), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;

        LambdaQueryWrapper<Orders> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.eq(Orders::getStatus, 3);
        Long expiredOrders = ordersMapper.selectCount(expiredWrapper);

        LambdaQueryWrapper<Orders> cancelledWrapper = new LambdaQueryWrapper<>();
        cancelledWrapper.eq(Orders::getStatus, 2);
        Long cancelledOrders = ordersMapper.selectCount(cancelledWrapper);

        LambdaQueryWrapper<Orders> refundedWrapper = new LambdaQueryWrapper<>();
        refundedWrapper.eq(Orders::getStatus, 5);
        Long refundedOrders = ordersMapper.selectCount(refundedWrapper);

        LambdaQueryWrapper<Orders> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(Orders::getStatus, 1);
        Long totalPaidOrders = ordersMapper.selectCount(paidWrapper);

        Long allOrders = ordersMapper.selectCount(new LambdaQueryWrapper<>());

        BigDecimal expiredRate = allOrders > 0
                ? new BigDecimal(expiredOrders).divide(new BigDecimal(allOrders), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;

        BigDecimal cancelRate = allOrders > 0
                ? new BigDecimal(cancelledOrders).divide(new BigDecimal(allOrders), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;

        // 退款率以“已退款(5)”订单为准，此前误用已取消(2)订单数，口径错误
        BigDecimal refundRate = allOrders > 0
                ? new BigDecimal(refundedOrders).divide(new BigDecimal(allOrders), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("totalTickets", totalTickets);
        result.put("soldTickets", soldTickets);
        result.put("sellRate", sellRate);
        result.put("expiredOrders", expiredOrders);
        result.put("expiredRate", expiredRate);
        result.put("cancelledOrders", cancelledOrders);
        result.put("cancelRate", cancelRate);
        result.put("refundedOrders", refundedOrders);
        result.put("refundRate", refundRate);
        result.put("totalPaidOrders", totalPaidOrders);
        return Result.success(result);
    }

    /**
     * 按日统计最近7天订单
     */
    @GetMapping("/daily-orders")
    public Result<List<Map<String, Object>>> getDailyOrders() {
        checkAdmin();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(formatter));

            LambdaQueryWrapper<Orders> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.ge(Orders::getCreateTime, dayStart).le(Orders::getCreateTime, dayEnd);
            item.put("totalOrders", ordersMapper.selectCount(totalWrapper));

            LambdaQueryWrapper<Orders> paidW = new LambdaQueryWrapper<>();
            paidW.ge(Orders::getCreateTime, dayStart).le(Orders::getCreateTime, dayEnd).eq(Orders::getStatus, 1);
            item.put("paidOrders", ordersMapper.selectCount(paidW));

            LambdaQueryWrapper<Orders> cancelW = new LambdaQueryWrapper<>();
            cancelW.ge(Orders::getCreateTime, dayStart).le(Orders::getCreateTime, dayEnd).eq(Orders::getStatus, 2);
            item.put("cancelledOrders", ordersMapper.selectCount(cancelW));

            LambdaQueryWrapper<Orders> expiredW = new LambdaQueryWrapper<>();
            expiredW.ge(Orders::getCreateTime, dayStart).le(Orders::getCreateTime, dayEnd).eq(Orders::getStatus, 3);
            item.put("expiredOrders", ordersMapper.selectCount(expiredW));

            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 按周统计最近8周订单
     */
    @GetMapping("/weekly-orders")
    public Result<List<Map<String, Object>>> getWeeklyOrders() {
        checkAdmin();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDate weekEnd = today.minusWeeks(i);
            LocalDate weekStart = weekEnd.minusDays(6);
            LocalDateTime start = weekStart.atStartOfDay();
            LocalDateTime end = weekEnd.atTime(LocalTime.MAX);

            Map<String, Object> item = new HashMap<>();
            item.put("week", weekStart.toString() + "~" + weekEnd.toString());

            LambdaQueryWrapper<Orders> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.ge(Orders::getCreateTime, start).le(Orders::getCreateTime, end);
            item.put("totalOrders", ordersMapper.selectCount(totalWrapper));

            LambdaQueryWrapper<Orders> paidW = new LambdaQueryWrapper<>();
            paidW.ge(Orders::getCreateTime, start).le(Orders::getCreateTime, end).eq(Orders::getStatus, 1);
            item.put("paidOrders", ordersMapper.selectCount(paidW));

            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 数据汇总分析
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary() {
        checkAdmin();
        Map<String, Object> summary = new HashMap<>();

        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LambdaQueryWrapper<User> monthUserWrapper = new LambdaQueryWrapper<>();
        monthUserWrapper.ge(User::getCreateTime, monthStart);
        Long monthlyNewUsers = userMapper.selectCount(monthUserWrapper);
        summary.put("totalUsers", totalUsers);
        summary.put("monthlyNewUsers", monthlyNewUsers);

        Long totalActivities = activityMapper.selectCount(new LambdaQueryWrapper<>());
        LambdaQueryWrapper<Activity> onlineWrapper = new LambdaQueryWrapper<>();
        onlineWrapper.eq(Activity::getStatus, 3);
        Long onlineActivities = activityMapper.selectCount(onlineWrapper);
        summary.put("totalActivities", totalActivities);
        summary.put("onlineActivities", onlineActivities);

        Long totalOrders = ordersMapper.selectCount(new LambdaQueryWrapper<>());

        LambdaQueryWrapper<Orders> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.eq(Orders::getStatus, 1);
        Long paidOrders = ordersMapper.selectCount(paidWrapper);

        LambdaQueryWrapper<Orders> cancelWrapper = new LambdaQueryWrapper<>();
        cancelWrapper.eq(Orders::getStatus, 2);
        Long cancelledOrders = ordersMapper.selectCount(cancelWrapper);

        LambdaQueryWrapper<Orders> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.eq(Orders::getStatus, 3);
        Long expiredOrders = ordersMapper.selectCount(expiredWrapper);

        Long totalAmount = ordersMapper.sumTotalAmount();
        summary.put("totalOrders", totalOrders);
        summary.put("paidOrders", paidOrders);
        summary.put("cancelledOrders", cancelledOrders);
        summary.put("expiredOrders", expiredOrders);
        summary.put("totalAmount", totalAmount != null ? new BigDecimal(totalAmount) : BigDecimal.ZERO);

        BigDecimal paidRate = totalOrders > 0
                ? new BigDecimal(paidOrders).divide(new BigDecimal(totalOrders), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;
        summary.put("paidRate", paidRate);

        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<>());
        int totalTickets = 0;
        int soldTicketsSum = 0;
        for (Activity act : activities) {
            totalTickets += act.getTotalTickets();
            soldTicketsSum += act.getSoldTickets();
        }
        BigDecimal overallSellRate = totalTickets > 0
                ? new BigDecimal(soldTicketsSum).divide(new BigDecimal(totalTickets), 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;
        summary.put("totalTickets", totalTickets);
        summary.put("soldTickets", soldTicketsSum);
        summary.put("overallSellRate", overallSellRate);

        return Result.success(summary);
    }

    private List<Map<String, Object>> fillMonthlyData(List<Map<String, Object>> dbData) {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, Object> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dbData) {
            String month = row.get("month").toString();
            dataMap.put(month, row.get("count"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            String month = date.format(formatter);
            Map<String, Object> item = new HashMap<>();
            item.put("month", month);
            item.put("count", dataMap.getOrDefault(month, 0L));
            result.add(item);
        }
        return result;
    }
}
