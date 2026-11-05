package com.ticket.system.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存对账定时任务：
 * <p>
 * 定期比对 Redis 预扣库存与 MySQL 实际库存（sold_tickets），
 * 当偏差超过阈值时自动校正，防止因异常导致的库存不一致。
 * <p>
 * 策略：
 * 1. 每分钟扫描一次在售活动（轻量级，仅比对数字）
 * 2. 偏差 = Redis 余量 - (total_tickets - sold_tickets)
 * 3. 偏差在 [-warnThreshold, warnThreshold] 内：正常，不处理
 * 4. 偏差超出阈值：以 DB 为准重设 Redis 库存（因为 DB 是持久化事实来源）
 * 5. 偏差为正数（Redis 比 DB 多）说明有漏扣，校正后库存减少，不影响已售
 * 6. 偏差为负数（Redis 比 DB 少）说明有漏回滚，校正后库存增加，可能多出几张票
 * 7. 校正操作记录 warn 日志，便于审计
 */
@Slf4j
@Component
public class StockReconciliationTask {

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private TicketPriceMapper ticketPriceMapper;

    @Resource
    private StockService stockService;

    @Value("${stock.warn-threshold:5}")
    private Integer warnThreshold;

    /**
     * 每分钟对账一次：Redis 活动总库存 vs MySQL sold_tickets
     * 初始延迟 30 秒，保证应用启动完成后再开始对账
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void reconcileActivityStock() {
        log.debug("开始库存对账...");

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 3); // 仅在售活动
        List<Activity> activities = activityMapper.selectList(wrapper);

        if (activities.isEmpty()) {
            return;
        }

        int fixedCount = 0;
        for (Activity activity : activities) {
            try {
                Integer redisStock = stockService.getStock(activity.getId());
                if (redisStock == null) {
                    // Redis 无缓存，跳过（首次抢票时会自动初始化）
                    continue;
                }

                int total = activity.getTotalTickets() != null ? activity.getTotalTickets() : 0;
                int sold = activity.getSoldTickets() != null ? activity.getSoldTickets() : 0;
                int dbRemain = total - sold;
                if (dbRemain < 0) dbRemain = 0;

                int diff = redisStock - dbRemain;

                if (Math.abs(diff) > warnThreshold) {
                    log.warn("活动[{}][{}]库存偏差超过阈值: Redis余量={}, DB余量={}, 差值={}, 自动校正",
                            activity.getId(), activity.getTitle(), redisStock, dbRemain, diff);
                    stockService.initStock(activity.getId(), dbRemain);
                    fixedCount++;
                }
            } catch (Exception e) {
                log.error("对账活动{}库存失败", activity.getId(), e);
            }
        }

        if (fixedCount > 0) {
            log.info("本次库存对账校正了{}个活动的库存偏差", fixedCount);
        }
    }

    /**
     * 每5分钟对账一次票档库存
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void reconcileTicketPriceStock() {
        log.debug("开始票档库存对账...");

        LambdaQueryWrapper<Activity> actWrapper = new LambdaQueryWrapper<>();
        actWrapper.eq(Activity::getStatus, 3);
        List<Activity> activities = activityMapper.selectList(actWrapper);

        if (activities.isEmpty()) {
            return;
        }

        int fixedCount = 0;
        for (Activity activity : activities) {
            try {
                LambdaQueryWrapper<TicketPrice> tpWrapper = new LambdaQueryWrapper<>();
                tpWrapper.eq(TicketPrice::getActivityId, activity.getId())
                        .eq(TicketPrice::getStatus, 1);
                List<TicketPrice> prices = ticketPriceMapper.selectList(tpWrapper);

                for (TicketPrice tp : prices) {
                    int total = tp.getTotalStock() != null ? tp.getTotalStock() : 0;
                    int sold = tp.getSoldStock() != null ? tp.getSoldStock() : 0;
                    int remain = total - sold;
                    if (remain < 0) {
                        // 票档超卖（理论上不应发生，因为有原子条件扣减）。
                        // 用 WARN 而非 ERROR：这是数据异常需要关注，但不是系统故障，
                        // 避免每5分钟的固定告警把真正的 ERROR 淹掉
                        log.warn("票档[{}][{}]库存异常: total={}, sold={}, 差额={}，请核对数据",
                                tp.getId(), tp.getSeatType(), total, sold, remain);
                        fixedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("对账活动{}票档库存失败", activity.getId(), e);
            }
        }

        if (fixedCount > 0) {
            log.warn("本次票档库存对账发现{}个异常票档", fixedCount);
        }
    }
}
