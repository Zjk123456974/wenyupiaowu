package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Category;
import com.ticket.system.entity.Reservation;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.CategoryMapper;
import com.ticket.system.mapper.ReservationMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.MessageService;
import com.ticket.system.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/activity")
@lombok.extern.slf4j.Slf4j
public class AdminActivityController {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private MessageService messageService;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm"};
        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @OperLog(module = "活动管理", operation = "新增活动")

    @PostMapping
    public Result<Activity> createActivity(@RequestBody Map<String, Object> params) {
        checkAdmin();
        
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String coverImage = params.get("coverImage") != null ? params.get("coverImage").toString() : null;
        Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId").toString()) : null;
        String venue = params.get("venue") != null ? params.get("venue").toString() : null;
        BigDecimal price = params.get("price") != null ? BigDecimal.valueOf(Double.parseDouble(params.get("price").toString())) : null;
        Integer totalTickets = params.get("totalTickets") != null ? Integer.valueOf(params.get("totalTickets").toString()) : null;
        String startTime = params.get("startTime") != null ? params.get("startTime").toString() : null;
        String endTime = params.get("endTime") != null ? params.get("endTime").toString() : null;
        String grabStartTime = params.get("grabStartTime") != null ? params.get("grabStartTime").toString() : null;
        String grabEndTime = params.get("grabEndTime") != null ? params.get("grabEndTime").toString() : null;
        String description = params.get("description") != null ? params.get("description").toString() : null;
        
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("活动标题不能为空");
        }
        if (venue == null || venue.trim().isEmpty()) {
            throw new BusinessException("场馆不能为空");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("票价必须大于0");
        }
        if (totalTickets == null || totalTickets <= 0) {
            throw new BusinessException("总票数必须大于0");
        }
        
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setCoverImage(coverImage);
        activity.setCategoryId(categoryId);
        activity.setVenue(venue);
        activity.setPrice(price);
        activity.setTotalTickets(totalTickets);
        activity.setSoldTickets(0);
        activity.setPurchaseLimit(1);
        
        activity.setStartTime(parseDateTime(startTime));
        activity.setEndTime(parseDateTime(endTime));
        activity.setGrabStartTime(parseDateTime(grabStartTime));
        activity.setGrabEndTime(parseDateTime(grabEndTime));
        
        activity.setDescription(description);
        activity.setStatus(1);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        
        activityMapper.insert(activity);
        
