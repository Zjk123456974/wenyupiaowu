package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.ActivityReview;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    /**
     * 发布评价
     */
    ActivityReview addReview(Long userId, Long activityId, Integer rating, String content, String images);

    /**
     * 评价分页查询
     */
    IPage<ActivityReview> getReviewPage(Integer page, Integer size, Long activityId, Integer status);

    /**
     * 获取已通过的评价（前端展示）
     */
    List<ActivityReview> getPublicReviews(Long activityId);

    /**
     * 审核通过
     */
    void approveReview(Long id);

    /**
     * 审核驳回
     */
    void rejectReview(Long id);

    /**
     * 隐藏评价
     */
    void hideReview(Long id);

    /**
     * 删除评价
     */
    void deleteReview(Long id);

    /**
     * 回复评价
     */
    void replyReview(Long replierId, Long id, String reply);

    /**
     * 评价统计（平均分、总数、各星数分布）
     */
    Map<String, Object> getReviewStatistics(Long activityId);

    /**
     * 获取我的评价历史
     */
    IPage<ActivityReview> getMyReviews(Long userId, Integer page, Integer size);

    /**
     * 用户删除自己的评价
     */
    void deleteMyReview(Long userId, Long reviewId);

    /**
     * 获取用户可评价的活动列表（已购票但未评价）
     */
    List<Map<String, Object>> getPendingReviews(Long userId);
}
