package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Orders;
import com.ticket.system.service.OrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private RequestContext requestContext;

    @Resource
    private OrderService orderService;

    /**
     * 我的订单列表
     */
    @GetMapping("/list")
    public Result<IPage<Orders>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = requestContext.getCurrentUserId();
        IPage<Orders> result = orderService.getMyOrders(userId, page, size, status);
        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{id}")
    public Result<Orders> detail(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        Orders order = orderService.getOrderDetail(userId, id);
        return Result.success(order);
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        orderService.payOrder(userId, id);
        return Result.success("支付成功", null);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        orderService.cancelOrder(userId, id);
        return Result.success("取消订单成功", null);
    }

    /**
     * 查看电子票务凭证
     */
    @GetMapping("/ticket/{id}")
    public Result<Map<String, Object>> getElectronicTicket(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        Map<String, Object> ticket = orderService.getElectronicTicket(userId, id);
        return Result.success(ticket);
    }

    /**
     * 删除订单（用户删除自己的终态订单）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        orderService.deleteOrder(userId, id);
        return Result.success();
    }
}
