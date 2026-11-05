package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Advertising;
import com.ticket.system.mapper.AdvertisingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 广告位管理控制器
 */
@RestController
@RequestMapping("/ad")
public class AdvertisingController {

    @Autowired
    private AdvertisingMapper advertisingMapper;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    // ==================== 管理端接口 ====================

    /**
     * 广告分页查询（管理端）
     */
    @GetMapping("/page")
    public Result<IPage<Advertising>> getAdPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String position) {
        checkAdmin();
        Page<Advertising> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Advertising> wrapper = new LambdaQueryWrapper<>();
        if (position != null && !position.trim().isEmpty()) {
            wrapper.eq(Advertising::getPosition, position);
        }
        wrapper.orderByAsc(Advertising::getSortOrder)
               .orderByDesc(Advertising::getCreateTime);
        IPage<Advertising> result = advertisingMapper.selectPage(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 新增广告
     */
    @OperLog(module = "广告管理", operation = "新增广告")
    @PostMapping
    public Result<Advertising> addAd(@RequestBody Map<String, Object> params) {
        checkAdmin();
        Advertising ad = new Advertising();
        ad.setTitle(params.get("title") != null ? params.get("title").toString() : null);
        ad.setImageUrl(params.get("imageUrl") != null ? params.get("imageUrl").toString() : null);
        ad.setLinkUrl(params.get("linkUrl") != null ? params.get("linkUrl").toString() : null);
        ad.setPosition(params.get("position") != null ? params.get("position").toString() : "sidebar");
        ad.setSortOrder(params.get("sort") != null ? Integer.valueOf(params.get("sort").toString()) : 0);
        ad.setStatus(params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : 1);

        if (params.get("activityId") != null && !params.get("activityId").toString().isEmpty()) {
            ad.setActivityId(Long.valueOf(params.get("activityId").toString()));
        }

        String startTime = params.get("startTime") != null ? params.get("startTime").toString() : null;
        String endTime = params.get("endTime") != null ? params.get("endTime").toString() : null;
        if (startTime != null && !startTime.isEmpty()) {
            ad.setStartTime(LocalDateTime.parse(startTime.replace(' ', 'T')));
        }
        if (endTime != null && !endTime.isEmpty()) {
            ad.setEndTime(LocalDateTime.parse(endTime.replace(' ', 'T')));
        }

        if (ad.getTitle() == null || ad.getTitle().trim().isEmpty()) {
            throw new BusinessException("广告标题不能为空");
        }
        if (ad.getImageUrl() == null || ad.getImageUrl().trim().isEmpty()) {
            throw new BusinessException("广告图片不能为空");
        }

        ad.setCreateTime(LocalDateTime.now());
        ad.setUpdateTime(LocalDateTime.now());
        advertisingMapper.insert(ad);
        return Result.success(ad);
    }

    /**
     * 更新广告
     */
    @OperLog(module = "广告管理", operation = "编辑广告")
    @PutMapping("/{id}")
    public Result<Advertising> updateAd(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Advertising ad = advertisingMapper.selectById(id);
        if (ad == null) {
            throw new BusinessException("广告不存在");
        }

        if (params.get("title") != null) ad.setTitle(params.get("title").toString());
        if (params.get("imageUrl") != null) ad.setImageUrl(params.get("imageUrl").toString());
        if (params.get("linkUrl") != null) ad.setLinkUrl(params.get("linkUrl").toString());
        if (params.get("position") != null) ad.setPosition(params.get("position").toString());
        if (params.get("sort") != null) ad.setSortOrder(Integer.valueOf(params.get("sort").toString()));
        if (params.get("status") != null) ad.setStatus(Integer.valueOf(params.get("status").toString()));

        if (params.get("activityId") != null && !params.get("activityId").toString().isEmpty()) {
            ad.setActivityId(Long.valueOf(params.get("activityId").toString()));
        } else {
            ad.setActivityId(null);
        }

        String startTime = params.get("startTime") != null ? params.get("startTime").toString() : null;
        String endTime = params.get("endTime") != null ? params.get("endTime").toString() : null;
        if (startTime != null && !startTime.isEmpty()) {
            ad.setStartTime(LocalDateTime.parse(startTime.replace(' ', 'T')));
        }
        if (endTime != null && !endTime.isEmpty()) {
            ad.setEndTime(LocalDateTime.parse(endTime.replace(' ', 'T')));
        }

        ad.setUpdateTime(LocalDateTime.now());
        advertisingMapper.updateById(ad);
        return Result.success(ad);
    }

    /**
     * 删除广告
     */
    @OperLog(module = "广告管理", operation = "删除广告")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAd(@PathVariable Long id) {
        checkAdmin();
        Advertising ad = advertisingMapper.selectById(id);
        if (ad == null) {
            throw new BusinessException("广告不存在");
        }
        advertisingMapper.deleteById(id);
        return Result.success();
    }

    // ==================== 用户端接口 ====================

    /**
     * 获取启用中的广告（用户端）
     * 可按位置筛选，只返回状态为启用且在有效期内广告
     */
    @GetMapping("/active")
    public Result<List<Advertising>> getActiveAds(
            @RequestParam(required = false) String position) {
        LambdaQueryWrapper<Advertising> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Advertising::getStatus, 1);
        if (position != null && !position.trim().isEmpty()) {
            wrapper.eq(Advertising::getPosition, position);
        }
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w.isNull(Advertising::getStartTime).or().le(Advertising::getStartTime, now))
               .and(w -> w.isNull(Advertising::getEndTime).or().ge(Advertising::getEndTime, now));
        wrapper.orderByAsc(Advertising::getSortOrder)
               .orderByDesc(Advertising::getCreateTime);
        List<Advertising> ads = advertisingMapper.selectList(wrapper);
        return Result.success(ads);
    }
}
