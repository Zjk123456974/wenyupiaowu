package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Reservation;

import java.util.Map;

public interface ReservationService {

    /**
     * 提交预约申请
     *
     * @param seatType      预约座位类型（可选，未选则开抢后按默认票档出票）
     * @param ticketPriceId 预约票档ID（可选，开抢后按该票档直接出票）
     */
    Reservation createReservation(Long userId, Long activityId, String seatType, Long ticketPriceId);

    /**
     * 查询我的预约列表
     */
    IPage<Reservation> getMyReservations(Long userId, Integer page, Integer size);

    /**
     * 查询某活动预约详情（排队序号、总排队人数）
     */
    Reservation getReservationDetail(Long userId, Long activityId);

    /**
     * 取消预约
     */
    void cancelReservation(Long userId, Long activityId);

    /**
     * 抢票开启后处理预约队列（优先出票给预约用户）
     */
    void processReservationQueue(Long activityId);

    /**
     * 处理补票等待队列（退票/释放后按顺序补票）
     */
    void processWaitQueue(Long activityId);

    /**
     * 加入候补补票队列（抢票失败/售罄时用户可主动排队等退票）
     *
     * @return 排队信息：queuePosition(排队位置), totalWaiters(总排队人数)
     */
    Map<String, Object> joinWaitQueue(Long userId, Long activityId);

    /**
     * 退出候补补票队列
     */
    void leaveWaitQueue(Long userId, Long activityId);

    /**
     * 查询用户在某活动候补队列中的位置
     *
     * @return 排队信息：inQueue(是否在队列中), queuePosition, totalWaiters
     */
    Map<String, Object> getWaitQueuePosition(Long userId, Long activityId);

    /**
     * 查询我的候补列表
     */
    IPage<Map<String, Object>> getMyWaitList(Long userId, Integer page, Integer size);
}
