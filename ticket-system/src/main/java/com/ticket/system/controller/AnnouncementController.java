package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.Result;
import com.ticket.system.entity.Announcement;
import com.ticket.system.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 公告列表（无需登录）
     */
    @GetMapping("/list")
    public Result<IPage<Announcement>> getAnnouncementList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<Announcement> result = announcementService.getAnnouncementList(page, size);
        return Result.success(result);
    }

    /**
     * 公告详情（无需登录）
     */
    @GetMapping("/detail/{id}")
    public Result<Announcement> getAnnouncementDetail(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementDetail(id);
        return Result.success(announcement);
    }
}
