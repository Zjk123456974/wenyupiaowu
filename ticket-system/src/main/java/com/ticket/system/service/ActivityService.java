package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Activity;

import java.math.BigDecimal;

public interface ActivityService {

    /**
     * 分页查询已上架活动列表
     */
    IPage<Activity> getActivityPage(Integer page, Integer size, Long categoryId, String keyword,
                                    String startTime, String endTime,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    String sortField, String sortOrder, Long tagId);

    /**
     * 获取活动详情，浏览量+1
     */
    Activity getActivityDetail(Long id);

    /**
     * 获取热门活动推荐（按soldTickets+viewCount排序取前10）
     */
    Object getHotActivities();

    /**
     * 计算活动的抢票状态
     */
    String getActivityGrabStatus(Activity activity);

    /**
     * 活动滚动排期：把在架(status=3)与已下架(status=4)的活动，按 id 顺序交错重排到
     * 「今天+1天」起的未来日历（步长5天，艺术展览类展期45天），统一置为在架(status=3)。
     * 目的：保证演示数据常新、活动列表永不空；被自动下架任务误关的活动也会被自动救活。
     * 由「登录成功」与「每日03:00定时任务」触发，幂等、可重复执行。
     */
    void rescheduleRolling();
}
