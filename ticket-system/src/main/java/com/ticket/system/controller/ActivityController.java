package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ActivityNotify;
import com.ticket.system.entity.ActivityWish;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticket.system.entity.ActivityLineup;
import com.ticket.system.entity.Favorite;
import com.ticket.system.entity.Orders;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityNotifyMapper;
import com.ticket.system.mapper.ActivityLineupMapper;
import com.ticket.system.mapper.ActivityWishMapper;
import com.ticket.system.mapper.FavoriteMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Resource
    private RequestContext requestContext;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private ActivityWishMapper activityWishMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private ActivityNotifyMapper activityNotifyMapper;

    @Resource
    private ActivityLineupMapper activityLineupMapper;

    /**
     * 分页查询活动列表（无需登录）
     */
    @GetMapping("/list")
    public Result<IPage<Activity>> getActivityPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long tagId) {
        IPage<Activity> pageResult = activityService.getActivityPage(
                page, size, categoryId, keyword, startTime, endTime,
                minPrice, maxPrice, sortField, sortOrder, tagId);
        return Result.success(pageResult);
    }

    /**
     * 活动详情（无需登录）
     */
    @GetMapping("/detail/{id}")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);
        return Result.success(activity);
    }

    /**
     * 热门推荐（无需登录）
     */
    @GetMapping("/hot")
    public Result<Object> getHotActivities() {
        Object hotList = activityService.getHotActivities();
        return Result.success(hotList);
    }

    /**
     * 想看 / 取消想看（轻量社交计数，对标大麦“X人想看）；需登录
     */
    @PostMapping("/{id}/wish")
    @Transactional
    public Result<Map<String, Object>> toggleWish(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录后再想看");
        }
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        LambdaQueryWrapper<ActivityWish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityWish::getUserId, userId).eq(ActivityWish::getActivityId, id);
        ActivityWish existing = activityWishMapper.selectOne(wrapper);

        boolean wished;
        if (existing != null) {
            // 已想看 → 取消：先删关系行，再原子减计数（GREATEST 兜底非负，杜绝并发丢失更新）
            activityWishMapper.deleteById(existing.getId());
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, id)
                    .setSql("wish_count = GREATEST(0, wish_count - 1)"));
            wished = false;
        } else {
            // 未想看 → 想看：先插关系行，再原子加计数
            ActivityWish wish = new ActivityWish();
            wish.setUserId(userId);
            wish.setActivityId(id);
            activityWishMapper.insert(wish);
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, id)
                    .setSql("wish_count = wish_count + 1"));
            wished = true;
        }
        // 重新读取最终计数返回（避免用缓存值导致前端计数失真）
        int finalCount = activityMapper.selectById(id).getWishCount();
        Map<String, Object> data = new HashMap<>();
        data.put("wished", wished);
        data.put("wishCount", finalCount);
        return Result.success(wished ? "已标记为想看" : "已取消想看", data);
    }

    /**
     * 查询当前用户对该活动是否已想看；未登录视为未想看
     */
    @GetMapping("/{id}/wish/me")
    public Result<Map<String, Object>> myWish(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        boolean wished = false;
        if (userId != null) {
            LambdaQueryWrapper<ActivityWish> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ActivityWish::getUserId, userId).eq(ActivityWish::getActivityId, id);
            wished = activityWishMapper.selectCount(wrapper) > 0;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("wished", wished);
        return Result.success(data);
    }

    /**
     * 到货提醒开关（独立于「收藏」，避免污染收藏夹）。
     * 售罄/无票时登记，补票或重新开票时通知用户；再次点击取消登记。
     */
    @PostMapping("/{id}/notify")
    @Transactional
    public Result<Map<String, Object>> toggleNotify(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<ActivityNotify> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityNotify::getUserId, userId).eq(ActivityNotify::getActivityId, id);
        ActivityNotify existing = activityNotifyMapper.selectOne(wrapper);
        boolean notified;
        if (existing != null) {
            activityNotifyMapper.deleteById(existing.getId());
            notified = false;
        } else {
            ActivityNotify notify = new ActivityNotify();
            notify.setUserId(userId);
            notify.setActivityId(id);
            activityNotifyMapper.insert(notify);
            notified = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("notified", notified);
        return Result.success(notified ? "已开启到货提醒" : "已取消到货提醒", data);
    }

    /**
     * 查询当前用户对该活动是否已开启到货提醒；未登录视为未开启
     */
    @GetMapping("/{id}/notify/me")
    public Result<Map<String, Object>> myNotify(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        boolean notified = false;
        if (userId != null) {
            LambdaQueryWrapper<ActivityNotify> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ActivityNotify::getUserId, userId).eq(ActivityNotify::getActivityId, id);
            notified = activityNotifyMapper.selectCount(wrapper) > 0;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("notified", notified);
        return Result.success(data);
    }

    /**
     * 个性化推荐：基于当前用户「想看 / 收藏 / 已购」活动所属的演出分类偏好，
     * 优先推荐同类在售且用户尚未交互的活动；未登录或没有任何行为时，降级为热门兜底。
     * 对标大麦「猜你喜欢」，体现基于用户行为的千人千面推荐能力。
     */
    @GetMapping("/recommend")
    public Result<Object> recommend(@RequestParam(defaultValue = "8") Integer size) {
        if (size == null || size <= 0 || size > 20) size = 8;
        Long userId = requestContext.getCurrentUserId();
        // 偏好分类权重：已购 > 想看 > 收藏（已购代表最强消费意图，收藏最弱）
        Map<Long, Integer> catWeight = new LinkedHashMap<>();
        List<Long> excludeIds = new ArrayList<>();
        List<Activity> result = new ArrayList<>();

        if (userId != null) {
            // 收集偏好分类 + 已交互活动（作为排除项，避免重复推荐已看过的）
            List<ActivityWish> wishes = activityWishMapper.selectList(
                    new LambdaQueryWrapper<ActivityWish>().eq(ActivityWish::getUserId, userId));
            List<Favorite> favs = favoriteMapper.selectList(
                    new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
            List<Orders> myOrders = ordersMapper.selectList(
                    new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId)
                            .in(Orders::getStatus, 1, 2, 5));
            // 已购权重 3、想看权重 2、收藏权重 1
            for (Orders o : myOrders) addSignal(catWeight, excludeIds, o.getActivityId(), 3);
            for (ActivityWish w : wishes) addSignal(catWeight, excludeIds, w.getActivityId(), 2);
            for (Favorite f : favs) addSignal(catWeight, excludeIds, f.getActivityId(), 1);
        }

        // 1) 按权重降序的分类，逐个优先推荐同类在售活动（高意图分类先填满）
        if (!catWeight.isEmpty()) {
            List<Long> rankedCats = catWeight.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            for (Long cat : rankedCats) {
                if (result.size() >= size) break;
                LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getStatus, 3)
                        .eq(Activity::getCategoryId, cat)
                        .orderByAsc(Activity::getGrabStartTime);
                if (!excludeIds.isEmpty()) q.notIn(Activity::getId, excludeIds);
                q.last("LIMIT " + (size - result.size()));
                result.addAll(activityMapper.selectList(q));
            }
        }
        // 2) 数量不足时用热门活动补足（按浏览量 + 真实已售）
        if (result.size() < size) {
            List<Long> taken = result.stream().map(Activity::getId).collect(Collectors.toList());
            LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<Activity>()
                    .eq(Activity::getStatus, 3)
                    .orderByDesc(Activity::getViewCount)
                    .orderByDesc(Activity::getSoldTickets);
            if (!taken.isEmpty()) q.notIn(Activity::getId, taken);
            q.last("LIMIT " + (size - result.size()));
            result.addAll(activityMapper.selectList(q));
        }
        // 填充展示用的剩余票数（total - sold），与列表接口保持一致
        for (Activity a : result) {
            int total = a.getTotalTickets() != null ? a.getTotalTickets() : 0;
            int sold = a.getSoldTickets() != null ? a.getSoldTickets() : 0;
            a.setRemainTickets(Math.max(0, total - sold));
        }
        return Result.success(result);
    }

    /**
     * 查询某活动的演出阵容 / 嘉宾列表（公开，无需登录），按 sortOrder 升序。
     * 用于详情页「演出阵容」模块展示，运营在后台配置，避免空壳。
     */
    @GetMapping("/{id}/lineup")
    public Result<List<ActivityLineup>> getActivityLineup(@PathVariable Long id) {
        LambdaQueryWrapper<ActivityLineup> wrapper = new LambdaQueryWrapper<ActivityLineup>()
                .eq(ActivityLineup::getActivityId, id)
                .orderByAsc(ActivityLineup::getSortOrder)
                .orderByAsc(ActivityLineup::getId);
        return Result.success(activityLineupMapper.selectList(wrapper));
    }

    private void addSignal(Map<Long, Integer> catWeight, List<Long> excludeIds, Long activityId, int weight) {
        if (activityId == null) return;
        excludeIds.add(activityId);
        Activity a = activityMapper.selectById(activityId);
        if (a != null && a.getCategoryId() != null) {
            catWeight.merge(a.getCategoryId(), weight, Integer::sum);
        }
    }
}
