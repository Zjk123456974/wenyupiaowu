package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Announcement;
import com.ticket.system.service.AnnouncementService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/announcement")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 管理端公告分页（含下架公告）
     */
    @GetMapping("/page")
    public Result<IPage<Announcement>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        return Result.success(announcementService.getAdminPage(page, size, status));
    }

    /**
     * 发布公告
     */
    @OperLog(module = "公告管理", operation = "发布公告")
    @PostMapping
    public Result<Void> addAnnouncement(@RequestBody Map<String, Object> params) {
        checkAdmin();
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String content = params.get("content") != null ? params.get("content").toString() : null;
        Integer isTop = params.get("isTop") != null ? Integer.valueOf(params.get("isTop").toString()) : 0;
        announcementService.addAnnouncement(title, content, isTop);
        return Result.success();
    }

    /**
     * 编辑公告
     */
    @OperLog(module = "公告管理", operation = "编辑公告")
    @PutMapping("/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String content = params.get("content") != null ? params.get("content").toString() : null;
        Integer isTop = params.get("isTop") != null ? Integer.valueOf(params.get("isTop").toString()) : null;
        announcementService.updateAnnouncement(id, title, content, isTop);
        return Result.success();
    }

    /**
     * 删除公告
     */
    @OperLog(module = "公告管理", operation = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        checkAdmin();
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    /**
     * 置顶/取消置顶
     */
    @OperLog(module = "公告管理", operation = "置顶公告")
    @PutMapping("/{id}/top")
    public Result<Void> topAnnouncement(@PathVariable Long id) {
        checkAdmin();
        announcementService.topAnnouncement(id);
        return Result.success();
    }

    /**
     * 下架公告
     */
    @OperLog(module = "公告管理", operation = "下架公告")
    @PutMapping("/{id}/offline")
    public Result<Void> offlineAnnouncement(@PathVariable Long id) {
        checkAdmin();
        announcementService.offlineAnnouncement(id);
        return Result.success();
    }

    /**
     * 上架公告
     */
    @OperLog(module = "公告管理", operation = "上架公告")
    @PutMapping("/{id}/online")
    public Result<Void> onlineAnnouncement(@PathVariable Long id) {
        checkAdmin();
        announcementService.onlineAnnouncement(id);
        return Result.success();
    }
}
