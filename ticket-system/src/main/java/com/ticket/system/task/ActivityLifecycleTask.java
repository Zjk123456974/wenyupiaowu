package com.ticket.system.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.entity.Activity;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动生命周期定时任务
 *
 * 业务背景：活动抢票结束（或开演）后，若不及时下架，用户在活动列表页仍能看到已结束的场次，
 * 点进详情才发现"抢票已结束"，属于体验缺陷；同时这些场次会继续占用首页热门推荐的坑位。
 *
 * 解决方式：
 * 1. 每 5 分钟扫描一次，把"抢票已结束"或"已开演"且仍处于上架状态(status=3)的活动，
 *    自动置为"已结束"(status=4)，与运营手动下架的状态保持一致。
 * 2. 每日 03:00 触发一次「活动滚动排期」，把在架/已下架活动重排到未来的交错日历，
 *    保证无论系统运行多久，活动列表与首页热门永远有"售票中/即将开演"的场次（演示数据常新）。
 */
@Slf4j
@Component
public class ActivityLifecycleTask {

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private ActivityService activityService;

    /**
     * 每5分钟扫描一次，自动下架已过期的活动
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void autoCloseExpiredActivities() {
        try {
            LocalDateTime now = LocalDateTime.now();

            LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Activity::getStatus, 3) // 仍处于上架状态
                   .and(w -> w.isNull(Activity::getGrabEndTime).or().lt(Activity::getGrabEndTime, now))
                   .and(w -> w.isNull(Activity::getStartTime).or().lt(Activity::getStartTime, now));

            List<Activity> expired = activityMapper.selectList(wrapper);
            if (expired.isEmpty()) {
                return;
            }

            int count = 0;
            for (Activity activity : expired) {
                try {
                    Activity update = new Activity();
                    update.setId(activity.getId());
                    update.setStatus(4); // 4 = 已结束
                    update.setUpdateTime(now);
                    activityMapper.updateById(update);
                    count++;
                } catch (Exception e) {
                    log.error("自动下架活动{}失败", activity.getId(), e);
                }
            }
            if (count > 0) {
                log.info("自动下架{}个已过期活动", count);
            }
        } catch (Exception e) {
            log.error("活动生命周期扫描任务异常", e);
        }
    }

    /**
     * 每日 03:00 触发「活动滚动排期」：把在架/已下架活动重排到未来的交错日历，
     * 保证系统长期运行或重启后，活动列表永不空。
     * 注意：登录接口不再触发排期（避免每次登录产生业务数据副作用），仅由本定时任务负责。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyRollingSchedule() {
        try {
            activityService.rescheduleRolling();
        } catch (Exception e) {
            log.error("每日活动滚动排期任务异常", e);
        }
    }
}
