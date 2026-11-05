package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.Reservation;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/operator/stats")
public class OperatorStatsController {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private RequestContext requestContext;

    private void checkOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无运营权限");
        }
    }

    /**
     * 单活动数据统计
     */
    @GetMapping("/activity/{activityId}")
    public Result<Map<String, Object>> getActivityStats(@PathVariable Long activityId) {
        checkOperator();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 预约人数
        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getActivityId, activityId));

        // 成交订单数（status=1已支付）
        Long successOrderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getActivityId, activityId).eq(Orders::getStatus, 1));

        // 取消订单数（status=2已取消）
        Long cancelOrderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getActivityId, activityId).eq(Orders::getStatus, 2));

        // 过期订单数（status=3已过期）
        Long expiredOrderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getActivityId, activityId).eq(Orders::getStatus, 3));

        // 退款订单数（status=5已退款）
        Long refundedOrderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getActivityId, activityId).eq(Orders::getStatus, 5));

        Map<String, Object> stats = new HashMap<>();
        stats.put("reservationCount", reservationCount);
        stats.put("ticketCount", activity.getSoldTickets());
        stats.put("successOrderCount", successOrderCount);
        stats.put("cancelOrderCount", cancelOrderCount);
        stats.put("expiredOrderCount", expiredOrderCount);
        stats.put("refundedOrderCount", refundedOrderCount);
        return Result.success(stats);
    }

    /**
     * 活动热度监控（预约量+购票量+浏览量）
     */
    @GetMapping("/activity/{activityId}/hot")
    public Result<Map<String, Object>> getActivityHot(@PathVariable Long activityId) {
        checkOperator();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getActivityId, activityId));

        Map<String, Object> hot = new HashMap<>();
        hot.put("reservationCount", reservationCount);
        hot.put("soldTickets", activity.getSoldTickets());
        hot.put("viewCount", activity.getViewCount());
        hot.put("hotScore", reservationCount + activity.getSoldTickets() + activity.getViewCount());
        return Result.success(hot);
    }
}