        return Result.success(activity);
    }

    private static final String DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&h=400&fit=crop";

    @GetMapping("/page")
    public Result<IPage<Activity>> getActivityPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        checkAdmin();
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Activity::getTitle, keyword).or().like(Activity::getVenue, keyword));
        }
        wrapper.orderByDesc(Activity::getCreateTime);
        IPage<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        for (Activity activity : result.getRecords()) {
            if (activity.getCoverImage() == null || activity.getCoverImage().trim().isEmpty()) {
                activity.setCoverImage(DEFAULT_COVER_IMAGE);
            }
            if (activity.getCategoryId() != null) {
                Category category = categoryMapper.selectById(activity.getCategoryId());
                if (category != null) {
                    activity.setCategoryName(category.getName());
                }
            }
            if (activity.getOperatorId() != null) {
                User operator = userMapper.selectById(activity.getOperatorId());
                if (operator != null) {
                    activity.setOperatorName(operator.getNickname() != null ? operator.getNickname() : operator.getUsername());
                }
            }
            if (activity.getTotalTickets() != null && activity.getSoldTickets() != null) {
                activity.setRemainTickets(activity.getTotalTickets() - activity.getSoldTickets());
            }
        }
        return Result.success(result);
    }

    @OperLog(module = "活动管理", operation = "审核活动")

    @PutMapping("/{id}/audit")
    public Result<Void> auditActivity(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        boolean approved = params.get("approved") != null ? Boolean.parseBoolean(params.get("approved").toString()) : false;
        Integer auditStatus = approved ? 1 : 2;
        activity.setStatus(auditStatus);
        if (auditStatus == 2) {
            String rejectReason = params.get("reason") != null ? params.get("reason").toString()
                    : (params.get("rejectReason") != null ? params.get("rejectReason").toString() : null);
            activity.setRejectReason(rejectReason);
        } else {
            activity.setRejectReason(null);
        }
        activityMapper.updateById(activity);
        return Result.success();
    }

    @OperLog(module = "活动管理", operation = "强制下线活动")

    @PutMapping("/{id}/force-offline")
    public Result<Void> forceOffline(@PathVariable Long id) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        activity.setStatus(4);
        activityMapper.updateById(activity);
        return Result.success();
    }

    @OperLog(module = "活动管理", operation = "发布活动")

    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getStatus() != 1 && activity.getStatus() != 4) {
            throw new BusinessException("仅审核通过或已下架的活动可上架");
        }
        activity.setStatus(3);
        activityMapper.updateById(activity);
        int remain = activity.getTotalTickets() - activity.getSoldTickets();
        stockService.initStock(id, remain);
        // 借鉴大麦「开票提醒」：上架后向曾预约该活动的用户推送开票通知
        notifyReservedUsersOnPublish(id);
        return Result.success();
    }

    @OperLog(module = "活动管理", operation = "编辑活动")

    @PutMapping("/{id}")
    public Result<Activity> updateActivity(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 基本信息
        String title = params.get("title") != null ? params.get("title").toString() : null;
        String coverImage = params.get("coverImage") != null ? params.get("coverImage").toString() : null;
        Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId").toString()) : null;
        String venue = params.get("venue") != null ? params.get("venue").toString() : null;
        BigDecimal price = params.get("price") != null ? BigDecimal.valueOf(Double.parseDouble(params.get("price").toString())) : null;
        String description = params.get("description") != null ? params.get("description").toString() : null;
        String ticketRule = params.get("ticketRule") != null ? params.get("ticketRule").toString() : null;
        Integer purchaseLimit = params.get("purchaseLimit") != null ? Integer.valueOf(params.get("purchaseLimit").toString()) : null;
        Integer totalTickets = params.get("totalTickets") != null ? Integer.valueOf(params.get("totalTickets").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;
        LocalDateTime startTime = parseDateTime(params.get("startTime") != null ? params.get("startTime").toString() : null);
        LocalDateTime endTime = parseDateTime(params.get("endTime") != null ? params.get("endTime").toString() : null);
        LocalDateTime grabStartTime = parseDateTime(params.get("grabStartTime") != null ? params.get("grabStartTime").toString() : null);
        LocalDateTime grabEndTime = parseDateTime(params.get("grabEndTime") != null ? params.get("grabEndTime").toString() : null);
        // 3D 场馆视野：开关与场馆模型（运营/管理员均可配置）
        Integer view3dEnabled = params.get("view3dEnabled") != null ? Integer.valueOf(params.get("view3dEnabled").toString()) : null;
        String view3dModel = params.get("view3dModel") != null ? params.get("view3dModel").toString() : null;

        // 校验：修改后总票数不能小于已售票数
        if (totalTickets != null) {
            int sold = activity.getSoldTickets() != null ? activity.getSoldTickets() : 0;
            if (totalTickets < sold) {
                throw new BusinessException("总票数不能小于已售票数(" + sold + ")");
            }
        }

        if (title != null && !title.trim().isEmpty()) {
            activity.setTitle(title);
        }
        if (venue != null && !venue.trim().isEmpty()) {
            activity.setVenue(venue);
        }
        if (categoryId != null) {
            activity.setCategoryId(categoryId);
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            activity.setPrice(price);
        }
        // 封面/介绍/票务规则允许清空
        if (params.containsKey("coverImage")) {
            activity.setCoverImage(coverImage);
        }
        if (params.containsKey("description")) {
            activity.setDescription(description);
        }
        if (params.containsKey("ticketRule")) {
            activity.setTicketRule(ticketRule);
        }
        if (purchaseLimit != null && purchaseLimit >= 1) {
            activity.setPurchaseLimit(purchaseLimit);
        }
        if (totalTickets != null) {
            activity.setTotalTickets(totalTickets);
        }
        if (startTime != null) {
            activity.setStartTime(startTime);
        }
        if (endTime != null) {
            activity.setEndTime(endTime);
        }
        if (grabStartTime != null) {
            activity.setGrabStartTime(grabStartTime);
        }
        if (grabEndTime != null) {
            activity.setGrabEndTime(grabEndTime);
        }
        // 3D 视野预览开关与场馆模型：仅当请求显式携带时更新（关闭时传 0 也会生效）
        if (params.containsKey("view3dEnabled")) {
            activity.setView3dEnabled(view3dEnabled);
        }
        if (params.containsKey("view3dModel")) {
            activity.setView3dModel(view3dModel != null && !view3dModel.trim().isEmpty() ? view3dModel.trim() : "arena_bowl");
        }
        if (status != null) {
            activity.setStatus(status);
            if (status == 3) {
                int remain = activity.getTotalTickets() - activity.getSoldTickets();
                stockService.initStock(id, remain);
            }
        }

        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);

        // 总票数变更时同步 Redis 剩余库存（仅当 Redis 中已存在该活动库存记录）
        if (totalTickets != null && stockService.getStock(id) != null) {
            int remain = activity.getTotalTickets() - (activity.getSoldTickets() != null ? activity.getSoldTickets() : 0);
            stockService.initStock(id, remain);
        }

        return Result.success(activity);
    }

    /**
     * 活动上架后，向曾预约该活动的用户推送「开票提醒」（借鉴大麦）。
     * 预约即“想看/待开抢”，正式开票后通知用户及时购票，提升转化。
     */
    private void notifyReservedUsersOnPublish(Long activityId) {
        try {
            Activity act = activityMapper.selectById(activityId);
            if (act == null) {
                return;
            }
            List<Reservation> reservations = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getActivityId, activityId)
                            .ne(Reservation::getStatus, 2));
            Set<Long> userIds = new HashSet<>();
            for (Reservation r : reservations) {
                if (r.getUserId() != null) {
                    userIds.add(r.getUserId());
                }
            }
            for (Long uid : userIds) {
                messageService.sendMessage(uid, "开票提醒",
                        "您预约的活动【" + act.getTitle() + "】已正式开票，快来购票吧！", 3);
            }
            if (!userIds.isEmpty()) {
                log.info("活动{}上架，已向{}位预约用户推送开票提醒", activityId, userIds.size());
            }
        } catch (Exception e) {
            log.warn("开票提醒推送失败（不影响上架）：{}", e.getMessage());
        }
    }

    @OperLog(module = "活动管理", operation = "删除活动")

    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getStatus() == 3) {
            throw new BusinessException("已上架活动不能删除，请先下架");
        }
        activityMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/{id}/stock")
    public Result<Map<String, Object>> getStockInfo(@PathVariable Long id) {
        checkAdmin();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        Integer redisStock = stockService.getStock(id);

        Map<String, Object> stockInfo = new HashMap<>();
        stockInfo.put("totalTickets", activity.getTotalTickets());
        stockInfo.put("soldTickets", activity.getSoldTickets());
        stockInfo.put("remainTickets", activity.getTotalTickets() != null && activity.getSoldTickets() != null
                ? activity.getTotalTickets() - activity.getSoldTickets() : 0);
        stockInfo.put("redisStock", redisStock);
        return Result.success(stockInfo);
    }
}