package com.ticket.system.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.entity.Activity;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.service.ReservationService;
import com.ticket.system.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ReservationQueueTask {

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private ReservationService reservationService;

    @Resource
    private StockService stockService;

    /**
     * 每15秒扫描一次：
     * 1. 为已进入抢票窗口的活动初始化 Redis 库存缓存（仅首次）
     * 2. 处理预约队列，为排队用户自动出票
     */
    @Scheduled(fixedRate = 15000)
    public void processReservations() {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 3)
               .le(Activity::getGrabStartTime, LocalDateTime.now());

        List<Activity> activities = activityMapper.selectList(wrapper);
        if (activities.isEmpty()) {
            return;
        }

        for (Activity activity : activities) {
            try {
                ensureStockInitialized(activity);
                reservationService.processReservationQueue(activity.getId());
            } catch (Exception e) {
                log.error("处理活动{}预约队列失败", activity.getId(), e);
            }
        }
    }

    /**
     * 确保活动的 Redis 库存缓存已初始化
     */
    private void ensureStockInitialized(Activity activity) {
        Integer cached = stockService.getStock(activity.getId());
        if (cached == null) {
            int remain = (activity.getTotalTickets() != null ? activity.getTotalTickets() : 0)
                       - (activity.getSoldTickets() != null ? activity.getSoldTickets() : 0);
            if (remain < 0) remain = 0;
            stockService.initStock(activity.getId(), remain);
            log.info("活动{}库存缓存初始化: {}张", activity.getId(), remain);
        }
    }
}
