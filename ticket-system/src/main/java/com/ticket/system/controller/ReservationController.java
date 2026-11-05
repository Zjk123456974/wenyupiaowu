package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Reservation;
import com.ticket.system.service.ReservationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Resource
    private RequestContext requestContext;

    @Resource
    private ReservationService reservationService;

    /**
     * 申请预约：可同时选定想要的票档价位，开抢后按预约票档直接出票
     */
    @PostMapping("/apply")
    public Result<Reservation> apply(@RequestBody(required = false) Map<String, Object> params) {
        Long userId = requestContext.getCurrentUserId();
        Long activityId = params != null && params.get("activityId") != null
                ? Long.valueOf(params.get("activityId").toString()) : null;
        String seatType = params != null && params.get("seatType") != null
                ? params.get("seatType").toString() : null;
        Long ticketPriceId = params != null && params.get("ticketPriceId") != null
                ? Long.valueOf(params.get("ticketPriceId").toString()) : null;
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Reservation reservation = reservationService.createReservation(userId, activityId, seatType, ticketPriceId);
        return Result.success("预约成功", reservation);
    }

    /**
     * 我的预约列表
     */
    @GetMapping("/list")
    public Result<IPage<Reservation>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = requestContext.getCurrentUserId();
        IPage<Reservation> result = reservationService.getMyReservations(userId, page, size);
        return Result.success(result);
    }

    /**
     * 预约详情
     */
    @GetMapping("/detail/{activityId}")
    public Result<Reservation> detail(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        Reservation reservation = reservationService.getReservationDetail(userId, activityId);
        return Result.success(reservation);
    }

    /**
     * 取消预约
     */
    @DeleteMapping("/cancel/{activityId}")
    public Result<Void> cancel(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        reservationService.cancelReservation(userId, activityId);
        return Result.success("取消预约成功", null);
    }

    // ==================== 候补补票队列 ====================

    /**
     * 加入候补队列（抢票失败/售罄时用户可主动排队等退票）
     */
    @PostMapping("/wait/join")
    public Result<Map<String, Object>> joinWaitQueue(@RequestParam Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        Map<String, Object> result = reservationService.joinWaitQueue(userId, activityId);
        return Result.success("候补成功", result);
    }

    /**
     * 退出候补队列
     */
    @DeleteMapping("/wait/leave/{activityId}")
    public Result<Void> leaveWaitQueue(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        reservationService.leaveWaitQueue(userId, activityId);
        return Result.success("已退出候补队列", null);
    }

    /**
     * 查询某活动候补队列中我的位置
     */
    @GetMapping("/wait/position/{activityId}")
    public Result<Map<String, Object>> getWaitPosition(@PathVariable Long activityId) {
        Long userId = requestContext.getCurrentUserId();
        Map<String, Object> result = reservationService.getWaitQueuePosition(userId, activityId);
        return Result.success(result);
    }

    /**
     * 我的候补列表
     */
    @GetMapping("/wait/list")
    public Result<IPage<Map<String, Object>>> getMyWaitList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = requestContext.getCurrentUserId();
        IPage<Map<String, Object>> result = reservationService.getMyWaitList(userId, page, size);
        return Result.success(result);
    }
}
