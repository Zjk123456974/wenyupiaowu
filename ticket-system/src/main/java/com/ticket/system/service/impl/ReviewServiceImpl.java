package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ActivityReview;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityReviewMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动评价服务实现
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private ActivityReviewMapper activityReviewMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public ActivityReview addReview(Long userId, Long activityId, Integer rating, String content, String images) {
        // 校验活动存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 校验评分
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须为1-5之间");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("评价内容不能为空");
        }

        // 口径统一：仅"已购票（存在已支付订单）"的用户可评价，与前端 hasPurchased 门槛一致，
        // 防止绕过前端直接调接口的越权评价；已退款订单(status=5)不可再评价，
        // 已有评价不受后续退款影响（与主流平台一致）。管理端仍可隐藏/驳回违规内容。

        // 校验购买记录：必须存在该活动的已支付订单（status=1）才可评价
        Long paidCount = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .eq(Orders::getActivityId, activityId)
                        .eq(Orders::getStatus, 1));
        if (paidCount == null || paidCount == 0) {
            throw new BusinessException("仅购买过该活动的用户可以评价");
        }

        // 校验是否已评价过（同一活动同一用户仅允许评价一次）
        Long existCount = activityReviewMapper.selectCount(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, activityId)
                        .eq(ActivityReview::getUserId, userId));
        if (existCount > 0) {
            throw new BusinessException("您已评价过该活动，不能重复评价");
        }

        // 填充用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setUserId(userId);
        review.setUsername(user.getUsername());
        review.setNickname(user.getNickname());
        review.setAvatar(user.getAvatar());
        review.setRating(rating);
        review.setContent(content);
        review.setImages(images);
        review.setStatus(1); // 已通过，即时展示（管理端可隐藏/驳回）
        activityReviewMapper.insert(review);

        // 填充非数据库字段
        review.setActivityTitle(activity.getTitle());

        log.info("用户{}发布评价，活动{}，评分{}", userId, activityId, rating);
        return review;
    }

    @Override
    public IPage<ActivityReview> getReviewPage(Integer page, Integer size, Long activityId, Integer status) {
        Page<ActivityReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<>();
        if (activityId != null) {
            wrapper.eq(ActivityReview::getActivityId, activityId);
        }
        if (status != null) {
            wrapper.eq(ActivityReview::getStatus, status);
        }
        wrapper.orderByDesc(ActivityReview::getCreateTime);
        IPage<ActivityReview> result = activityReviewMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillReviewInfo);
        return result;
    }

    @Override
    public List<ActivityReview> getPublicReviews(Long activityId) {
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityReview::getActivityId, activityId)
               .eq(ActivityReview::getStatus, 1)
               .orderByDesc(ActivityReview::getCreateTime);
        List<ActivityReview> list = activityReviewMapper.selectList(wrapper);
        list.forEach(this::fillReviewInfo);
        return list;
    }

    @Override
    @Transactional
    public void approveReview(Long id) {
        ActivityReview review = activityReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        // 允许"待审核(0)→通过"和"已隐藏(2)→恢复展示"两种流转
        if (review.getStatus() != 0 && review.getStatus() != 2) {
            throw new BusinessException("仅待审核或已隐藏的评价可通过/恢复");
        }
        review.setStatus(1);
        activityReviewMapper.updateById(review);
        log.info("评价{}审核通过/恢复展示", id);
    }

    @Override
    @Transactional
    public void rejectReview(Long id) {
        ActivityReview review = activityReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (review.getStatus() != 0) {
            throw new BusinessException("仅待审核评价可驳回");
        }
        review.setStatus(3);
        activityReviewMapper.updateById(review);
        log.info("评价{}审核驳回", id);
    }

    @Override
    @Transactional
    public void hideReview(Long id) {
        ActivityReview review = activityReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setStatus(2);
        activityReviewMapper.updateById(review);
        log.info("评价{}已隐藏", id);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        ActivityReview review = activityReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        activityReviewMapper.deleteById(id);
        log.info("评价{}已删除", id);
    }

    @Override
    @Transactional
    public void replyReview(Long replierId, Long id, String reply) {
        ActivityReview review = activityReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!StringUtils.hasText(reply)) {
            throw new BusinessException("回复内容不能为空");
        }
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        review.setReplierId(replierId);
        activityReviewMapper.updateById(review);
        log.info("评价{}已回复，回复人{}", id, replierId);
    }

    @Override
    public Map<String, Object> getReviewStatistics(Long activityId) {
        // 获取该活动所有已通过的评价
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityReview::getActivityId, activityId)
               .eq(ActivityReview::getStatus, 1);
        List<ActivityReview> reviews = activityReviewMapper.selectList(wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        int total = reviews.size();
        result.put("totalCount", total);

        if (total == 0) {
            result.put("avgRating", BigDecimal.ZERO);
            result.put("rating5Count", 0);
            result.put("rating4Count", 0);
            result.put("rating3Count", 0);
            result.put("rating2Count", 0);
            result.put("rating1Count", 0);
            result.put("ratingDistribution", buildEmptyDistribution());
            return result;
        }

        // 计算平均评分
        int sum = 0;
        int[] distribution = new int[6]; // index 1-5
        for (ActivityReview r : reviews) {
            int rate = r.getRating() == null ? 0 : r.getRating();
            sum += rate;
            if (rate >= 1 && rate <= 5) {
                distribution[rate]++;
            }
        }
        BigDecimal avgRating = new BigDecimal(sum).divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);

        result.put("avgRating", avgRating);
        result.put("rating5Count", distribution[5]);
        result.put("rating4Count", distribution[4]);
        result.put("rating3Count", distribution[3]);
        result.put("rating2Count", distribution[2]);
        result.put("rating1Count", distribution[1]);

        // 各星数分布（带百分比）
        List<Map<String, Object>> distributionList = new java.util.ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            Map<String, Object> item = new HashMap<>();
            item.put("star", star);
            item.put("count", distribution[star]);
            BigDecimal percent = new BigDecimal(distribution[star])
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
            item.put("percent", percent);
            distributionList.add(item);
        }
        result.put("ratingDistribution", distributionList);

        return result;
    }

    // ==================== 私有方法 ====================

    @Override
    public IPage<ActivityReview> getMyReviews(Long userId, Integer page, Integer size) {
        Page<ActivityReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ActivityReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityReview::getUserId, userId)
               .orderByDesc(ActivityReview::getCreateTime);
        IPage<ActivityReview> result = activityReviewMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillReviewInfo);
        return result;
    }

    @Override
    @Transactional
    public void deleteMyReview(Long userId, Long reviewId) {
        ActivityReview review = activityReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评价");
        }
        activityReviewMapper.deleteById(reviewId);
        log.info("用户{}删除评价{}", userId, reviewId);
    }

    @Override
    public List<Map<String, Object>> getPendingReviews(Long userId) {
        // 查询用户已支付订单关联的活动（去重）
        List<Orders> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .eq(Orders::getStatus, 1)
                        .orderByDesc(Orders::getCreateTime));

        // 查询用户已评价的活动ID集合
        List<ActivityReview> myReviews = activityReviewMapper.selectList(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getUserId, userId));
        java.util.Set<Long> reviewedActivityIds = new java.util.HashSet<>();
        for (ActivityReview r : myReviews) {
            reviewedActivityIds.add(r.getActivityId());
        }

        // 构建可评价活动列表（去重 + 排除已评价）
        Map<Long, Map<String, Object>> activityMap = new LinkedHashMap<>();
        for (Orders order : orders) {
            Long activityId = order.getActivityId();
            if (reviewedActivityIds.contains(activityId) || activityMap.containsKey(activityId)) {
                continue;
            }
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("activityId", activityId);
                item.put("activityTitle", activity.getTitle());
                item.put("venue", activity.getVenue());
                item.put("coverImage", activity.getCoverImage());
                item.put("orderNo", order.getOrderNo());
                activityMap.put(activityId, item);
            }
        }

        return new java.util.ArrayList<>(activityMap.values());
    }

    // ==================== 私有方法（原有） ====================

    /**
     * 填充评价非数据库字段
     */
    private void fillReviewInfo(ActivityReview review) {
        if (review.getActivityId() != null) {
            Activity act = activityMapper.selectById(review.getActivityId());
            if (act != null) {
                review.setActivityTitle(act.getTitle());
            }
        }
    }

    /**
     * 构建空分布
     */
    private List<Map<String, Object>> buildEmptyDistribution() {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            Map<String, Object> item = new HashMap<>();
            item.put("star", star);
            item.put("count", 0);
            item.put("percent", BigDecimal.ZERO);
            list.add(item);
        }
        return list;
    }
}
