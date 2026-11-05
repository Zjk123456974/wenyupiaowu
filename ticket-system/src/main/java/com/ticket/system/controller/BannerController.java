package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Banner;
import com.ticket.system.mapper.BannerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private static final String DEFAULT_BANNER_IMAGE = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1200&h=600&fit=crop";

    @GetMapping("/list")
    public Result<List<Banner>> getBannerList() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
               .orderByAsc(Banner::getSortOrder)
               .orderByDesc(Banner::getCreateTime);
        List<Banner> banners = bannerMapper.selectList(wrapper);
        for (Banner banner : banners) {
            if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
                banner.setImageUrl(DEFAULT_BANNER_IMAGE);
            }
        }
        return Result.success(banners);
    }

    @GetMapping("/admin/list")
    public Result<IPage<Banner>> getAdminBannerList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdmin();
        Page<Banner> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Banner::getCreateTime);
        IPage<Banner> result = bannerMapper.selectPage(pageParam, wrapper);
        for (Banner banner : result.getRecords()) {
            if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
                banner.setImageUrl(DEFAULT_BANNER_IMAGE);
            }
        }
        return Result.success(result);
    }

    @OperLog(module = "轮播图管理", operation = "新增轮播图")

    @PostMapping
    public Result<Banner> createBanner(@RequestBody Map<String, Object> params) {
        checkAdmin();

        String imageUrl = params.get("imageUrl") != null ? params.get("imageUrl").toString() : null;
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String description = params.get("description") != null ? params.get("description").toString() : null;
        Long activityId = params.get("activityId") != null ? Long.valueOf(params.get("activityId").toString()) : null;
        Integer sortOrder = params.get("sortOrder") != null ? Integer.valueOf(params.get("sortOrder").toString()) : 0;

        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("标题不能为空");
        }

        Banner banner = new Banner();
        banner.setImageUrl(imageUrl);
        banner.setTitle(title);
        banner.setDescription(description);
        banner.setActivityId(activityId);
        banner.setSortOrder(sortOrder);
        banner.setStatus(1);
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());

        bannerMapper.insert(banner);
        return Result.success(banner);
    }

    @OperLog(module = "轮播图管理", operation = "编辑轮播图")

    @PutMapping("/{id}")
    public Result<Banner> updateBanner(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }

        String imageUrl = params.get("imageUrl") != null ? params.get("imageUrl").toString() : null;
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String description = params.get("description") != null ? params.get("description").toString() : null;
        boolean hasActivityId = params.containsKey("activityId");
        Long activityId = hasActivityId && params.get("activityId") != null ? Long.valueOf(params.get("activityId").toString()) : null;
        Integer sortOrder = params.get("sortOrder") != null ? Integer.valueOf(params.get("sortOrder").toString()) : null;

        // 使用 UpdateWrapper 显式 set，保证 activityId 传 null 时能真正清空数据库字段
        LambdaUpdateWrapper<Banner> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Banner::getId, id);
        updateWrapper.set(Banner::getUpdateTime, LocalDateTime.now());
        if (imageUrl != null) updateWrapper.set(Banner::getImageUrl, imageUrl);
        if (title != null) updateWrapper.set(Banner::getTitle, title);
        if (description != null) updateWrapper.set(Banner::getDescription, description);
        // 允许清空关联活动（前端显式传 null 时清除，未传时保持原值）
        if (hasActivityId) updateWrapper.set(Banner::getActivityId, activityId);
        if (sortOrder != null) updateWrapper.set(Banner::getSortOrder, sortOrder);

        bannerMapper.update(null, updateWrapper);

        Banner updated = bannerMapper.selectById(id);
        return Result.success(updated);
    }

    @OperLog(module = "轮播图管理", operation = "切换轮播图状态")

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }

        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }

        banner.setStatus(status);
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(banner);
        return Result.success();
    }

    @OperLog(module = "轮播图管理", operation = "删除轮播图")

    @DeleteMapping("/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        checkAdmin();
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }

        bannerMapper.deleteById(id);
        return Result.success();
    }
}