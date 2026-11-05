package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Favorite;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.Reservation;
import com.ticket.system.entity.TransactionFlow;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.ReservationMapper;
import com.ticket.system.mapper.TransactionFlowMapper;
import com.ticket.system.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/center")
public class UserCenterController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private TransactionFlowMapper transactionFlowMapper;

    @Autowired
    private RequestContext requestContext;

    /**
     * 个人数据汇总
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Long userId = requestContext.getCurrentUserId();

        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, userId));

        Long ticketCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId).eq(Orders::getStatus, 1));

        Long orderCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("reservationCount", reservationCount);
        stats.put("ticketCount", ticketCount);
        stats.put("orderCount", orderCount);
        return Result.success(stats);
    }

    /**
     * 收藏活动
     */
    @PostMapping("/favorite/{activityId}")
    public Result<Void> addFavorite(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        favoriteService.addFavorite(userId, activityId);
        return Result.success();
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/favorite/{activityId}")
    public Result<Void> removeFavorite(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        favoriteService.removeFavorite(userId, activityId);
        return Result.success();
    }

    /**
     * 收藏列表
     */
    @GetMapping("/favorites")
    public Result<IPage<Favorite>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = requestContext.getCurrentUserId();
        IPage<Favorite> result = favoriteService.getMyFavorites(userId, page, size);
        return Result.success(result);
    }

    /**
     * 消费统计
     */
    @GetMapping("/consumption/stats")
    public Result<Map<String, Object>> getConsumptionStats() {
        Long userId = requestContext.getCurrentUserId();

        // 累计消费：历史上已支付(1)和已退款(5)的订单都算曾经消费过
        LambdaQueryWrapper<Orders> paidWrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .in(Orders::getStatus, 1, 5);
        java.util.List<Orders> paidOrders = ordersMapper.selectList(paidWrapper);
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (Orders o : paidOrders) {
            if (o.getTotalAmount() != null) {
                totalSpent = totalSpent.add(o.getTotalAmount());
            }
        }

        // 实际退款金额：以退款流水（实退到账）为准，而非订单全额
        java.util.List<TransactionFlow> refundFlows = transactionFlowMapper.selectList(
                new LambdaQueryWrapper<TransactionFlow>()
                        .eq(TransactionFlow::getUserId, userId)
                        .eq(TransactionFlow::getFlowType, 2));
        BigDecimal totalRefunded = BigDecimal.ZERO;
        for (TransactionFlow f : refundFlows) {
            if (f.getAmount() != null) {
                totalRefunded = totalRefunded.add(f.getAmount());
            }
        }

        Long paidCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId).eq(Orders::getStatus, 1));
        Long cancelledCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId).eq(Orders::getStatus, 2));
        Long expiredCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId).eq(Orders::getStatus, 3));
        Long refundedCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId).eq(Orders::getStatus, 5));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSpent", totalSpent);
        stats.put("totalRefunded", totalRefunded);
        stats.put("netSpent", totalSpent.subtract(totalRefunded));
        stats.put("paidCount", paidCount);
        stats.put("cancelledCount", cancelledCount);
        stats.put("expiredCount", expiredCount);
        stats.put("refundedCount", refundedCount);
        return Result.success(stats);
    }

    /**
     * 交易流水列表
     */
    @GetMapping("/consumption/flows")
    public Result<IPage<TransactionFlow>> getConsumptionFlows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer flowType) {
        Long userId = requestContext.getCurrentUserId();
        Page<TransactionFlow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TransactionFlow> wrapper = new LambdaQueryWrapper<TransactionFlow>()
                .eq(TransactionFlow::getUserId, userId);
        if (flowType != null) {
            wrapper.eq(TransactionFlow::getFlowType, flowType);
        }
        wrapper.orderByDesc(TransactionFlow::getPayTime);
        IPage<TransactionFlow> result = transactionFlowMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }
}
