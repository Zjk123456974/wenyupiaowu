package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ActivityTag;
import com.ticket.system.entity.ActivityLineup;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityLineupMapper;
import com.ticket.system.service.StockService;
import com.ticket.system.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/operator/activity")
public class OperatorActivityController {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private com.ticket.system.mapper.CategoryMapper categoryMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private com.ticket.system.mapper.TicketPriceMapper ticketPriceMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private ActivityLineupMapper activityLineupMapper;

    private void checkOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无运营权限");
        }
    }

    /**
     * 归属校验：运营只能操作自己创建的活动，管理员不受限
     */
    private void checkOwnership(Activity activity) {
        Integer role = requestContext.getCurrentRole();
        Long userId = requestContext.getCurrentUserId();
        if (role != null && role == 2) {
            return;
        }
        if (activity.getOperatorId() == null || !activity.getOperatorId().equals(userId)) {
            throw new BusinessException(403, "只能操作自己创建的活动");
        }
    }

    /**
     * 新增活动（运营提交，默认 status=-1 草稿，提交审核后进入待审核）
     */
    @PostMapping
    public Result<Long> addActivity(@RequestBody Map<String, Object> params) {
        checkOperator();
        Long operatorId = requestContext.getCurrentUserId();

        Activity activity = new Activity();
        activity.setTitle(getStr(params, "title"));
        activity.setCategoryId(getLong(params, "categoryId"));
        activity.setVenue(getStr(params, "venue"));
        activity.setStartTime(getDateTime(params, "startTime"));
        activity.setEndTime(getDateTime(params, "endTime"));
        activity.setCoverImage(getStr(params, "coverImage"));
        activity.setDescription(getStr(params, "description"));
        activity.setTicketNotice(getStr(params, "ticketNotice"));
        activity.setTicketRule(getStr(params, "ticketRule"));
        activity.setPurchaseLimit(getInt(params, "purchaseLimit"));
        // 票务规则开关（不传则走默认值：可退/免实名/关闭场馆地图/开启预约）
        if (params.containsKey("refundPolicy")) activity.setRefundPolicy(getInt(params, "refundPolicy"));
        if (params.containsKey("realNameMode")) activity.setRealNameMode(getInt(params, "realNameMode"));
        if (params.containsKey("seatMapEnabled")) activity.setSeatMapEnabled(getInt(params, "seatMapEnabled"));
        if (params.containsKey("view3dEnabled")) activity.setView3dEnabled(getInt(params, "view3dEnabled"));
        if (params.containsKey("view3dModel")) activity.setView3dModel(getStr(params, "view3dModel"));
        if (params.containsKey("reservationEnabled")) activity.setReservationEnabled(getInt(params, "reservationEnabled"));
        // 座位平面图布局（仅 seatMapEnabled=1 时生效；前端编排为 JSON，后端原样存储）
        activity.setSeatLayout(getStr(params, "seatLayout"));
        activity.setTotalTickets(getInt(params, "totalTickets"));
        activity.setSoldTickets(0);
        activity.setPrice(getDecimal(params, "price"));
        activity.setGrabStartTime(getDateTime(params, "grabStartTime"));
        activity.setGrabEndTime(getDateTime(params, "grabEndTime"));
        // 新增即「草稿」(-1)，提交审核后才会进入待审核(0)，与大麦“先存草稿再提交”一致
        activity.setStatus(-1);
        activity.setViewCount(0);
        activity.setOperatorId(operatorId);

        activityMapper.insert(activity);

        // 初始化Redis库存
        if (activity.getTotalTickets() != null) {
            stockService.initStock(activity.getId(), activity.getTotalTickets());
        }

        // 同步活动标签（payload 携带 tagIds 时全量覆盖绑定）
        bindTagsIfPresent(params, activity.getId());

        return Result.success(activity.getId());
    }

    /**
     * 编辑活动（仅status=0或2可编辑）
     */
    @PutMapping("/{id}")
    public Result<Void> updateActivity(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        if (activity.getStatus() != -1 && activity.getStatus() != 0 && activity.getStatus() != 2) {
            throw new BusinessException("当前活动状态不可编辑");
        }

        if (params.containsKey("title")) activity.setTitle(getStr(params, "title"));
        if (params.containsKey("categoryId")) activity.setCategoryId(getLong(params, "categoryId"));
        if (params.containsKey("venue")) activity.setVenue(getStr(params, "venue"));
        if (params.containsKey("startTime")) activity.setStartTime(getDateTime(params, "startTime"));
        if (params.containsKey("endTime")) activity.setEndTime(getDateTime(params, "endTime"));
        if (params.containsKey("coverImage")) activity.setCoverImage(getStr(params, "coverImage"));
        if (params.containsKey("description")) activity.setDescription(getStr(params, "description"));
        if (params.containsKey("ticketNotice")) activity.setTicketNotice(getStr(params, "ticketNotice"));
        if (params.containsKey("ticketRule")) activity.setTicketRule(getStr(params, "ticketRule"));
        if (params.containsKey("purchaseLimit")) activity.setPurchaseLimit(getInt(params, "purchaseLimit"));
        if (params.containsKey("refundPolicy")) activity.setRefundPolicy(getInt(params, "refundPolicy"));
        if (params.containsKey("realNameMode")) activity.setRealNameMode(getInt(params, "realNameMode"));
        if (params.containsKey("seatMapEnabled")) activity.setSeatMapEnabled(getInt(params, "seatMapEnabled"));
        if (params.containsKey("view3dEnabled")) activity.setView3dEnabled(getInt(params, "view3dEnabled"));
        if (params.containsKey("view3dModel")) activity.setView3dModel(getStr(params, "view3dModel"));
        if (params.containsKey("reservationEnabled")) activity.setReservationEnabled(getInt(params, "reservationEnabled"));
        if (params.containsKey("seatLayout")) activity.setSeatLayout(getStr(params, "seatLayout"));
        if (params.containsKey("totalTickets")) activity.setTotalTickets(getInt(params, "totalTickets"));
        if (params.containsKey("price")) activity.setPrice(getDecimal(params, "price"));
        if (params.containsKey("grabStartTime")) activity.setGrabStartTime(getDateTime(params, "grabStartTime"));
        if (params.containsKey("grabEndTime")) activity.setGrabEndTime(getDateTime(params, "grabEndTime"));

        activityMapper.updateById(activity);

        // 如果总票数变更，同步Redis库存
        if (params.containsKey("totalTickets") && activity.getTotalTickets() != null) {
            stockService.initStock(activity.getId(), activity.getTotalTickets());
        }

        // 同步活动标签（payload 携带 tagIds 时全量覆盖绑定，不携带则保持原样，兼容模板端旧提交）
        bindTagsIfPresent(params, id);

        return Result.success();
    }

    /**
     * 提交上架审核申请（status改为0待审核）
     */
    @PostMapping("/{id}/apply")
    public Result<Void> applyActivity(@PathVariable Long id) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        // 草稿(-1)/待审核(0)/被驳回(2)可提交审核；已下架(4)可重新提交审核走上架流程
        if (activity.getStatus() != -1 && activity.getStatus() != 0 && activity.getStatus() != 2
                && activity.getStatus() != 4) {
            throw new BusinessException("仅草稿、待审核、被驳回或已下架的活动可提交审核");
        }
        activity.setStatus(0);
        activity.setRejectReason(null);
        activityMapper.updateById(activity);
        return Result.success();
    }

    /**
     * 申请下架（status改为4）
     */
    @PostMapping("/{id}/offline")
    public Result<Void> offlineActivity(@PathVariable Long id) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        if (activity.getStatus() != 3) {
            throw new BusinessException("仅已发布的活动可申请下架");
        }
        activity.setStatus(4);
        activityMapper.updateById(activity);
        return Result.success();
    }

    /**
     * 我的活动列表（按operatorId过滤）
     */
    @GetMapping("/list")
    public Result<IPage<Activity>> getMyActivityList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        checkOperator();
        Long operatorId = requestContext.getCurrentUserId();
        Integer role = requestContext.getCurrentRole();
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        // 管理员(role=2)查看全部运营者活动；运营者(role=1)仅查看自己创建的活动
        if (role == null || role != 2) {
            wrapper.eq(Activity::getOperatorId, operatorId);
        }
        if (status != null) {
            wrapper.eq(Activity::getStatus, status);
        }
        wrapper.orderByDesc(Activity::getCreateTime);
        IPage<Activity> result = activityMapper.selectPage(pageParam, wrapper);
        // 填充分类名，供列表展示；按票档汇总"预计营收"（价格×已售，多票档口径正确）
        result.getRecords().forEach(a -> {
            if (a.getCategoryId() != null) {
                com.ticket.system.entity.Category cat = categoryMapper.selectById(a.getCategoryId());
                if (cat != null) {
                    a.setCategoryName(cat.getName());
                }
            }
            BigDecimal rev = ticketPriceMapper.selectList(
                    new LambdaQueryWrapper<com.ticket.system.entity.TicketPrice>()
                            .eq(com.ticket.system.entity.TicketPrice::getActivityId, a.getId()))
                    .stream()
                    .map(tp -> (tp.getPrice() == null || tp.getSoldStock() == null)
                            ? BigDecimal.ZERO
                            : tp.getPrice().multiply(BigDecimal.valueOf(tp.getSoldStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            a.setRevenue(rev);
        });
        return Result.success(result);
    }

    /**
     * 活动详情
     */
    @GetMapping("/{id}")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        fillActivityTags(activity);
        return Result.success(activity);
    }

    /**
     * 修改票务参数（未开售前可改，即grabStartTime > now）
     */
    @PutMapping("/{id}/ticket-config")
    public Result<Void> updateTicketConfig(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        LocalDateTime now = LocalDateTime.now();
        if (activity.getGrabStartTime() != null && !now.isBefore(activity.getGrabStartTime())) {
            throw new BusinessException("已开售，无法修改票务参数");
        }

        if (params.containsKey("totalTickets")) {
            activity.setTotalTickets(getInt(params, "totalTickets"));
        }
        if (params.containsKey("price")) {
            activity.setPrice(getDecimal(params, "price"));
        }
        if (params.containsKey("purchaseLimit")) {
            activity.setPurchaseLimit(getInt(params, "purchaseLimit"));
        }
        if (params.containsKey("grabStartTime")) {
            activity.setGrabStartTime(getDateTime(params, "grabStartTime"));
        }
        if (params.containsKey("grabEndTime")) {
            activity.setGrabEndTime(getDateTime(params, "grabEndTime"));
        }

        activityMapper.updateById(activity);

        // 如果总票数变更，同步Redis库存
        if (params.containsKey("totalTickets") && activity.getTotalTickets() != null) {
            stockService.initStock(activity.getId(), activity.getTotalTickets());
        }

        return Result.success();
    }

    /**
     * 保存活动票档（多票价/场次配置）：新增、修改、删除同一次提交内完成，
     * 并同步活动总库存/起步价/Redis 库存，保证活动卡片展示与分票档抢票口径一致。
     * 仅草稿(-1)/待审核(0)/被驳回(2)/已下架(4)的活动允许维护——已下架正是为了让运营调整票档后再重新上架。
     * 已有售出记录的票档不可删除。
     */
    @PutMapping("/{id}/tiers")
    public Result<List<com.ticket.system.entity.TicketPrice>> saveActivityTiers(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        if (activity.getStatus() == null || (activity.getStatus() != -1 && activity.getStatus() != 0
                && activity.getStatus() != 2 && activity.getStatus() != 4)) {
            throw new BusinessException("仅草稿、待审核、被驳回或已下架的活动可维护票档，已发布活动请先申请下架后再调整");
        }

        Object tiersObj = body.get("tiers");
        if (!(tiersObj instanceof List)) {
            throw new BusinessException("请至少配置一个票档");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : (List<?>) tiersObj) {
            if (o instanceof Map) {
                rows.add((Map<String, Object>) o);
            }
        }
        if (rows.isEmpty()) {
            throw new BusinessException("请至少配置一个票档（座位类型/票价/库存）");
        }

        List<com.ticket.system.entity.TicketPrice> existing = ticketPriceMapper.selectList(
                new LambdaQueryWrapper<com.ticket.system.entity.TicketPrice>()
                        .eq(com.ticket.system.entity.TicketPrice::getActivityId, id));
        Map<Long, com.ticket.system.entity.TicketPrice> existMap = new HashMap<>();
        for (com.ticket.system.entity.TicketPrice tp : existing) {
            existMap.put(tp.getId(), tp);
        }
        Set<Long> keepIds = new java.util.HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map<String, Object> row : rows) {
            String seatType = row.get("seatType") != null ? row.get("seatType").toString().trim() : null;
            BigDecimal price = row.get("price") != null ? new BigDecimal(row.get("price").toString()) : null;
            Integer totalStock = row.get("totalStock") != null
                    ? Integer.valueOf(row.get("totalStock").toString()) : null;
            if (seatType == null || seatType.isEmpty() || price == null || price.compareTo(BigDecimal.ZERO) <= 0
                    || totalStock == null || totalStock <= 0) {
                throw new BusinessException("每个票档需填写座位类型、大于0的票价与库存");
            }
            Long tpId = row.get("id") != null ? Long.valueOf(row.get("id").toString()) : null;
            if (tpId != null) {
                com.ticket.system.entity.TicketPrice tp = existMap.get(tpId);
                if (tp == null) {
                    throw new BusinessException("票档不存在或不属于该活动");
                }
                int sold = tp.getSoldStock() != null ? tp.getSoldStock() : 0;
                if (totalStock < sold) {
                    throw new BusinessException("票档【" + seatType + "】库存不能小于已售数量(" + sold + ")");
                }
                tp.setSeatType(seatType);
                tp.setPrice(price);
                tp.setTotalStock(totalStock);
                tp.setUpdateTime(now);
                ticketPriceMapper.updateById(tp);
                keepIds.add(tpId);
            } else {
                com.ticket.system.entity.TicketPrice tp = new com.ticket.system.entity.TicketPrice();
                tp.setActivityId(id);
                tp.setActivityTitle(activity.getTitle());
                tp.setSeatType(seatType);
                tp.setPrice(price);
                tp.setTotalStock(totalStock);
                tp.setSoldStock(0);
                tp.setSeatIndex(0);
                tp.setStatus(1);
                tp.setCreateTime(now);
                tp.setUpdateTime(now);
                ticketPriceMapper.insert(tp);
                keepIds.add(tp.getId());
            }
        }

        // 移除被删除的票档；已有销量的票档不允许删除，避免破坏已售数据关联
        for (com.ticket.system.entity.TicketPrice tp : existing) {
            if (!keepIds.contains(tp.getId())) {
                int sold = tp.getSoldStock() != null ? tp.getSoldStock() : 0;
                if (sold > 0) {
                    throw new BusinessException("票档【" + tp.getSeatType() + "】已有售出记录，不可删除，请保留该票档");
                }
                ticketPriceMapper.deleteById(tp.getId());
            }
        }

        // 回读并按序重排，计算总库存/总销量/起步价
        List<com.ticket.system.entity.TicketPrice> fresh = ticketPriceMapper.selectList(
                new LambdaQueryWrapper<com.ticket.system.entity.TicketPrice>()
                        .eq(com.ticket.system.entity.TicketPrice::getActivityId, id)
                        .orderByAsc(com.ticket.system.entity.TicketPrice::getSeatIndex)
                        .orderByAsc(com.ticket.system.entity.TicketPrice::getId));
        int index = 1;
        int totalSum = 0;
        int soldSum = 0;
        BigDecimal minPrice = null;
        for (com.ticket.system.entity.TicketPrice tp : fresh) {
            tp.setSeatIndex(index++);
            int sold = tp.getSoldStock() != null ? tp.getSoldStock() : 0;
            int stock = tp.getTotalStock() != null ? tp.getTotalStock() : 0;
            totalSum += stock;
            soldSum += sold;
            if (tp.getPrice() != null && (minPrice == null || tp.getPrice().compareTo(minPrice) < 0)) {
                minPrice = tp.getPrice();
            }
        }

        // 同步活动汇总字段与 Redis 剩余库存，保证首页/列表/抢票与票档口径一致
        activity.setTotalTickets(totalSum);
        activity.setPrice(minPrice);
        if (soldSum == 0) {
            activity.setSoldTickets(0);
        } else if (activity.getSoldTickets() == null || activity.getSoldTickets() == 0) {
            activity.setSoldTickets(soldSum);
        }
        activityMapper.updateById(activity);
        stockService.initStock(activity.getId(), totalSum - soldSum);

        return Result.success(fresh);
    }

    /**
     * 保存活动演出阵容 / 嘉宾（全量替换：前端提交整份列表，后端先删后插）。
     * 仅草稿(-1)/待审核(0)/被驳回(2)/已下架(4)的活动允许维护——已上架活动需先下架再调整。
     */
    @PutMapping("/{id}/lineup")
    @Transactional
    public Result<Void> saveActivityLineup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        checkOperator();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        checkOwnership(activity);
        if (activity.getStatus() != null && activity.getStatus() != -1 && activity.getStatus() != 0
                && activity.getStatus() != 2 && activity.getStatus() != 4) {
            throw new BusinessException("仅草稿、待审核、被驳回或已下架的活动可维护阵容，已上架活动请先申请下架");
        }

        Object listObj = body.get("lineup");
        List<ActivityLineup> items = new ArrayList<>();
        if (listObj instanceof List) {
            int order = 1;
            for (Object o : (List<?>) listObj) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> row = (Map<String, Object>) o;
                String name = row.get("name") != null ? row.get("name").toString().trim() : null;
                if (name == null || name.isEmpty()) {
                    continue; // 跳过未填姓名的空行
                }
                ActivityLineup item = new ActivityLineup();
                item.setActivityId(id);
                item.setName(name);
                item.setRole(row.get("role") != null ? row.get("role").toString().trim() : null);
                item.setAvatar(row.get("avatar") != null ? row.get("avatar").toString().trim() : null);
                item.setDescription(row.get("description") != null ? row.get("description").toString().trim() : null);
                Integer sort = row.get("sortOrder") != null ? getInt(row, "sortOrder") : null;
                item.setSortOrder(sort != null ? sort : order);
                items.add(item);
                order++;
            }
        }

        // 全量替换：删除旧阵容后批量插入新阵容
        activityLineupMapper.delete(new LambdaQueryWrapper<ActivityLineup>().eq(ActivityLineup::getActivityId, id));
        if (!items.isEmpty()) {
            for (ActivityLineup item : items) {
                activityLineupMapper.insert(item);
            }
        }
        return Result.success();
    }

    /**
     * payload 携带 tagIds 时全量覆盖活动标签（空数组=清除全部标签）
     */
    private void bindTagsIfPresent(Map<String, Object> params, Long activityId) {
        if (!params.containsKey("tagIds") || activityId == null) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        Object val = params.get("tagIds");
        if (val instanceof List) {
            for (Object o : (List<?>) val) {
                if (o != null) {
                    try {
                        ids.add(Long.valueOf(o.toString()));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法标签ID
                    }
                }
            }
        }
        tagService.bindActivityTags(activityId, ids);
    }

    /**
     * 填充活动标签列表（编辑页回显）
     */
    private void fillActivityTags(Activity activity) {
        if (activity == null || activity.getId() == null) {
            return;
        }
        List<Long> ids = tagService.getActivityTagIds(activity.getId());
        List<com.ticket.system.entity.ActivityTag> all = tagService.getAllTags();
        List<com.ticket.system.entity.ActivityTag> result = new ArrayList<>();
        if (ids != null && all != null) {
            for (com.ticket.system.entity.ActivityTag t : all) {
                if (t != null && ids.contains(t.getId())) {
                    result.add(t);
                }
            }
        }
        activity.setTags(result);
    }

    private String getStr(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? Long.valueOf(val.toString()) : null;
    }

    private Integer getInt(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? Integer.valueOf(val.toString()) : null;
    }

    private BigDecimal getDecimal(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val != null ? new BigDecimal(val.toString()) : null;
    }

    private LocalDateTime getDateTime(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) {
            return null;
        }
        String s = val.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        // 兼容 "2026-08-15 19:00:00"（前端 date-picker 格式）与 ISO "2026-08-15T19:00:00"
        if (s.contains(" ")) {
            return LocalDateTime.parse(s.replace(" ", "T"));
        }
        return LocalDateTime.parse(s);
    }
}
