package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.service.OrderService;
import com.ticket.system.service.StockService;
import com.ticket.system.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    private static final String RATE_LIMIT_PREFIX = "rate:limit:ticket:";
    /** IP 维度抢票节流：同一分钟同一来源 IP 最多允许的抢票请求数，用于限制多账号脚本刷票 */
    private static final String RATE_LIMIT_IP_PREFIX = "rate:limit:ip:ticket:";
    private static final String USER_LOCK_PREFIX = "lock:ticket:user:";

    @Value("${ticket.rate-limit.interval:3}")
    private Integer rateLimitInterval;

    @Value("${ticket.rate-limit.ip-per-minute:60}")
    private Integer ipRateLimitPerMinute;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private TicketPriceMapper ticketPriceMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private StockService stockService;

    @Resource
    private OrderService orderService;

    @Resource
    private com.ticket.system.service.impl.SeatService seatService;

    @Override
    public Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId) {
        return grabTicket(userId, activityId, quantity, ticketPriceId, (List<OrderAttendee>) null);
    }

    @Override
    public Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                              String attendeeName, String attendeeIdCard) {
        // 兼容旧版单观演人入参
        List<OrderAttendee> attendees = new ArrayList<>();
        boolean hasName = StringUtils.hasText(attendeeName);
        boolean hasIdCard = StringUtils.hasText(attendeeIdCard);
        if (hasName || hasIdCard) {
            if (!hasName || !hasIdCard) {
                throw new BusinessException("观演人姓名与身份证号需同时填写");
            }
            OrderAttendee a = new OrderAttendee();
            a.setAttendeeName(attendeeName.trim());
            a.setAttendeeIdCard(attendeeIdCard.trim());
            attendees.add(a);
        }
        return grabTicket(userId, activityId, quantity, ticketPriceId, attendees);
    }

    @Override
    public Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                              List<OrderAttendee> attendees) {
        return grabTicket(userId, activityId, quantity, ticketPriceId, attendees, null);
    }

    @Override
    public Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                              List<OrderAttendee> attendees, List<String> seats) {
        // 同一用户对同一活动的抢票加分布式锁，防止连续点击/多端重复请求
        String userLockKey = USER_LOCK_PREFIX + userId + ":" + activityId;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(userLockKey, "1", 5, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException("正在处理您的抢票请求，请勿重复提交");
        }

        try {
            return doGrabTicket(userId, activityId, quantity, ticketPriceId, attendees, seats);
        } finally {
            stringRedisTemplate.delete(userLockKey);
        }
    }

    private Orders doGrabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                                List<OrderAttendee> attendees, List<String> seats) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getGrabStartTime() != null && now.isBefore(activity.getGrabStartTime())) {
            throw new BusinessException("抢票尚未开始，开始时间: " + activity.getGrabStartTime());
        }
        if (activity.getGrabEndTime() != null && now.isAfter(activity.getGrabEndTime())) {
            throw new BusinessException("抢票已结束，结束时间: " + activity.getGrabEndTime());
        }

        if (activity.getStatus() != 3) {
            throw new BusinessException("该活动暂未开放抢票");
        }

        if (quantity == null || quantity <= 0) {
            throw new BusinessException("购买数量必须大于0");
        }
        // 实名场景下的早检（避免进入扣库存后再失败回滚）：
        // 一票一实名(2)：买多张必须为每张填实名；一单一实名(1)：买多张必须至少填 1 位实名购票人；
        // 免实名(0)：允许匿名购买多张。
        int realNameMode = activity.getRealNameMode() == null ? 0 : activity.getRealNameMode();
        if (quantity > 1 && (attendees == null || attendees.isEmpty())) {
            if (realNameMode == 2) {
                throw new BusinessException("该活动为“一票一实名”，每张门票都必须填写观演人身份信息");
            }
            if (realNameMode == 1) {
                throw new BusinessException("该活动为“一单一实名”，请填写 1 位实名购票人即可购买多张门票");
            }
        }

        // IP 维度节流：同一 IP 在窗口期内抢票过于频繁（典型多账号脚本特征）则整体限流
        if (!allowByIpRateLimit()) {
            throw new BusinessException("抢票请求过于频繁，请稍后再试");
        }

        Integer boughtCount = getUserTicketCount(userId, activityId);
        // 限购为空视为不限购，避免空指针
        if (activity.getPurchaseLimit() != null && boughtCount + quantity > activity.getPurchaseLimit()) {
            throw new BusinessException("超出限购数量，您已购" + boughtCount + "张，限购" + activity.getPurchaseLimit() + "张");
        }

        // ===== 选座场次：校验并锁定座位（对标大麦「在线选座」） =====
        List<String> lockedSeats = null;
        boolean seatMapEnabled = activity.getSeatMapEnabled() != null && activity.getSeatMapEnabled() == 1;
        if (seatMapEnabled) {
            if (seats == null || seats.isEmpty()) {
                throw new BusinessException("本场为选座场次，请先选择座位");
            }
            if (seats.size() != quantity) {
                throw new BusinessException("所选座位数(" + seats.size() + ")与购票数量(" + quantity + ")不一致");
            }
            if (new HashSet<>(seats).size() != seats.size()) {
                throw new BusinessException("座位不可重复选择");
            }
            // 校验座位合法性 + 未售，并锁定（带 TTL 覆盖订单超时，超时自动释放）
            seatService.validateSeats(activityId, activity, seats);
            if (!seatService.tryLockSeats(activityId, seats, 900)) {
                throw new BusinessException("您选择的座位中已有部分被占用，请重新选择");
            }
            lockedSeats = seats;
        }

        String rateLimitKey = RATE_LIMIT_PREFIX + userId + ":" + activityId;
        Boolean canRequest = stringRedisTemplate.opsForValue()
                .setIfAbsent(rateLimitKey, "1", rateLimitInterval, TimeUnit.SECONDS);
        if (canRequest == null || !canRequest) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        TicketPrice ticketPrice = null;
        if (ticketPriceId != null) {
            ticketPrice = ticketPriceMapper.selectById(ticketPriceId);
            if (ticketPrice == null || !ticketPrice.getActivityId().equals(activityId)) {
                throw new BusinessException("票档不存在或不属于该活动");
            }
            if (ticketPrice.getTotalStock() - ticketPrice.getSoldStock() < quantity) {
                throw new BusinessException("该票档库存不足");
            }
        }

        Long remainStock = stockService.decreaseStock(activityId, quantity);
        if (remainStock == null || remainStock == -2) {
            int remain = (activity.getTotalTickets() != null ? activity.getTotalTickets() : 0) 
                       - (activity.getSoldTickets() != null ? activity.getSoldTickets() : 0);
            stockService.initStock(activityId, remain);
            remainStock = stockService.decreaseStock(activityId, quantity);
        }
        if (remainStock != null && remainStock == -1) {
            throw new BusinessException("库存不足，抢票失败");
        }

        try {
            Orders order = orderService.createOrder(userId, activityId, quantity, 0, ticketPriceId, attendees);
            // 选座场次：将座位编码持久化到订单，供出票时落座
            if (lockedSeats != null && !lockedSeats.isEmpty()) {
                order.setSeats(com.ticket.system.util.SeatUtil.toJsonArray(lockedSeats));
                ordersMapper.updateById(order);
            }
            log.info("用户{}抢票成功，活动{}，数量{}，票档{}，实名观演人{}", userId, activityId, quantity, ticketPriceId,
                    attendees == null ? 0 : attendees.size());
            return order;
        } catch (BusinessException e) {
            // 业务校验失败（实名重复/观演人数量不匹配等）需先回滚已扣减库存，再透出原错误文案，便于用户知道失败原因
            rollbackStock(activityId, ticketPrice, quantity);
            if (lockedSeats != null) {
                seatService.releaseLocks(activityId, lockedSeats);
            }
            log.warn("用户{}抢票被拒：{}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            rollbackStock(activityId, ticketPrice, quantity);
            if (lockedSeats != null) {
                seatService.releaseLocks(activityId, lockedSeats);
            }
            log.error("用户{}抢票创建订单失败，回滚库存", userId, e);
            throw new BusinessException("抢票失败，请重试");
        }
    }

    /**
     * 抢票失败时回滚活动 Redis 总库存，避免因失败把库存扣没。
     * 注：票档已售库存（ticket_price.sold_stock）已在 OrderServiceImpl.createOrder 同事务内原子扣减，
     * 建单失败时由数据库事务整体回滚，无需在此手动回滚，避免与 createOrder 重复处理。
     */
    private void rollbackStock(Long activityId, TicketPrice ticketPrice, Integer quantity) {
        try {
            stockService.increaseStock(activityId, quantity);
        } catch (Exception ex) {
            log.error("回滚活动{}Redis库存失败", activityId, ex);
        }
    }

    @Override
    public Integer getUserTicketCount(Long userId, Long activityId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .eq(Orders::getActivityId, activityId)
               .in(Orders::getStatus, 0, 1);
        wrapper.select(Orders::getQuantity);
        List<Orders> orders = ordersMapper.selectList(wrapper);
        return orders.stream().mapToInt(Orders::getQuantity).sum();
    }

    /**
     * IP 维度的抢票节流校验：同一分钟同一 IP 允许 ipRateLimitPerMinute 次抢票请求。
     */
    private boolean allowByIpRateLimit() {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                return true;
            }
            String ip = getClientIp(request);
            if (!StringUtils.hasText(ip)) {
                return true;
            }
            // 本地回环地址（127.0.0.1 / ::1 / localhost）跳过 IP 维度节流：
            // 本地演示/多账号调试时所有请求共用同一 IP，若计入额度极易被误伤；真实远程 IP 仍正常限流
            if ("127.0.0.1".equals(ip) || "::1".equals(ip)
                    || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equals(ip)) {
                return true;
            }
            String minute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String key = RATE_LIMIT_IP_PREFIX + ip + ":" + minute;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, 70, TimeUnit.SECONDS);
            }
            if (count != null && count > ipRateLimitPerMinute) {
                log.warn("IP {} 触发抢票节流：1分钟内{}次", ip, count);
                return false;
            }
        } catch (Exception e) {
            // 节流失效不应阻断抢票主流程，放行
        }
        return true;
    }

    private HttpServletRequest getRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取真实客户端 IP：优先取 X-Forwarded-For（多级代理场景取最左非空项），其次取 RemoteAddr。
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}