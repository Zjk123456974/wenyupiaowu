package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.ActivityTag;
import com.ticket.system.service.TagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private void checkAdminOrOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无操作权限");
        }
    }

    /**
     * 获取所有标签（管理端）
     */
    @GetMapping("/list")
    public Result<List<ActivityTag>> list() {
        checkAdminOrOperator();
        return Result.success(tagService.getAllTags());
    }

    /**
     * 获取启用标签（前端用，需登录）
     */
    @GetMapping("/enabled")
    public Result<List<ActivityTag>> enabled() {
        return Result.success(tagService.getEnabledTags());
    }

    /**
     * 新增标签
     */
    @OperLog(module = "标签管理", operation = "新增标签")
    @PostMapping
    public Result<ActivityTag> add(@RequestBody ActivityTag tag) {
        checkAdmin();
        return Result.success("新增成功", tagService.addTag(tag));
    }

    /**
     * 编辑标签
     */
    @OperLog(module = "标签管理", operation = "编辑标签")
    @PutMapping("/{id}")
    public Result<ActivityTag> update(@PathVariable Long id, @RequestBody ActivityTag tag) {
        checkAdmin();
        return Result.success("修改成功", tagService.updateTag(id, tag));
    }

    /**
     * 删除标签
     */
    @OperLog(module = "标签管理", operation = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        tagService.deleteTag(id);
        return Result.success("删除成功", null);
    }

    /**
     * 绑定活动标签
     */
    @PostMapping("/bind/{activityId}")
    public Result<Void> bind(@PathVariable Long activityId, @RequestBody List<Long> tagIds) {
        checkAdminOrOperator();
        tagService.bindActivityTags(activityId, tagIds);
        return Result.success("绑定成功", null);
    }

    /**
     * 获取活动的标签ID列表
     */
    @GetMapping("/activity/{activityId}")
    public Result<List<Long>> activityTags(@PathVariable Long activityId) {
        return Result.success(tagService.getActivityTagIds(activityId));
    }

    /**
     * 标签统计数据
     */
    @GetMapping("/statistics")
    public Result<List<Map<String, Object>>> statistics() {
        checkAdmin();
        return Result.success(tagService.getTagStatistics());
    }
}
