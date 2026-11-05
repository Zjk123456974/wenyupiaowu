package com.ticket.system.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.entity.Orders;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutTask {

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private OrderService orderService;

    /**
     * 每30秒扫描一次超时未支付订单，自动取消并释放库存
     */
    @Scheduled(fixedRate = 30000)
    public void scanTimeoutOrders() {
        log.debug("开始扫描超时未支付订单...");

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, 0) // 待支付
               .lt(Orders::getExpireTime, LocalDateTime.now());

        List<Orders> timeoutOrders = ordersMapper.selectList(wrapper);
        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("发现{}个超时未支付订单", timeoutOrders.size());
        for (Orders order : timeoutOrders) {
            try {
                orderService.expireOrder(order.getId());
            } catch (Exception e) {
                log.error("处理超时订单{}失败", order.getOrderNo(), e);
            }
        }
    }
}
