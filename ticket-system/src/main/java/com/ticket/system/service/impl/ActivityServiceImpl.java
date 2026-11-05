package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ActivityTagRelation;
import com.ticket.system.entity.Category;
import com.ticket.system.entity.Reservation;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityTagRelationMapper;
import com.ticket.system.mapper.CategoryMapper;
import com.ticket.system.mapper.ReservationMapper;
import com.ticket.system.service.ActivityService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ActivityTagRelationMapper activityTagRelationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private com.ticket.system.service.StockService stockService;

    @Autowired
    private ReservationMapper reservationMapper;

    private static final String HOT_ACTIVITY_KEY = "activity:hot";
    private static final long HOT_ACTIVITY_EXPIRE = 5;

    @Override
    public IPage<Activity> getActivityPage(Integer page, Integer size, Long categoryId, String keyword,
                                           String startTime, String endTime,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           String sortField, String sortOrder, Long tagId) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        // 只查询已上架的活动
        wrapper.eq(Activity::getStatus, 3);

        // 过滤已结束的场次：抢票已结束或已开演的活动不再出现在活动列表，
        // 避免用户点进详情才发现"抢票已结束"（与首页热门推荐的过滤条件保持一致）
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w.isNull(Activity::getGrabEndTime).or().ge(Activity::getGrabEndTime, now));
        wrapper.and(w -> w.isNull(Activity::getStartTime).or().ge(Activity::getStartTime, now));

        // 分类筛选
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Activity::getCategoryId, categoryId);
        }

        // 关键词模糊搜索：同时匹配活动名称 / 场馆 / 分类名称
        // （用户习惯输入"体育赛事""演唱会"这类分类词，只搜标题会误以为"没有活动"）
        if (StringUtils.isNotBlank(keyword)) {
            List<Long> matchedCategoryIds = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>().like(Category::getName, keyword))
                    .stream().map(Category::getId).collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(Activity::getTitle, keyword).or().like(Activity::getVenue, keyword);
                if (!matchedCategoryIds.isEmpty()) {
                    w.or().in(Activity::getCategoryId, matchedCategoryIds);
                }
            });
        }

        // 时间区间筛选（活动时间与筛选区间有交集）
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.le(Activity::getStartTime, endTime != null ? endTime : "9999-12-31T23:59:59");
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.ge(Activity::getEndTime, startTime != null ? startTime : "0000-01-01T00:00:00");
        }

        // 价格区间筛选
        if (minPrice != null) {
            wrapper.ge(Activity::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Activity::getPrice, maxPrice);
        }

        // 标签筛选：查询该标签关联的活动ID列表
        if (tagId != null && tagId > 0) {
            LambdaQueryWrapper<ActivityTagRelation> tagWrapper = new LambdaQueryWrapper<ActivityTagRelation>()
                    .eq(ActivityTagRelation::getTagId, tagId);
            List<ActivityTagRelation> relations = activityTagRelationMapper.selectList(tagWrapper);
            List<Long> activityIds = relations.stream()
                    .map(ActivityTagRelation::getActivityId)
                    .collect(Collectors.toList());
            if (activityIds.isEmpty()) {
                // 没有匹配的活动，返回空结果
                wrapper.eq(Activity::getId, -1L);
            } else {
                wrapper.in(Activity::getId, activityIds);
            }
        }

        // 排序
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = !"desc".equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime":
                    wrapper.orderBy(true, isAsc, Activity::getCreateTime);
                    break;
                case "hot":
                    wrapper.orderByDesc(Activity::getViewCount);
                    wrapper.orderByDesc(Activity::getSoldTickets);
                    break;
                case "grabStartTime":
                    wrapper.orderBy(true, isAsc, Activity::getGrabStartTime);
                    break;
                case "price":
                    wrapper.orderBy(true, isAsc, Activity::getPrice);
                    break;
                default:
                    wrapper.orderByDesc(Activity::getCreateTime);
                    break;
            }
        } else {
            wrapper.orderByDesc(Activity::getCreateTime);
        }

        IPage<Activity> result = activityMapper.selectPage(pageParam, wrapper);

        // 填充非数据库字段
        for (Activity activity : result.getRecords()) {
            fillNonDbFields(activity);
        }

        return result;
    }

    @Override
    public Activity getActivityDetail(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 浏览量+1：使用 SQL 原子自增，避免“整行更新”在并发下覆盖他人刚提交的 sold_tickets/view_count 等字段
        activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, id)
                .setSql("view_count = view_count + 1"));

        // 填充非数据库字段
        fillNonDbFields(activity);

        return activity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getHotActivities() {
        // 先从Redis缓存获取
        Object cached = redisTemplate.opsForValue().get(HOT_ACTIVITY_KEY);
        if (cached != null) {
            return cached;
        }

        // 查询数据库：按soldTickets+viewCount排序取前10
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 3);
        // 已结束/已开演的活动不再进入热门推荐，避免把已经演完的场次挂在首页推荐
        wrapper.and(w -> w.isNull(Activity::getGrabEndTime).or().ge(Activity::getGrabEndTime, now));
        wrapper.and(w -> w.isNull(Activity::getStartTime).or().ge(Activity::getStartTime, now));
        wrapper.orderByDesc(Activity::getSoldTickets);
        wrapper.orderByDesc(Activity::getViewCount);
        wrapper.last("LIMIT 10");

        List<Activity> hotList = activityMapper.selectList(wrapper);

        // 填充非数据库字段
        for (Activity activity : hotList) {
            fillNonDbFields(activity);
        }

        // 写入Redis缓存，5分钟过期
        redisTemplate.opsForValue().set(HOT_ACTIVITY_KEY, hotList, HOT_ACTIVITY_EXPIRE, TimeUnit.MINUTES);

        return hotList;
    }

    @Override
    public String getActivityGrabStatus(Activity activity) {
        if (activity == null || activity.getGrabStartTime() == null || activity.getGrabEndTime() == null) {
            return "NOT_STARTED";
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getGrabStartTime())) {
            return "NOT_STARTED";
        } else if (!now.isAfter(activity.getGrabEndTime())) {
            return "GRABBING";
        } else {
            return "ENDED";
        }
    }

    private static final String DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=400&fit=crop";

    /**
     * 填充非数据库字段：categoryName、remainTickets、grabStatus、默认封面图
     */
    private void fillNonDbFields(Activity activity) {
        // 默认封面图
        if (StringUtils.isBlank(activity.getCoverImage())) {
            activity.setCoverImage(DEFAULT_COVER_IMAGE);
        }
        // 分类名称
        if (activity.getCategoryId() != null) {
            Category category = categoryMapper.selectById(activity.getCategoryId());
            if (category != null) {
                activity.setCategoryName(category.getName());
            }
        }
        // 剩余票数：优先读取 Redis 实时库存（与抢票扣减同源，保证所有人同一时刻看到一致票数）；
        // Redis 无缓存时回退 DB 差值，并将 Redis 库存补齐，保证后续扣减可靠
        Integer remain = null;
        if (activity.getId() != null) {
            remain = stockService.getStock(activity.getId());
            if (remain == null && activity.getTotalTickets() != null && activity.getSoldTickets() != null) {
                remain = activity.getTotalTickets() - activity.getSoldTickets();
                stockService.initStock(activity.getId(), remain);
            }
        }
        if (remain != null) {
            activity.setRemainTickets(remain);
        } else if (activity.getTotalTickets() != null && activity.getSoldTickets() != null) {
            activity.setRemainTickets(activity.getTotalTickets() - activity.getSoldTickets());
        }
        // 抢票状态
        activity.setGrabStatus(getActivityGrabStatus(activity));
        // 已预约想看人数：基于活动热度用公式生成大数字，制造抢票紧张感（类似大麦"X万人想看"）
        int baseCount = getReservationBaseCount(activity);
        Long actualCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getActivityId, activity.getId())
                        .ne(Reservation::getStatus, 2));
        activity.setReservationCount(baseCount + (actualCount != null ? actualCount.intValue() : 0));
    }

    /**
     * 根据活动分类和ID生成基础"想看"人数（模拟真实大麦数据量级）
     * 演唱会: 8000-15000, 话剧: 3000-6000, 艺术展: 2000-5000
     * 体育电竞: 5000-10000, 校园文娱: 1000-3000
     */
    private int getReservationBaseCount(Activity activity) {
        long id = activity.getId();
        int base;
        if (activity.getCategoryId() != null) {
            switch (activity.getCategoryId().intValue()) {
                case 1: // 演唱会
                    base = 8000 + (int) (id * 713) % 7000;
                    break;
                case 2: // 话剧演出
                    base = 3000 + (int) (id * 431) % 3000;
                    break;
                case 3: // 艺术展览
                    base = 2000 + (int) (id * 297) % 3000;
                    break;
                case 4: // 体育赛事
                    base = 5000 + (int) (id * 533) % 5000;
                    break;
                case 5: // 校园文娱
                    base = 1000 + (int) (id * 191) % 2000;
                    break;
                default:
                    base = 2000 + (int) (id * 257) % 4000;
                    break;
            }
        } else {
            base = 2000 + (int) (id * 257) % 4000;
        }
        return base;
    }

    /**
     * 活动滚动排期：把在架(status=3)与已下架(status=4)的活动，按 id 升序交错重排到
     * 「今天+1天」起的未来日历，统一置为在架(status=3)。
     * - 单日演出/赛事：start 19:30、end 22:30；艺术展览(category=3)：展期45天。
     * - 抢票窗口：start 前30天10:00 ~ start 当天23:59（保证 grabEnd 恒大于 now，不会被自动下架任务误杀）。
     * - 最早一场为「明天」，既保证列表中始终有未来活动，又规避了「当天深夜被误下架」的边界。
     */
    @Override
    public void rescheduleRolling() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Activity::getStatus, 3, 4);
            // 仅对「真正过期」的活动做滚动重排：销售窗口已关闭(grab_end<now) 或 演出已结束(end<now)。
            // 仍在有效期内的在售/未来活动保留运营设置的原始排期，避免被任意登录/定时任务覆盖（修复 P0-1）。
            wrapper.and(w -> w.isNotNull(Activity::getGrabEndTime).lt(Activity::getGrabEndTime, now)
                             .or().isNotNull(Activity::getEndTime).lt(Activity::getEndTime, now));
            wrapper.orderByAsc(Activity::getId);
            List<Activity> acts = activityMapper.selectList(wrapper);
            if (acts == null || acts.isEmpty()) {
                return;
            }
            int stepDays = 5;
            int i = 0;
            for (Activity a : acts) {
                LocalDate base = today.plusDays(1L + (long) i * stepDays);
                LocalDateTime start;
                LocalDateTime end;
                if (a.getCategoryId() != null && a.getCategoryId() == 3) {
                    // 艺术展览：长展期
                    start = base.atTime(10, 0);
                    end = base.plusDays(45).atTime(18, 0);
                } else {
                    // 单日演出 / 赛事
                    start = base.atTime(19, 30);
                    end = base.atTime(22, 30);
                }
                LocalDateTime grabStart = base.minusDays(30).atTime(10, 0);
                LocalDateTime grabEnd = base.atTime(23, 59);
                Activity update = new Activity();
                update.setId(a.getId());
                update.setStartTime(start);
                update.setEndTime(end);
                update.setGrabStartTime(grabStart);
                update.setGrabEndTime(grabEnd);
                update.setStatus(3);
                update.setUpdateTime(now);
                activityMapper.updateById(update);
                i++;
            }
            log.info("活动滚动排期完成，重新排布 {} 个在架/已下架活动", acts.size());
        } catch (Exception e) {
            log.error("活动滚动排期失败", e);
        }
    }
}
