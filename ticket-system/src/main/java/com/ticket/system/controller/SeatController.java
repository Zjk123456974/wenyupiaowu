package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.service.impl.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 选座控制器（对标大麦「在线选座」）
 */
@RestController
@RequestMapping("/seat")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private RequestContext requestContext;

    /**
     * 获取活动座位图（布局 + 已售 + 锁定）
     */
    @GetMapping("/map")
    public Result<Map<String, Object>> getSeatMap(@RequestParam Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        return Result.success(seatService.getSeatMap(activityId));
    }
}
