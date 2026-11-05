package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.AfterCommit;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.OrderHandleRecord;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import com.ticket.system.mapper.OrderAttendeeMapper;
import com.ticket.system.mapper.OrderHandleRecordMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.service.OrderService;
import com.ticket.system.service.ReservationService;
import com.ticket.system.service.StockService;
import com.ticket.system.service.MessageService;
import com.ticket.system.service.FinanceService;
import com.ticket.system.service.TicketCheckService;
import com.ticket.system.util.IdCardUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /** 订单号序号发生器：同秒内单调递增，杜绝随机数在同一秒内碰撞导致唯一键冲突 */
    private static final AtomicLong ORDER_NO_SEQ = new AtomicLong(0);

    @Value("${order.timeout:15}")
    private Integer orderTimeout;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private TicketPriceMapper ticketPriceMapper;

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private OrderAttendeeMapper orderAttendeeMapper;

    @Resource
    private OrderHandleRecordMapper orderHandleRecordMapper;

    @Resource
    private StockService stockService;

    @Lazy
    @Resource
    private ReservationService reservationService;

    @Resource
    private MessageService messageService;

    @Lazy
    @Resource
    private FinanceService financeService;

    @Lazy
    @Resource
    private TicketCheckService ticketCheckService;

    @Resource
    private com.ticket.system.service.impl.SeatService seatService;

    @Override
    @Transactional
    public Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId) {
        return createOrder(userId, activityId, quantity, source, ticketPriceId, (List<OrderAttendee>) null);
    }

    @Override
    @Transactional
    public Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId,
                               String attendeeName, String attendeeIdCard) {
        // 兼容旧版单观演人入参：转换成观演人列表后统一走多观演人链路
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
        return createOrder(userId, activityId, quantity, source, ticketPriceId, attendees);
    }

    @Override
    @Transactional
    public Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId,
                               List<OrderAttendee> attendeeList) {
        // 同一活动行加悲观锁（FOR UPDATE）串行化整个建单事务：
        // 实名“一人一证”跨订单查重、限购数量、票档校验全部在锁内完成，
        // 杜绝两个账号/两次请求并发同时通过查重从而重复占票的竞态窗口。
        // 活动行也是后续 sold_tickets 原子扣减的目标，锁持有期间行为一致，不会引入死锁。
        Activity activity = activityMapper.selectOne(
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getId, activityId)
                        .last("FOR UPDATE"));
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("购买数量必须大于0");
        }

        // 开售窗口统一收口校验（所有出票入口共用）：未开抢 / 已结束 / 非上架状态一律禁止建单，
        // 避免"活动未开售却产生销量/订单"的数据矛盾（预约自动出票入口本身已在抢票窗口内触发，不受影响）
        LocalDateTime saleNow = LocalDateTime.now();
        if (activity.getStatus() == null || activity.getStatus() != 3) {
            throw new BusinessException("该活动暂未开放抢票");
        }
        if (activity.getGrabStartTime() != null && saleNow.isBefore(activity.getGrabStartTime())) {
            throw new BusinessException("抢票尚未开始，开始时间: " + activity.getGrabStartTime());
        }
        if (activity.getGrabEndTime() != null && saleNow.isAfter(activity.getGrabEndTime())) {
            throw new BusinessException("抢票已结束，结束时间: " + activity.getGrabEndTime());
        }

        // 实名模式：0 免实名 / 1 一单一实名（电竞，一个实名可购多张）/ 2 一票一实名（演唱会，每张票都必须实名）
        int realNameMode = activity.getRealNameMode() == null ? 0 : activity.getRealNameMode();
        // 直播间/预约出票补票（source=1/2）无法在队列阶段收集每位观演人，仅对“直接抢票(source=0)”做强实名校验
        boolean directBuy = source == null || source == 0;

        // 观演人校验与标准化：姓名/身份证成对、身份证格式校验、去空白项、限同单数量、单内去重
        List<OrderAttendee> attendees = normalizeAndValidateAttendees(quantity, attendeeList, realNameMode, directBuy);

        // 实名唯一核验（跨单）：模式要求实名时，同一场活动同一身份证只能出现在一个有效订单中
        if (directBuy && realNameMode != 0) {
            checkAttendeeDuplicateAcrossOrders(activityId, attendees, realNameMode);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        String seatType = "";

        if (ticketPriceId != null) {
            TicketPrice ticketPrice = ticketPriceMapper.selectById(ticketPriceId);
            if (ticketPrice != null) {
                totalAmount = ticketPrice.getPrice().multiply(new BigDecimal(quantity));
                seatType = ticketPrice.getSeatType();
            } else {
                throw new BusinessException("票档不存在");
            }
        } else if (activity.getPrice() != null) {
            totalAmount = activity.getPrice().multiply(new BigDecimal(quantity));
        } else {
            throw new BusinessException("未设置票价，请先配置票档");
        }

        // 为兼容旧版展示逻辑，订单主表冗余第一位观演人（完整观演人以 order_attendee 子表为准）
        OrderAttendee first = attendees.isEmpty() ? null : attendees.get(0);

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setExpireTime(LocalDateTime.now().plusMinutes(orderTimeout));
        order.setSource(source);
        order.setTicketPriceId(ticketPriceId);
        order.setSeatType(seatType);
        order.setAttendeeName(first != null ? first.getAttendeeName() : null);
        order.setAttendeeIdCard(first != null ? first.getAttendeeIdCard() : null);
        ordersMapper.insert(order);

        // 多观演人批量落子表：每张票对应一位观演人（一票一证）
        for (OrderAttendee a : attendees) {
            a.setOrderId(order.getId());
            a.setActivityId(activityId);
            a.setCreateTime(LocalDateTime.now());
            orderAttendeeMapper.insert(a);
        }

        // 活动库存 DB 层原子条件扣减：仅当 sold_tickets + quantity <= total_tickets 时才累加，
        // 作为 Redis 库存之外的数据库兜底，双保险杜绝超卖
        int stockUpdated = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .apply("sold_tickets + " + quantity + " <= total_tickets")
                .setSql("sold_tickets = sold_tickets + " + quantity));
        if (stockUpdated == 0) {
            throw new BusinessException("库存不足，抢票失败");
        }

        // 票档已售库存原子条件扣减：与活动总库存在「同一事务、同一活动行 FOR UPDATE」保护下完成，
        // 作为票档维度的库存兜底。统一在此处处理，兼容「直购、预约出票、候补补票」所有建单入口，
        // 避免预约/补票订单只增活动总销量而不增票档销量的数据不一致（此前 tier 库存仅在直购路径维护）。
        if (ticketPriceId != null) {
            int tierUpdated = ticketPriceMapper.update(null, new LambdaUpdateWrapper<TicketPrice>()
                    .eq(TicketPrice::getId, ticketPriceId)
                    .eq(TicketPrice::getActivityId, activityId)
                    .apply("sold_stock + " + quantity + " <= total_stock")
                    .setSql("sold_stock = sold_stock + " + quantity));
            if (tierUpdated == 0) {
                throw new BusinessException("该票档库存不足，请更换票档");
            }
        }

        order.setActivityTitle(activity.getTitle());
        order.setCoverImage(activity.getCoverImage());
        order.setActivityStartTime(activity.getStartTime());
        order.setVenue(activity.getVenue());

        log.info("创建订单: orderNo={}, userId={}, activityId={}, quantity={}, source={}, ticketPriceId={}, 观演人数量={}",
                order.getOrderNo(), userId, activityId, quantity, source, ticketPriceId, attendees.size());
        return order;
    }

    /**
     * 观演人标准化与校验（按活动实名模式区分规则）：
     * <ul>
     *   <li>剔除完全空白的行；姓名/身份证必须成对填写，否则报错</li>
     *   <li>身份证格式合法性校验；同一订单内身份证不得重复</li>
     *   <li>一票一实名(2)：直接抢票时必须为每张票填齐一位观演人，数量必须等于购票数量</li>
     *   <li>一单一实名(1)：直接抢票时只需 1 位实名（购票人/POC），可购买多张，全部票共用该实名</li>
     *   <li>免实名(0)：允许匿名；若填写观演人则数量须等于购票数量（沿用旧逻辑）</li>
     * </ul>
     * 直接抢票(scource=0)时按上述强校验；预约/补票出票(source≠0)走宽松校验，避免队列阶段因信息不足被拦截。
     */
    private List<OrderAttendee> normalizeAndValidateAttendees(Integer quantity, List<OrderAttendee> attendeeList,
                                                               int realNameMode, boolean directBuy) {
        List<OrderAttendee> normalized = new ArrayList<>();
        if (attendeeList != null) {
            for (OrderAttendee raw : attendeeList) {
                if (raw == null) {
                    continue;
                }
                String name = raw.getAttendeeName() == null ? null : raw.getAttendeeName().trim();
                // 归一化身份证号：去空白、统一大写X，保证同单/跨单“一人一证”判重不受写法差异影响
                String idCard = IdCardUtil.normalize(raw.getAttendeeIdCard());
                boolean hasName = StringUtils.hasText(name);
                boolean hasIdCard = StringUtils.hasText(idCard);
                if (!hasName && !hasIdCard) {
                    // 空槽位视为匿名票，跳过
                    continue;
                }
                if (!hasName || !hasIdCard) {
                    throw new BusinessException("观演人姓名与身份证号需成对填写");
                }
                if (!IdCardUtil.isValid(idCard)) {
                    throw new BusinessException("观演人身份证号码不合法");
                }
                OrderAttendee a = new OrderAttendee();
                a.setAttendeeName(name);
                a.setAttendeeIdCard(idCard);
                normalized.add(a);
            }
        }

        if (!directBuy) {
            // 预约/补票出票：不强制实名数量，避免队列一键出票被拦截
            return normalized;
        }

        if (realNameMode == 2) {
            // 一票一实名：每张票都必须实名，数量必须等于购票数量
            if (normalized.isEmpty()) {
                throw new BusinessException("该活动为“一票一实名”，每张门票都必须填写观演人身份证，请为每张票填写一位观演人");
            }
            if (normalized.size() != quantity) {
                throw new BusinessException("已填写 " + normalized.size() + " 位观演人，与购票数量 " + quantity
                        + " 不一致，请为每张票填写一位观演人（一票一实名）");
            }
        } else if (realNameMode == 1) {
            // 一单一实名：只需 1 位实名（购票人），可购买多张
            if (normalized.isEmpty()) {
                throw new BusinessException("该活动为“一单一实名”，请填写 1 位实名观演人（购票人）即可购买多张门票");
            }
            if (normalized.size() > 1) {
                throw new BusinessException("该活动为“一单一实名”，只需 1 位实名购票人即可购买多张门票，请勿重复填写");
            }
        }
        // 免实名(0)：normalized 可为空（匿名），已填则沿用旧逻辑要求数量一致

        if (normalized.isEmpty()) {
            return normalized;
        }
        if (realNameMode == 0 && normalized.size() != quantity) {
            throw new BusinessException("已填写 " + normalized.size() + " 位观演人，与购票数量 " + quantity
                    + " 不一致，请补齐全部观演人或清空后按匿名购买");
        }
        // 同单去重
        Set<String> seen = new HashSet<>();
        for (OrderAttendee a : normalized) {
            if (!seen.add(a.getAttendeeIdCard())) {
                throw new BusinessException("同一身份证号在一笔订单中只能绑定一次，请勿重复添加观演人");
            }
        }
        return normalized;
    }

    /**
     * 跨订单实名唯一核验：同一场活动同一身份证号，在「待支付/已支付」订单中只能出现一次。
     * 兼顾旧数据（orders 冗余列）与新数据（order_attendee 子表）。
     * 一票一实名(2)：逐张判重；一单一实名(1)：只校验购票人这 1 个身份证。
     */
    private void checkAttendeeDuplicateAcrossOrders(Long activityId, List<OrderAttendee> attendees, int realNameMode) {
        for (OrderAttendee a : attendees) {
            String idCard = a.getAttendeeIdCard();
            Long ordersDup = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getActivityId, activityId)
                    .eq(Orders::getAttendeeIdCard, idCard)
                    .in(Orders::getStatus, 0, 1));
            if (ordersDup != null && ordersDup > 0) {
                throw new BusinessException("观演人(" + IdCardUtil.mask(idCard) + ")已购买本场活动门票，同一实名不可重复购票");
            }
            // 多观演人订单中，该观演人可能不是订单主表第一位，需查子表关联的有效订单
            List<OrderAttendee> rows = orderAttendeeMapper.selectList(new LambdaQueryWrapper<OrderAttendee>()
                    .eq(OrderAttendee::getActivityId, activityId)
                    .eq(OrderAttendee::getAttendeeIdCard, idCard));
            if (!rows.isEmpty()) {
                List<Long> orderIds = rows.stream().map(OrderAttendee::getOrderId).distinct().collect(Collectors.toList());
                Long orderDup = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                        .in(Orders::getId, orderIds)
                        .in(Orders::getStatus, 0, 1));
                if (orderDup != null && orderDup > 0) {
                    throw new BusinessException("观演人(" + IdCardUtil.mask(idCard) + ")已购买本场活动门票，同一实名不可重复购票");
                }
            }
        }
    }

    @Override
    public IPage<Orders> getMyOrders(Long userId, Integer page, Integer size, Integer status) {
        Page<Orders> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        IPage<Orders> result = ordersMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillOrderInfo);
        return result;
    }

    @Override
    public Orders getOrderDetail(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        fillOrderInfo(order);
        return order;
    }

    @Override
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }
        // 检查是否超时
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException("订单已超时，请重新抢票");
        }

        // 条件更新防止并发竞态：仅当订单仍为待支付时才能置为已支付
        int updated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 0)
                .set(Orders::getStatus, 1)
                .set(Orders::getPayTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        order.setStatus(1); // 已支付
        order.setPayTime(order.getPayTime() == null ? LocalDateTime.now() : order.getPayTime());

        // 记录支付流水
        financeService.recordPayment(order.getOrderNo(), userId, order.getActivityId(),
                order.getTotalAmount(), LocalDateTime.now());

        // 生成电子票：失败时抛出异常使整个支付事务回滚（含支付流水），
        // 杜绝“已支付但缺/缺电子票”的不一致，用户可重新支付后自动补票
        ticketCheckService.generateTickets(order.getId());

        // 发送支付成功消息通知
        Activity act = activityMapper.selectById(order.getActivityId());
        messageService.sendMessage(userId, "支付成功",
                "您购买的活动【" + (act != null ? act.getTitle() : "") + "】订单已支付成功，订单号：" + order.getOrderNo(), 3);

        log.info("订单{}支付成功，用户{}", order.getOrderNo(), userId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待支付订单可取消");
        }

        // 条件更新：仅当仍为待支付时才置为已取消，避免与支付/超时任务竞态
        int updated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 0)
                .set(Orders::getStatus, 2));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        // 释放Redis库存
        stockService.increaseStock(order.getActivityId(), order.getQuantity());

        // 回滚票档已售库存
        rollbackTicketPriceStock(order);

        // 原子更新数据库sold_tickets减回
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Activity::getId, order.getActivityId())
                     .setSql("sold_tickets = GREATEST(0, sold_tickets - " + order.getQuantity() + ")");
        activityMapper.update(null, updateWrapper);

        // 事务提交后再触发补票，避免补票失败导致取消操作被回滚
        AfterCommit.run(() -> reservationService.processWaitQueue(order.getActivityId()));

        // 释放选座场次被锁定的座位（锁本身带 TTL，这里主动清理）
        releaseSeatLocks(order);

        log.info("订单{}已取消，释放库存{}，活动{}", order.getOrderNo(), order.getQuantity(), order.getActivityId());
    }

    @Override
    @Transactional
    public void expireOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return;
        }

        // 条件更新：若用户刚好完成支付（status已变1），此处更新0行直接返回，不得释放库存
        int updated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 0)
                .set(Orders::getStatus, 3));
        if (updated == 0) {
            log.info("订单{}状态已被其他操作变更，跳过超时处理", orderId);
            return;
        }

        // 回滚票档已售库存
        rollbackTicketPriceStock(order);

        // 发送订单过期消息通知
        Activity act = activityMapper.selectById(order.getActivityId());
        messageService.sendMessage(order.getUserId(), "订单已过期",
                "您购买的活动【" + (act != null ? act.getTitle() : "") + "】订单因超时未支付已自动取消，票源已释放。", 3);

        // 释放Redis库存
        stockService.increaseStock(order.getActivityId(), order.getQuantity());

        // 原子更新数据库sold_tickets减回
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Activity::getId, order.getActivityId())
                     .setSql("sold_tickets = GREATEST(0, sold_tickets - " + order.getQuantity() + ")");
        activityMapper.update(null, updateWrapper);

        // 事务提交后再触发补票
        AfterCommit.run(() -> reservationService.processWaitQueue(order.getActivityId()));

        // 释放选座场次被锁定的座位（锁本身带 TTL，这里主动清理）
        releaseSeatLocks(order);

        log.info("订单{}已超时自动取消，释放库存{}，活动{}", order.getOrderNo(), order.getQuantity(), order.getActivityId());
    }

    @Override
    public IPage<Orders> getOrderPage(Integer page, Integer size, Long activityId,
                                      Integer status, String keyword, LocalDateTime startTime, LocalDateTime endTime,
                                      Long scopeOperatorId) {
        Page<Orders> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (activityId != null) {
            wrapper.eq(Orders::getActivityId, activityId);
        }
        // 运营端订单管理：仅看归属自己活动的订单，避免越权查看他人活动订单
        if (scopeOperatorId != null) {
            wrapper.inSql(Orders::getActivityId, "SELECT id FROM activity WHERE operator_id = " + scopeOperatorId);
        }
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Orders::getOrderNo, keyword);
        }
        if (startTime != null) {
            wrapper.ge(Orders::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Orders::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        IPage<Orders> result = ordersMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillOrderInfo);
        return result;
    }

    @Override
    @Transactional
    public void handleAbnormalOrder(Long orderId, Integer handleType, String remark,
                                    Long operatorId, String operatorName) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (handleType == null || (handleType != 1 && handleType != 2 && handleType != 3)) {
            throw new BusinessException("请选择正确的处理方式");
        }

        // 记录处理日志（无论哪种处理方式都留痕，便于追溯）
        insertHandleRecord(orderId, handleType, remark, operatorId, operatorName);

        // 处理方式2：强制取消订单，释放库存并作废电子票
        if (handleType == 2) {
            if (order.getStatus() == 0 || order.getStatus() == 1) {
                Integer originalStatus = order.getStatus();
                int updated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, originalStatus)
                        .set(Orders::getStatus, 2));
                if (updated == 0) {
                    throw new BusinessException("订单状态已变化，请刷新后重试");
                }

                // 释放Redis库存
                stockService.increaseStock(order.getActivityId(), order.getQuantity());
                // 回滚票档已售库存；已支付订单还需作废电子票
                rollbackTicketPriceStock(order);
                if (originalStatus == 1) {
                    invalidateOrderTickets(orderId);
                }
                // 事务提交后触发补票（取消/异常处理释放的票统一补给出队用户）
                AfterCommit.run(() -> reservationService.processWaitQueue(order.getActivityId()));

                log.info("异常订单{}已强制取消，原状态: {}", order.getOrderNo(), originalStatus);
            }
        }

        // 处理方式1（恢复订单）/ 3（标记已解决）：不改订单状态，仅记录处理结果
        log.info("异常订单{}处理完成，方式: {}，备注: {}，原状态: {}",
                order.getOrderNo(), handleType, remark, order.getStatus());
    }

    /**
     * 写入订单异常处理记录
     */
    private void insertHandleRecord(Long orderId, Integer handleType, String remark,
                                    Long operatorId, String operatorName) {
        OrderHandleRecord record = new OrderHandleRecord();
        record.setOrderId(orderId);
        record.setHandleType(handleType);
        record.setHandleRemark(remark);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setCreateTime(LocalDateTime.now());
        orderHandleRecordMapper.insert(record);
    }

    @Override
    public List<OrderHandleRecord> getHandleRecords(Long orderId) {
        LambdaQueryWrapper<OrderHandleRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderHandleRecord::getOrderId, orderId)
               .orderByDesc(OrderHandleRecord::getCreateTime);
        return orderHandleRecordMapper.selectList(wrapper);
    }

    /**
     * 生成订单号：毫秒时间戳 + 进程内单调序号。
     * 相比“秒级时间戳+随机数”，同毫秒内序号单调递增，可彻底避免高并发下随机碰撞导致的唯一键冲突。
     */
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        long seq = ORDER_NO_SEQ.incrementAndGet() % 100000;
        return "TK" + timestamp + String.format("%05d", seq);
    }

    /**
     * 释放选座场次订单锁定的座位（锁本身带 TTL，这里主动清理避免锁死库存）
     */
    private void releaseSeatLocks(Orders order) {
        try {
            if (order != null && order.getSeats() != null) {
                java.util.List<String> codes = com.ticket.system.util.SeatUtil.parseSeats(order.getSeats());
                if (codes != null && !codes.isEmpty()) {
                    seatService.releaseLocks(order.getActivityId(), codes);
                }
            }
        } catch (Exception e) {
            log.warn("释放座位锁失败 orderId={}", order != null ? order.getId() : null, e);
        }
    }

    /**
     * 填充订单非数据库字段
     */
    private void fillOrderInfo(Orders order) {
        Activity activity = activityMapper.selectById(order.getActivityId());
        if (activity != null) {
            order.setActivityTitle(activity.getTitle());
            order.setCoverImage(activity.getCoverImage());
            order.setActivityStartTime(activity.getStartTime());
            order.setVenue(activity.getVenue());
        }
    }

    @Override
    public Map<String, Object> getElectronicTicket(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("订单未支付，无法查看电子票");
        }

        Activity activity = activityMapper.selectById(order.getActivityId());
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("orderNo", order.getOrderNo());
        ticket.put("quantity", order.getQuantity());
        ticket.put("totalAmount", order.getTotalAmount());
        ticket.put("payTime", order.getPayTime());
        ticket.put("activityTitle", activity != null ? activity.getTitle() : "");
        ticket.put("venue", activity != null ? activity.getVenue() : "");
        ticket.put("startTime", activity != null ? activity.getStartTime() : "");
        ticket.put("endTime", activity != null ? activity.getEndTime() : "");
        ticket.put("coverImage", activity != null ? activity.getCoverImage() : "");
        // 返回电子表中的真实核销码，供检票使用
        List<ElectronicTicket> tickets = electronicTicketMapper.selectList(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, orderId)
                        .orderByAsc(ElectronicTicket::getId));
        List<Map<String, Object>> ticketList = new java.util.ArrayList<>();
        for (ElectronicTicket t : tickets) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("ticketNo", t.getTicketNo());
            item.put("verifyCode", t.getVerifyCode());
            item.put("seatType", t.getSeatType());
            item.put("seatInfo", t.getSeatInfo());
            item.put("attendeeName", t.getAttendeeName());
            item.put("status", t.getStatus());
            ticketList.add(item);
        }
        ticket.put("tickets", ticketList);
        ticket.put("verifyCode", tickets.isEmpty() ? null : tickets.get(0).getVerifyCode());
        ticket.put("valid", order.getStatus() == 1);
        return ticket;
    }

    @Override
    @Transactional
    public void refundOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("仅已支付订单可申请退款");
        }

        // 【退票规则】统一前置校验（借鉴大麦：按活动退票策略 + 退票截止时间）
        Activity activity = activityMapper.selectById(order.getActivityId());
        if (activity != null) {
            if (activity.getRefundPolicy() != null && activity.getRefundPolicy() == 0) {
                throw new BusinessException("该活动为“不可退票”，购票时已在购票须知中说明，无法申请退款");
            }
            // 退票截止：演出一旦开始即不再支持退票
            if (activity.getStartTime() != null && !LocalDateTime.now().isBefore(activity.getStartTime())) {
                throw new BusinessException("演出已开始，不再支持退票");
            }
        }

        // 【关键防护】检查是否有已核销的电子票：已入场的票不可退款，防止"先入场再退款"的金融漏洞
        Long verifiedCount = electronicTicketMapper.selectCount(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, orderId)
                        .eq(ElectronicTicket::getStatus, 1));
        if (verifiedCount != null && verifiedCount > 0) {
            throw new BusinessException("订单下有" + verifiedCount + "张票已核销入场，无法退款。" +
                    "如确有特殊情况，请联系客服人工处理。");
        }

        int updated = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 1)
                .set(Orders::getStatus, 5));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        // 记录退款流水
        financeService.recordRefund(order.getOrderNo(), userId, order.getActivityId(),
                order.getTotalAmount(), LocalDateTime.now());

        stockService.increaseStock(order.getActivityId(), order.getQuantity());
        rollbackTicketPriceStock(order);
        invalidateOrderTickets(orderId);
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Activity::getId, order.getActivityId())
                     .setSql("sold_tickets = GREATEST(0, sold_tickets - " + order.getQuantity() + ")");
        activityMapper.update(null, updateWrapper);

        // 退款释放的票事务提交后再触发补票
        AfterCommit.run(() -> reservationService.processWaitQueue(order.getActivityId()));

        Activity act = activityMapper.selectById(order.getActivityId());
        messageService.sendMessage(userId, "退款成功",
                "您购买的活动【" + (act != null ? act.getTitle() : "") + "】订单已退款成功，订单号：" + order.getOrderNo(), 3);

        log.info("订单{}已退款，用户{}", order.getOrderNo(), userId);
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        // 仅允许删除已终态订单（已取消2/已过期3/已退款5），待支付订单请走取消流程释放库存，已支付订单不可删
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 5) {
            throw new BusinessException("仅已取消/已过期/已退款的订单可删除");
        }
        ordersMapper.deleteById(orderId);
        // 同步清理订单关联的观演人子表数据
        orderAttendeeMapper.delete(new LambdaQueryWrapper<OrderAttendee>()
                .eq(OrderAttendee::getOrderId, orderId));
        log.info("订单{}已删除，用户{}", order.getOrderNo(), userId);
    }

    /**
     * 回滚票档已售库存（取消/过期/退款时调用）
     */
    private void rollbackTicketPriceStock(Orders order) {
        if (order.getTicketPriceId() == null) {
            return;
        }
        try {
            ticketPriceMapper.update(null, new LambdaUpdateWrapper<TicketPrice>()
                    .eq(TicketPrice::getId, order.getTicketPriceId())
                    .setSql("sold_stock = GREATEST(0, sold_stock - " + order.getQuantity() + ")"));
        } catch (Exception e) {
            log.error("回滚票档{}库存失败", order.getTicketPriceId(), e);
        }
    }

    /**
     * 作废订单关联的电子票（退款/异常处理时调用），未核销的票置为已作废(2)
     */
    private void invalidateOrderTickets(Long orderId) {
        try {
            electronicTicketMapper.update(null, new LambdaUpdateWrapper<ElectronicTicket>()
                    .eq(ElectronicTicket::getOrderId, orderId)
                    .eq(ElectronicTicket::getStatus, 0)
                    .set(ElectronicTicket::getStatus, 2)
                    .set(ElectronicTicket::getUpdateTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("作废订单{}电子票失败", orderId, e);
        }
    }
}
