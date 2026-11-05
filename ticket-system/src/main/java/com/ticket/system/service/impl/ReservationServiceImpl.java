package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Reservation;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.ReservationMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.service.OrderService;
import com.ticket.system.service.ReservationService;
import com.ticket.system.service.StockService;
import com.ticket.system.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final String RESERVATION_QUEUE_PREFIX = "reservation:queue:";
    private static final String WAIT_QUEUE_PREFIX = "wait:queue:";
    private static final String RESERVATION_NUMBER_PREFIX = "reservation:number:";
    /** 同一用户对同一活动的预约分布式锁，防止快速连点/多端并发重复预约 */
    private static final String RESERVATION_LOCK_PREFIX = "lock:reservation:user:";
    /** 候补队列分布式锁：防止同一用户重复加入候补队列 */
    private static final String WAIT_QUEUE_LOCK_PREFIX = "lock:wait:user:";
    /** 候补队列用户-活动映射：记录用户在哪些活动的候补队列中，用于"我的候补"列表查询 */
    private static final String WAIT_USER_ACTIVITIES_PREFIX = "wait:user:activities:";

    @Resource
    private ReservationMapper reservationMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private TicketPriceMapper ticketPriceMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private StockService stockService;

    @Resource
    private OrderService orderService;

    @Resource
    private MessageService messageService;

    @Override
    @Transactional
    public Reservation createReservation(Long userId, Long activityId, String seatType, Long ticketPriceId) {
        // 同一用户对同一活动加分布式锁，防止快速连点/多端并发生成多条重复预约
        String lockKey = RESERVATION_LOCK_PREFIX + userId + ":" + activityId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("正在处理您的预约请求，请勿重复提交");
        }

        try {
            // 检查活动是否存在且上架售卖中（status=3）
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                throw new BusinessException("活动不存在");
            }
            if (activity.getStatus() != 3) {
                throw new BusinessException("该活动暂未开放预约");
            }
            // 预约开关：活动关闭预约时不允许预约
            if (activity.getReservationEnabled() != null && activity.getReservationEnabled() == 0) {
                throw new BusinessException("该活动未开放预约，请开售时直接抢票");
            }
            // 预约用于“准备开售”的票：仅抢票未开始时允许预约，已开售则无需预约（倒计时提醒在详情页展示）
            if (activity.getGrabStartTime() != null && !LocalDateTime.now().isBefore(activity.getGrabStartTime())) {
                throw new BusinessException("该活动已开售，无需预约，请直接抢票");
            }

            // 票档校验：预约时选定的票档必须属于该活动且有可用余票，开抢后按预约票档直接出票
            String finalSeatType = seatType;
            if (ticketPriceId != null) {
                TicketPrice ticketPrice = ticketPriceMapper.selectById(ticketPriceId);
                if (ticketPrice == null || !ticketPrice.getActivityId().equals(activityId)) {
                    throw new BusinessException("所选票档不存在或不属于该活动");
                }
                if (ticketPrice.getTotalStock() != null && ticketPrice.getSoldStock() != null
                        && ticketPrice.getTotalStock() - ticketPrice.getSoldStock() <= 0) {
                    throw new BusinessException("所选票档已售罄，请重新选择");
                }
                // 座位类型以票档为准，保证出票一致
                finalSeatType = ticketPrice.getSeatType();
            }

            // 检查是否已预约（有效预约含排队中/已出票，已取消的记录允许重新预约）
            LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Reservation::getUserId, userId)
                   .eq(Reservation::getActivityId, activityId)
                   .ne(Reservation::getStatus, 2);
            Long count = reservationMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException("您已预约过该活动，请勿重复预约");
            }

            // 使用Redis自增获取排队序号
            String numberKey = RESERVATION_NUMBER_PREFIX + activityId;
            Long queueNumber = stringRedisTemplate.opsForValue().increment(numberKey);

            // 加入Redis预约队列
            String queueKey = RESERVATION_QUEUE_PREFIX + activityId;
            stringRedisTemplate.opsForList().rightPush(queueKey, String.valueOf(userId));

            // 写入数据库；若入库失败则补偿移出 Redis 队列，避免“排着队但无预约记录”的幽灵条目
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setActivityId(activityId);
            reservation.setQueueNumber(queueNumber.intValue());
            reservation.setSeatType(finalSeatType);
            reservation.setTicketPriceId(ticketPriceId);
            reservation.setStatus(0); // 排队中
            try {
                reservationMapper.insert(reservation);
            } catch (Exception e) {
                stringRedisTemplate.opsForList().remove(queueKey, 1, String.valueOf(userId));
                log.warn("用户{}预约活动{}入库失败，已移出预约队列", userId, activityId);
                throw e;
            }

            // 查询总排队人数
            Long totalQueue = stringRedisTemplate.opsForList().size(queueKey);
            reservation.setTotalQueueCount(totalQueue != null ? totalQueue.intValue() : 0);
            reservation.setActivityTitle(activity.getTitle());
            reservation.setGrabStartTime(activity.getGrabStartTime());

            log.info("用户{}预约活动{}，排队序号: {}", userId, activityId, queueNumber);

            // 发送预约成功消息通知
            String countdownTip = "";
            if (activity.getGrabStartTime() != null) {
                long remainMillis = java.time.Duration.between(LocalDateTime.now(), activity.getGrabStartTime()).toMillis();
                if (remainMillis > 0) {
                    long days = remainMillis / (1000L * 60 * 60 * 24);
                    long hours = (remainMillis % (1000L * 60 * 60 * 24)) / (1000L * 60 * 60);
                    countdownTip = "距离开售约还有 " + (days > 0 ? days + "天" : "") + hours + "小时，开售前请注意查收提醒并提前进入页面等待。";
                }
            }
            messageService.sendMessage(userId, "预约成功",
                    "您已成功预约活动【" + activity.getTitle() + "】，排队序号：" + queueNumber + "，抢票开启后将优先为您出票。"
                            + countdownTip, 2);

            return reservation;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public IPage<Reservation> getMyReservations(Long userId, Integer page, Integer size) {
        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
               .orderByDesc(Reservation::getCreateTime);
        IPage<Reservation> result = reservationMapper.selectPage(pageParam, wrapper);

        // 填充活动标题和总排队人数
        result.getRecords().forEach(this::fillReservationInfo);
        return result;
    }

    @Override
    public Reservation getReservationDetail(Long userId, Long activityId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
               .eq(Reservation::getActivityId, activityId)
               .ne(Reservation::getStatus, 2);
        Reservation reservation = reservationMapper.selectOne(wrapper);
        if (reservation == null) {
            // 未预约属于正常状态，返回 null 由前端判断，不作为异常提示
            return null;
        }
        fillReservationInfo(reservation);
        return reservation;
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, Long activityId) {
        // 查询预约记录
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
               .eq(Reservation::getActivityId, activityId)
               .eq(Reservation::getStatus, 0);
        Reservation reservation = reservationMapper.selectOne(wrapper);
        if (reservation == null) {
            throw new BusinessException("未找到有效的预约记录");
        }

        // 从Redis队列中移除
        String queueKey = RESERVATION_QUEUE_PREFIX + activityId;
        stringRedisTemplate.opsForList().remove(queueKey, 1, String.valueOf(userId));

        // 更新数据库状态为已取消
        reservation.setStatus(2);
        reservationMapper.updateById(reservation);

        log.info("用户{}取消预约活动{}", userId, activityId);
    }

    @Override
    public void processReservationQueue(Long activityId) {
        String queueKey = RESERVATION_QUEUE_PREFIX + activityId;
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("活动{}不存在，跳过预约队列处理", activityId);
            return;
        }
        // 仅抢票窗口内的活动可自动出票，避免提前触发
        if (activity.getStatus() == null || activity.getStatus() != 3
                || (activity.getGrabStartTime() != null && LocalDateTime.now().isBefore(activity.getGrabStartTime()))) {
            log.info("活动{}不在抢票窗口内，跳过预约出票", activityId);
            return;
        }
        // 一票一实名(2)场次：队列无法收集每位观演人身份证，退化为“预约提醒”，开抢后由用户手动实名抢票
        if (activity.getRealNameMode() != null && activity.getRealNameMode() == 2) {
            log.info("活动{}为一票一实名场次，跳过自动出票（仅作预约提醒，开抢后需手动实名抢票）", activityId);
            return;
        }

        // 注意：本方法不做整体事务管理——Redis 扣减/出队无法回滚。
        // 每个用户“建单”由 orderService 独立事务完成，失败时在此补偿 Redis。
        while (true) {
            // 从队列头部取出一个预约用户（先出队，避免占着库存却一直不消费）
            String userIdStr = stringRedisTemplate.opsForList().leftPop(queueKey);
            if (userIdStr == null) {
                log.info("活动{}预约队列为空，停止出票", activityId);
                break;
            }
            Long userId = Long.parseLong(userIdStr);

            // 扣减 Redis 库存
            Long remainStock = stockService.decreaseStock(activityId, 1);
            if (remainStock == null || remainStock == -2) {
                // Redis 无库存缓存，初始化后重试一次
                int remain = activity.getTotalTickets() - activity.getSoldTickets();
                stockService.initStock(activityId, remain);
                remainStock = stockService.decreaseStock(activityId, 1);
            }
            if (remainStock != null && remainStock == -1) {
                // 库存不足，Lua 脚本未实际扣减，用户放回队尾等下一批
                stringRedisTemplate.opsForList().rightPush(queueKey, String.valueOf(userId));
                log.info("活动{}库存不足，停止预约出票", activityId);
                break;
            }
            if (remainStock == null) {
                stringRedisTemplate.opsForList().rightPush(queueKey, String.valueOf(userId));
                log.info("活动{}库存操作异常，停止预约出票", activityId);
                break;
            }

            try {
                // 查询该用户预约记录，取预约时选定的票档；无预约记录或无票档时按默认出票
                Long reservedTicketPriceId = null;
                LambdaQueryWrapper<Reservation> resWrapper = new LambdaQueryWrapper<>();
                resWrapper.eq(Reservation::getUserId, userId)
                        .eq(Reservation::getActivityId, activityId)
                        .ne(Reservation::getStatus, 2);
                Reservation res = reservationMapper.selectOne(resWrapper);
                if (res != null) {
                    reservedTicketPriceId = res.getTicketPriceId();
                }
                // 为该预约用户生成待支付订单（source=1 预约出票，按预约票档直接出票），独立事务，失败不影响其他用户
                orderService.createOrder(userId, activityId, 1, 1, reservedTicketPriceId);
            } catch (Exception e) {
                // 建单失败：归还 Redis 库存，用户放回队尾，等待下次触发
                stockService.increaseStock(activityId, 1);
                stringRedisTemplate.opsForList().rightPush(queueKey, String.valueOf(userId));
                log.warn("预约用户{}出票失败：{}，库存已归还", userId, e.getMessage());
                break;
            }

            // 建单成功后更新预约状态为已出票
            try {
                LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Reservation::getUserId, userId)
                       .eq(Reservation::getActivityId, activityId)
                       .eq(Reservation::getStatus, 0);
                Reservation reservation = reservationMapper.selectOne(wrapper);
                if (reservation != null) {
                    reservation.setStatus(1); // 已出票
                    reservationMapper.updateById(reservation);
                }
            } catch (Exception e) {
                log.warn("预约状态更新失败（不影响出票）：{}", e.getMessage());
            }

            // 发送出票通知（尽力而为，失败不影响出票结果）
            try {
                messageService.sendMessage(userId, "预约出票成功",
                        "您预约的活动【" + activity.getTitle() + "】已优先出票，请尽快完成支付。", 3);
            } catch (Exception e) {
                log.warn("预约出票通知发送失败：{}", e.getMessage());
            }
            log.info("预约用户{}出票成功，活动{}", userId, activityId);
        }
    }

    @Override
    public void processWaitQueue(Long activityId) {
        String waitQueueKey = WAIT_QUEUE_PREFIX + activityId;
        String userActivitiesKey = WAIT_USER_ACTIVITIES_PREFIX + activityId;
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return;
        }
        // 仅售卖中(3)的活动才补票，避免为已下架活动生成新订单
        if (activity.getStatus() == null || activity.getStatus() != 3) {
            log.info("活动{}不在售卖中，跳过补票", activityId);
            return;
        }

        // 不在本方法开启整体事务，逐用户补偿
        int processed = 0;
        while (true) {
            // 检查当前库存
            Integer stock = stockService.getStock(activityId);
            if (stock == null || stock <= 0) {
                log.info("活动{}无可用库存，停止补票，本轮已补{}张", activityId, processed);
                break;
            }

            // 从候补队列（ZSet）取出 score 最小的用户（最早加入的）
            Set<String> members = stringRedisTemplate.opsForZSet().range(waitQueueKey, 0, 0);
            if (members == null || members.isEmpty()) {
                log.info("活动{}候补队列为空，本轮已补{}张", activityId, processed);
                break;
            }
            String userIdStr = members.iterator().next();
            Long userId = Long.parseLong(userIdStr);

            // 原子移除该用户（防止并发重复补票）
            Long removed = stringRedisTemplate.opsForZSet().remove(waitQueueKey, userIdStr);
            if (removed == null || removed == 0) {
                // 被其他线程抢走了，继续下一个
                continue;
            }

            // 扣减 Redis 库存
            Long remainStock = stockService.decreaseStock(activityId, 1);
            if (remainStock != null && remainStock == -1) {
                // 库存不足，Lua 脚本未实际扣减，用户放回队列
                stringRedisTemplate.opsForZSet().add(waitQueueKey, userIdStr, System.currentTimeMillis());
                log.info("活动{}库存不足，补票停止，本轮已补{}张", activityId, processed);
                break;
            }
            if (remainStock == null || remainStock == -2) {
                // 库存缓存异常，用户放回队列
                stringRedisTemplate.opsForZSet().add(waitQueueKey, userIdStr, System.currentTimeMillis());
                log.info("活动{}库存缓存异常，补票停止", activityId);
                break;
            }

            // 候补补票未指定票档：选取该活动下尚有可用余量的票档，保证票档已售库存与活动总销量对账一致
            // （否则补票订单只增活动总销量而不增票档销量，会造成 tier 级数据失真）
            Long finalTicketPriceId = null;
            if (finalTicketPriceId == null) {
                TicketPrice avail = ticketPriceMapper.selectOne(
                        new LambdaQueryWrapper<TicketPrice>()
                                .eq(TicketPrice::getActivityId, activityId)
                                .apply("sold_stock < total_stock")
                                .last("LIMIT 1"));
                if (avail != null) {
                    finalTicketPriceId = avail.getId();
                }
            }
            try {
                // 为候补用户生成待支付订单（source=2 补票），独立事务
                orderService.createOrder(userId, activityId, 1, 2, finalTicketPriceId);
            } catch (Exception e) {
                // 建单失败：归还库存并把用户放回队尾（下次释放票时重试）
                stockService.increaseStock(activityId, 1);
                stringRedisTemplate.opsForZSet().add(waitQueueKey, userIdStr, System.currentTimeMillis());
                log.warn("候补用户{}补票失败：{}，库存已归还", userId, e.getMessage());
                break;
            }

            // 从用户-活动映射中移除
            try {
                stringRedisTemplate.opsForSet().remove(userActivitiesKey, userIdStr);
            } catch (Exception e) {
                log.warn("移除候补用户-活动映射失败：{}", e.getMessage());
            }

            // 发送补票成功通知（尽力而为）
            try {
                messageService.sendMessage(userId, "补票成功",
                        "您候补排队的活动【" + activity.getTitle() + "】已有余票，已自动为您补票，请尽快完成支付。", 3);
            } catch (Exception e) {
                log.warn("补票通知发送失败：{}", e.getMessage());
            }
            processed++;
            log.info("候补用户{}补票成功，活动{}", userId, activityId);
        }
    }

    @Override
    public Map<String, Object> joinWaitQueue(Long userId, Long activityId) {
        String lockKey = WAIT_QUEUE_LOCK_PREFIX + userId + ":" + activityId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("正在处理您的候补请求，请勿重复提交");
        }

        try {
            // 检查活动
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                throw new BusinessException("活动不存在");
            }
            if (activity.getStatus() != 3) {
                throw new BusinessException("该活动暂未开放候补");
            }

            // 检查用户是否已有待支付或已支付订单（有票就不用候补了）
            LambdaQueryWrapper<com.ticket.system.entity.Orders> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(com.ticket.system.entity.Orders::getUserId, userId)
                    .eq(com.ticket.system.entity.Orders::getActivityId, activityId)
                    .in(com.ticket.system.entity.Orders::getStatus, 0, 1);
            Long orderCount = ordersMapper.selectCount(orderWrapper);
            if (orderCount != null && orderCount > 0) {
                throw new BusinessException("您已有该活动的订单，无需候补");
            }

            // 检查是否已在候补队列中
            String waitQueueKey = WAIT_QUEUE_PREFIX + activityId;
            Double score = stringRedisTemplate.opsForZSet().score(waitQueueKey, String.valueOf(userId));
            if (score != null) {
                throw new BusinessException("您已在候补队列中，请勿重复加入");
            }

            // 加入 ZSet 候补队列（score = 当前时间戳，越早加入越靠前）
            long timestamp = System.currentTimeMillis();
            stringRedisTemplate.opsForZSet().add(waitQueueKey, String.valueOf(userId), timestamp);

            // 记录到用户-活动映射，便于"我的候补"查询
            String userActivitiesKey = WAIT_USER_ACTIVITIES_PREFIX + activityId;
            stringRedisTemplate.opsForSet().add(userActivitiesKey, String.valueOf(userId));

            // 查询排队位置（ZRANK 是 0-based，+1 后是 1-based）
            Long rank = stringRedisTemplate.opsForZSet().rank(waitQueueKey, String.valueOf(userId));
            Long total = stringRedisTemplate.opsForZSet().size(waitQueueKey);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("activityId", activityId);
            result.put("activityTitle", activity.getTitle());
            result.put("queuePosition", rank != null ? rank.intValue() + 1 : 1);
            result.put("totalWaiters", total != null ? total.intValue() : 1);
            result.put("joinTime", timestamp);

            // 发送候补成功通知
            try {
                messageService.sendMessage(userId, "候补成功",
                        "您已成功加入活动【" + activity.getTitle() + "】的候补队列，当前排位：第" + (rank != null ? rank.intValue() + 1 : 1) + "位。有退票时将按顺序自动补票。", 2);
            } catch (Exception e) {
                log.warn("候补成功通知发送失败：{}", e.getMessage());
            }

            log.info("用户{}加入活动{}候补队列，排位{}", userId, activityId,
                    rank != null ? rank.intValue() + 1 : 1);
            return result;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public void leaveWaitQueue(Long userId, Long activityId) {
        String waitQueueKey = WAIT_QUEUE_PREFIX + activityId;
        String userActivitiesKey = WAIT_USER_ACTIVITIES_PREFIX + activityId;

        Double score = stringRedisTemplate.opsForZSet().score(waitQueueKey, String.valueOf(userId));
        if (score == null) {
            throw new BusinessException("您不在该活动的候补队列中");
        }

        stringRedisTemplate.opsForZSet().remove(waitQueueKey, String.valueOf(userId));
        stringRedisTemplate.opsForSet().remove(userActivitiesKey, String.valueOf(userId));

        log.info("用户{}退出活动{}候补队列", userId, activityId);
    }

    @Override
    public Map<String, Object> getWaitQueuePosition(Long userId, Long activityId) {
        String waitQueueKey = WAIT_QUEUE_PREFIX + activityId;
        Map<String, Object> result = new HashMap<>();
        result.put("activityId", activityId);

        Double score = stringRedisTemplate.opsForZSet().score(waitQueueKey, String.valueOf(userId));
        boolean inQueue = score != null;
        result.put("inQueue", inQueue);

        if (inQueue) {
            Long rank = stringRedisTemplate.opsForZSet().rank(waitQueueKey, String.valueOf(userId));
            Long total = stringRedisTemplate.opsForZSet().size(waitQueueKey);
            result.put("queuePosition", rank != null ? rank.intValue() + 1 : 0);
            result.put("totalWaiters", total != null ? total.intValue() : 0);
            result.put("joinTime", score != null ? score.longValue() : 0L);
        } else {
            Long total = stringRedisTemplate.opsForZSet().size(waitQueueKey);
            result.put("queuePosition", 0);
            result.put("totalWaiters", total != null ? total.intValue() : 0);
        }

        // 填充活动标题
        Activity activity = activityMapper.selectById(activityId);
        if (activity != null) {
            result.put("activityTitle", activity.getTitle());
            result.put("coverImage", activity.getCoverImage());
            result.put("venue", activity.getVenue());
            result.put("startTime", activity.getStartTime());
        }

        return result;
    }

    @Override
    public IPage<Map<String, Object>> getMyWaitList(Long userId, Integer page, Integer size) {
        // 由于 Redis 中无法直接按用户反查"所有活动的候补队列"，
        // 采用 SCAN 遍历所有 wait:queue:* 键的方式在大数据量下效率太低。
        // 这里通过 WAIT_USER_ACTIVITIES_PREFIX 维护用户-活动映射关系，便于反向查询。
        // 注：实际生产中建议用 DB 表持久化候补记录；本系统为轻量级实现，
        // 扫描所有活动检查用户是否在队列中，对毕业设计体量完全足够。

        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        List<Map<String, Object>> records = new ArrayList<>();

        // 查询所有在售活动
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getStatus, 3)
                .orderByDesc(Activity::getCreateTime);
        List<Activity> activities = activityMapper.selectList(wrapper);

        // 逐个检查用户是否在候补队列中
        for (Activity activity : activities) {
            String waitQueueKey = WAIT_QUEUE_PREFIX + activity.getId();
            Double score = stringRedisTemplate.opsForZSet().score(waitQueueKey, String.valueOf(userId));
            if (score != null) {
                Long rank = stringRedisTemplate.opsForZSet().rank(waitQueueKey, String.valueOf(userId));
                Long total = stringRedisTemplate.opsForZSet().size(waitQueueKey);

                Map<String, Object> item = new HashMap<>();
                item.put("activityId", activity.getId());
                item.put("activityTitle", activity.getTitle());
                item.put("coverImage", activity.getCoverImage());
                item.put("venue", activity.getVenue());
                item.put("startTime", activity.getStartTime());
                item.put("queuePosition", rank != null ? rank.intValue() + 1 : 0);
                item.put("totalWaiters", total != null ? total.intValue() : 0);
                item.put("joinTime", score.longValue());
                records.add(item);
            }
        }

        // 按加入时间排序（越早加入越靠前）
        records.sort(Comparator.comparingLong(m -> ((Long) m.get("joinTime"))));

        // 手动分页
        int total = records.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageRecords = fromIndex < total
                ? records.subList(fromIndex, toIndex)
                : Collections.emptyList();

        pageResult.setRecords(pageRecords);
        pageResult.setTotal(total);
        pageResult.setCurrent(page);
        pageResult.setSize(size);
        return pageResult;
    }

    /**
     * 填充预约记录的非数据库字段
     */
    private void fillReservationInfo(Reservation reservation) {
        // 填充活动标题与开抢时间（预约倒计时提醒用）
        Activity activity = activityMapper.selectById(reservation.getActivityId());
        if (activity != null) {
            reservation.setActivityTitle(activity.getTitle());
            reservation.setGrabStartTime(activity.getGrabStartTime());
        }

        // 填充总排队人数
        String queueKey = RESERVATION_QUEUE_PREFIX + reservation.getActivityId();
        Long totalQueue = stringRedisTemplate.opsForList().size(queueKey);
        reservation.setTotalQueueCount(totalQueue != null ? totalQueue.intValue() : 0);
    }
}
