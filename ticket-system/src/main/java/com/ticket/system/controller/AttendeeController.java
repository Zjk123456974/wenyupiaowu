package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Attendee;
import com.ticket.system.service.AttendeeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendee")
public class AttendeeController {

    @Resource
    private RequestContext requestContext;

    @Resource
    private AttendeeService attendeeService;

    /**
     * 获取我的观演人列表
     */
    @GetMapping("/list")
    public Result<List<Attendee>> list() {
        Long userId = requestContext.getCurrentUserId();
        return Result.success(attendeeService.getMyAttendees(userId));
    }

    /**
     * 分页查询观演人
     */
    @GetMapping("/page")
    public Result<IPage<Attendee>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Long userId = requestContext.getCurrentUserId();
        return Result.success(attendeeService.getAttendeePage(userId, page, size, keyword));
    }

    /**
     * 添加观演人
     */
    @PostMapping
    public Result<Attendee> add(@RequestBody Attendee attendee) {
        Long userId = requestContext.getCurrentUserId();
        return Result.success("添加成功", attendeeService.addAttendee(userId, attendee));
    }

    /**
     * 修改观演人信息
     */
    @PutMapping("/{id}")
    public Result<Attendee> update(@PathVariable Long id, @RequestBody Attendee attendee) {
        Long userId = requestContext.getCurrentUserId();
        return Result.success("修改成功", attendeeService.updateAttendee(userId, id, attendee));
    }

    /**
     * 删除观演人
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        attendeeService.deleteAttendee(userId, id);
        return Result.success("删除成功", null);
    }

    /**
     * 设置默认观演人
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        attendeeService.setDefault(userId, id);
        return Result.success("设置成功", null);
    }

    /**
     * 校验同场活动是否重复购票
     */
    @GetMapping("/check-duplicate")
    public Result<Map<String, Object>> checkDuplicate(
            @RequestParam Long activityId,
            @RequestParam String idCard) {
        Long userId = requestContext.getCurrentUserId();
        boolean duplicate = attendeeService.checkDuplicatePurchase(userId, activityId, idCard);
        return Result.success(Map.of("duplicate", duplicate));
    }
}
