package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.ActivityReview;
import com.ticket.system.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RequestContext requestContext;

    /**
     * 权限校验：管理员或运营
     */
    private void checkAdminOrOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无操作权限");
        }
    }

    // ==================== 用户接口 ====================

    /**
     * 发布评价
     */
    @PostMapping("/add")
    public Result<ActivityReview> addReview(@RequestBody Map<String, Object> params) {
        Long userId = requestContext.getCurrentUserId();
        Long activityId = params.get("activityId") != null ? Long.valueOf(params.get("activityId").toString()) : null;
        // 兼容前端两种字段名：rating（我的评价页）/ score（活动详情页）
        Object ratingVal = params.get("rating") != null ? params.get("rating") : params.get("score");
        Integer rating = ratingVal != null ? Integer.valueOf(ratingVal.toString()) : null;
        String content = params.get("content") != null ? params.get("content").toString() : null;
        String images = params.get("images") != null ? params.get("images").toString() : null;
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        ActivityReview review = reviewService.addReview(userId, activityId, rating, content, images);
        return Result.success(review);
    }

    /**
     * 获取公开评价列表
     */
    @GetMapping("/public/{activityId}")
    public Result<List<ActivityReview>> publicReviews(@PathVariable Long activityId) {
        List<ActivityReview> list = reviewService.getPublicReviews(activityId);
        return Result.success(list);
    }

    /**
     * 评价统计
     */
    @GetMapping("/statistics/{activityId}")
    public Result<Map<String, Object>> statistics(@PathVariable Long activityId) {
        Map<String, Object> statistics = reviewService.getReviewStatistics(activityId);
        return Result.success(statistics);
    }

    /**
     * 我的评价历史
     */
    @GetMapping("/my")
    public Result<IPage<ActivityReview>> myReviews(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = requestContext.getCurrentUserId();
        IPage<ActivityReview> result = reviewService.getMyReviews(userId, page, size);
        return Result.success(result);
    }

    /**
     * 用户删除自己的评价
     */
    @DeleteMapping("/my/{id}")
    public Result<Void> deleteMyReview(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        reviewService.deleteMyReview(userId, id);
        return Result.success();
    }

    /**
     * 获取用户可评价的活动列表（已购票但未评价）
     */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pendingReviews() {
        Long userId = requestContext.getCurrentUserId();
        List<Map<String, Object>> list = reviewService.getPendingReviews(userId);
        return Result.success(list);
    }

    // ==================== 管理接口 ====================

    /**
     * 评价分页查询
     */
    @GetMapping("/admin/page")
    public Result<IPage<ActivityReview>> reviewPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Integer status) {
        checkAdminOrOperator();
        IPage<ActivityReview> result = reviewService.getReviewPage(page, size, activityId, status);
        return Result.success(result);
    }

    /**
     * 审核通过
     */
    @OperLog(module = "评价管理", operation = "通过评价")
    @PutMapping("/admin/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        checkAdminOrOperator();
        reviewService.approveReview(id);
        return Result.success();
    }

    /**
     * 审核驳回
     */
    @OperLog(module = "评价管理", operation = "驳回评价")
    @PutMapping("/admin/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        checkAdminOrOperator();
        reviewService.rejectReview(id);
        return Result.success();
    }

    /**
     * 隐藏评价
     */
    @OperLog(module = "评价管理", operation = "隐藏评价")
    @PutMapping("/admin/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        checkAdminOrOperator();
        reviewService.hideReview(id);
        return Result.success();
    }

    /**
     * 删除评价
     */
    @OperLog(module = "评价管理", operation = "删除评价")
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdminOrOperator();
        reviewService.deleteReview(id);
        return Result.success();
    }

    /**
     * 回复评价
     */
    @OperLog(module = "评价管理", operation = "回复评价")
    @PutMapping("/admin/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdminOrOperator();
        Long replierId = requestContext.getCurrentUserId();
        // 兼容 reply / replyContent 两种字段名
        Object replyVal = params.get("reply") != null ? params.get("reply") : params.get("replyContent");
        String reply = replyVal != null ? replyVal.toString() : null;
        reviewService.replyReview(replierId, id, reply);
        return Result.success();
    }
}
