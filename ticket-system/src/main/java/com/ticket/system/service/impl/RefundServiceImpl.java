package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.AfterCommit;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ActivityNotify;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.RefundOrder;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityNotifyMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.RefundOrderMapper;
import com.ticket.system.mapper.TicketPriceMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.FinanceService;
import com.ticket.system.service.MessageService;
import com.ticket.system.service.RefundService;
import com.ticket.system.service.ReservationService;
import com.ticket.system.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 退票工单服务实现
 */
@Slf4j
@Service
public class RefundServiceImpl implements RefundService {

    /** 默认手续费比例 10% */
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.10");
    /** 退票工单号单调序号 */
    private static final AtomicLong REFUND_NO_SEQ = new AtomicLong(0);

    @Resource
    private RefundOrderMapper refundOrderMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private TicketPriceMapper ticketPriceMapper;

    @Lazy
    @Resource
    private FinanceService financeService;

    @Lazy
    @Resource
    private StockService stockService;

    @Lazy
    @Resource
    private MessageService messageService;

    @Lazy
    @Resource
    private ReservationService reservationService;

    @Lazy
    @Resource
    private SeatService seatService;

    @Resource
    private ActivityNotifyMapper activityNotifyMapper;

    @Override
    @Transactional
    public RefundOrder applyRefund(Long userId, Long orderId, String reason) {
        // 校验订单：对订单行加悲观锁，串行化同一订单的重复申请/重复退款并发，保证校验一致性
        Orders order = ordersMapper.selectOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getId, orderId)
                        .last("FOR UPDATE"));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("仅已支付订单可申请退票");
        }

        // 已核销的票不允许退票
        Long checkedCount = electronicTicketMapper.selectCount(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, orderId)
                        .eq(ElectronicTicket::getStatus, 1));
        if (checkedCount != null && checkedCount > 0) {
            throw new BusinessException("该订单门票已核销入场，无法申请退票");
        }

        // 活动已开演不允许退票
        Activity act = activityMapper.selectById(order.getActivityId());
        if (act == null) {
            throw new BusinessException("活动不存在");
        }
        // 退票策略：不可退（例如演唱会）拒绝申请，避免“抢了还能退”的逻辑漏洞
        if (act.getRefundPolicy() != null && act.getRefundPolicy() == 0) {
            throw new BusinessException("该活动门票为「不可退票」，购票后不支持退票，请您购票前仔细阅读活动注意事项");
        }
        if (act.getStartTime() != null && LocalDateTime.now().isAfter(act.getStartTime())) {
            throw new BusinessException("活动已开始，无法申请退票");
        }

        // 校验是否已存在未处理完的退票工单
        Long existCount = refundOrderMapper.selectCount(
                new LambdaQueryWrapper<RefundOrder>()
                        .eq(RefundOrder::getOrderId, orderId)
                        .in(RefundOrder::getStatus, 0, 1));
        if (existCount > 0) {
            throw new BusinessException("该订单已存在待审核或已通过的退票申请");
        }

        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("退款原因不能为空");
        }

        // 计算手续费：暂定固定10%，可根据活动时间动态计算
        BigDecimal feeRate = calcFeeRate(order.getActivityId());
        BigDecimal refundAmount = order.getTotalAmount();
        BigDecimal feeAmount = refundAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = refundAmount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP);

        // 创建退票工单
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(generateRefundNo());
        refundOrder.setOrderId(order.getId());
        refundOrder.setOrderNo(order.getOrderNo());
        refundOrder.setUserId(userId);
        refundOrder.setActivityId(order.getActivityId());
        refundOrder.setRefundAmount(refundAmount);
        refundOrder.setFeeAmount(feeAmount);
        refundOrder.setActualAmount(actualAmount);
        refundOrder.setReason(reason);
        refundOrder.setStatus(0); // 待审核
        refundOrderMapper.insert(refundOrder);

        // 填充非数据库字段
        fillRefundInfo(refundOrder);

        log.info("用户{}申请退票，订单{}，退款金额{}，手续费{}，实退{}",
                userId, order.getOrderNo(), refundAmount, feeAmount, actualAmount);
        return refundOrder;
    }

    @Override
    public IPage<RefundOrder> getRefundPage(Integer page, Integer size, Integer status, String keyword, Long scopeOperatorId) {
        Page<RefundOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RefundOrder::getStatus, status);
        }
        // 运营端退款审核：仅看归属自己活动的退票工单
        if (scopeOperatorId != null) {
            wrapper.inSql(RefundOrder::getActivityId, "SELECT id FROM activity WHERE operator_id = " + scopeOperatorId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RefundOrder::getRefundNo, keyword)
                    .or().like(RefundOrder::getOrderNo, keyword)
                    .or().like(RefundOrder::getReason, keyword));
        }
        wrapper.orderByDesc(RefundOrder::getCreateTime);
        IPage<RefundOrder> result = refundOrderMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillRefundInfo);
        return result;
    }

    @Override
    @Transactional
    public void approveRefund(Long auditorId, String auditorName, Long refundId, String remark) {
        RefundOrder refundOrder = refundOrderMapper.selectById(refundId);
        if (refundOrder == null) {
            throw new BusinessException("退票工单不存在");
        }

        // 条件更新：仅“待审核(0)”的工单可置为“通过(1)”，防止两名审核人同时点击造成的重复审核/重复退款
        LocalDateTime auditTime = LocalDateTime.now();
        int updated = refundOrderMapper.update(null, new LambdaUpdateWrapper<RefundOrder>()
                .eq(RefundOrder::getId, refundId)
                .eq(RefundOrder::getStatus, 0)
                .set(RefundOrder::getStatus, 1)
                .set(RefundOrder::getAuditorId, auditorId)
                .set(RefundOrder::getAuditorName, auditorName)
                .set(RefundOrder::getAuditRemark, remark)
                .set(RefundOrder::getAuditTime, auditTime));
        if (updated == 0) {
            throw new BusinessException("退票工单状态已变化，仅待审核工单可通过，请刷新后重试");
        }
        refundOrder.setStatus(1);
        refundOrder.setAuditorId(auditorId);
        refundOrder.setAuditorName(auditorName);
        refundOrder.setAuditRemark(remark);
        refundOrder.setAuditTime(auditTime);

        log.info("退票工单{}审核通过，审核人{}", refundOrder.getRefundNo(), auditorName);

        // 审核通过后立即执行退款
        executeRefund(refundId);
    }

    @Override
    @Transactional
    public void executeRefund(Long refundId) {
        RefundOrder refundOrder = refundOrderMapper.selectById(refundId);
        if (refundOrder == null) {
            throw new BusinessException("退票工单不存在");
        }
        if (refundOrder.getStatus() != 1) {
            throw new BusinessException("仅审核通过的工单可执行退款");
        }

        // 更新订单状态为已退款(5)：条件更新，仅已支付订单可退，防止并发重复退款
        Orders order = ordersMapper.selectById(refundOrder.getOrderId());
        if (order == null) {
            throw new BusinessException("关联订单不存在");
        }
        // 复查：申请后至执行退款前，若任一张票已被核销入场，则不可退款（杜绝“已入场后退款”漏洞）
        Long checkedCount = electronicTicketMapper.selectCount(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, order.getId())
                        .eq(ElectronicTicket::getStatus, 1));
        if (checkedCount != null && checkedCount > 0) {
            throw new BusinessException("该订单门票已核销入场，无法执行退款");
        }
        int updated = ordersMapper.update(null,
                new LambdaUpdateWrapper<Orders>()
                        .eq(Orders::getId, order.getId())
                        .eq(Orders::getStatus, 1)
                        .set(Orders::getStatus, 5));
        if (updated == 0) {
            throw new BusinessException("订单状态已变化（可能已退款或已取消），无法执行退款");
        }

        // 记录退款流水（实退金额）
        financeService.recordRefund(order.getOrderNo(), order.getUserId(), order.getActivityId(),
                refundOrder.getActualAmount(), LocalDateTime.now());

        // 记录手续费流水
        if (refundOrder.getFeeAmount() != null && refundOrder.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            financeService.recordFee(order.getOrderNo(), order.getUserId(), order.getActivityId(),
                    refundOrder.getFeeAmount(), LocalDateTime.now());
        }

        // 作废未核销的电子票，防止退款后仍可入场
        electronicTicketMapper.update(null,
                new LambdaUpdateWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, order.getId())
                        .eq(ElectronicTicket::getStatus, 0)
                        .set(ElectronicTicket::getStatus, 2)
                        .set(ElectronicTicket::getUpdateTime, LocalDateTime.now()));

        // 释放选座场次占用的座位锁：退款后座位应可再次出售，
        // 避免抢票阶段写入的 Redis 座位锁残留（TTL 15 分钟）导致该座位持续“被占用”而无法被他人选购
        releaseSeatLocks(order);

        // 回滚票档已售库存
        if (order.getTicketPriceId() != null) {
            ticketPriceMapper.update(null,
                    new LambdaUpdateWrapper<com.ticket.system.entity.TicketPrice>()
                            .eq(com.ticket.system.entity.TicketPrice::getId, order.getTicketPriceId())
                            .setSql("sold_stock = GREATEST(0, sold_stock - " + order.getQuantity() + ")"));
        }

        // 释放库存
        stockService.increaseStock(order.getActivityId(), order.getQuantity());
        LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Activity::getId, order.getActivityId())
                     .setSql("sold_tickets = GREATEST(0, sold_tickets - " + order.getQuantity() + ")");
        activityMapper.update(null, updateWrapper);

        // 退款释放的库存事务提交后统一补给出队用户
        AfterCommit.run(() -> reservationService.processWaitQueue(order.getActivityId()));

        // 更新工单状态为已退款：条件更新（仅“通过(1)”可置为“已退款(3)”），防止重复执行退款
        LocalDateTime refundTime = LocalDateTime.now();
        int refundStateUpdated = refundOrderMapper.update(null, new LambdaUpdateWrapper<RefundOrder>()
                .eq(RefundOrder::getId, refundId)
                .eq(RefundOrder::getStatus, 1)
                .set(RefundOrder::getStatus, 3)
                .set(RefundOrder::getRefundTime, refundTime));
        if (refundStateUpdated == 0) {
            throw new BusinessException("退票工单状态已变化，无法完成退款，请勿重复操作");
        }
        refundOrder.setStatus(3);
        refundOrder.setRefundTime(refundTime);

        // 发送消息通知用户
        Activity act = activityMapper.selectById(order.getActivityId());
        String actTitle = act != null ? act.getTitle() : "";
        messageService.sendMessage(order.getUserId(), "退款成功",
                "您购买的活动【" + actTitle + "】退款申请已通过并完成退款，订单号：" + order.getOrderNo()
                        + "，实退金额：" + refundOrder.getActualAmount() + "元。", 3);

        // 退票回流通知：将释放出的库存通知“到货提醒”订阅用户（缺货登记/回流补票，对标猫眼/大麦）。
        // 排除退款人本人（其刚退回的票不应再通知自己），失败不影响退款主流程。
        try {
            Long subActivityId = order.getActivityId();
            List<ActivityNotify> subscribers = activityNotifyMapper.selectList(
                    new LambdaQueryWrapper<ActivityNotify>()
                            .eq(ActivityNotify::getActivityId, subActivityId)
                            .ne(ActivityNotify::getUserId, order.getUserId()));
            if (subscribers != null && !subscribers.isEmpty()) {
                String noticeTitle = "退票回流通知";
                String noticeContent = "您关注的【" + actTitle + "】有票回流/补票啦，名额有限，快去抢购！";
                for (ActivityNotify sub : subscribers) {
                    messageService.sendMessage(sub.getUserId(), noticeTitle, noticeContent, 4);
                }
                log.info("退票回流通知已发送给{}位订阅用户（活动{}）", subscribers.size(), subActivityId);
            }
        } catch (Exception e) {
            log.warn("退票回流通知发送失败（不影响退款完成）：{}", e.getMessage());
        }

        log.info("退票工单{}执行退款完成，订单{}", refundOrder.getRefundNo(), order.getOrderNo());
    }

    /**
     * 释放订单占用的座位锁（退款后将座位重新放回可售池）
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
            log.warn("退款释放座位锁失败 orderId={}", order != null ? order.getId() : null, e);
        }
    }

    @Override
    @Transactional
    public void rejectRefund(Long auditorId, String auditorName, Long refundId, String remark) {
        RefundOrder refundOrder = refundOrderMapper.selectById(refundId);
        if (refundOrder == null) {
            throw new BusinessException("退票工单不存在");
        }

        // 条件更新：仅“待审核(0)”可置为“驳回(2)”，避免与通过操作并发竞态
        LocalDateTime auditTime = LocalDateTime.now();
        int updated = refundOrderMapper.update(null, new LambdaUpdateWrapper<RefundOrder>()
                .eq(RefundOrder::getId, refundId)
                .eq(RefundOrder::getStatus, 0)
                .set(RefundOrder::getStatus, 2)
                .set(RefundOrder::getAuditorId, auditorId)
                .set(RefundOrder::getAuditorName, auditorName)
                .set(RefundOrder::getAuditRemark, remark)
                .set(RefundOrder::getAuditTime, auditTime));
        if (updated == 0) {
            throw new BusinessException("退票工单状态已变化，仅待审核工单可驳回，请刷新后重试");
        }
        refundOrder.setStatus(2); // 驳回
        refundOrder.setAuditorId(auditorId);
        refundOrder.setAuditorName(auditorName);
        refundOrder.setAuditRemark(remark);
        refundOrder.setAuditTime(auditTime);

        // 发送消息通知用户
        Orders order = ordersMapper.selectById(refundOrder.getOrderId());
        if (order != null) {
            Activity act = activityMapper.selectById(order.getActivityId());
            String actTitle = act != null ? act.getTitle() : "";
            String content = "您购买的活动【" + actTitle + "】退款申请已被驳回，订单号：" + order.getOrderNo();
            if (StringUtils.hasText(remark)) {
                content += "，驳回原因：" + remark;
            }
            messageService.sendMessage(order.getUserId(), "退款申请被驳回", content, 3);
        }

        log.info("退票工单{}审核驳回，审核人{}", refundOrder.getRefundNo(), auditorName);
    }

    @Override
    public List<RefundOrder> getMyRefunds(Long userId) {
        LambdaQueryWrapper<RefundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundOrder::getUserId, userId)
               .orderByDesc(RefundOrder::getCreateTime);
        List<RefundOrder> list = refundOrderMapper.selectList(wrapper);
        list.forEach(this::fillRefundInfo);
        return list;
    }

    @Override
    public Map<String, Object> previewRefund(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("refundable", false);
        result.put("totalAmount", order.getTotalAmount());

        Activity act = activityMapper.selectById(order.getActivityId());
        if (act == null) {
            result.put("reason", "活动不存在");
            return result;
        }
        result.put("activityTitle", act.getTitle());
        result.put("startTime", act.getStartTime());

        // 仅已支付订单可退
        if (order.getStatus() != 1) {
            result.put("reason", "仅已支付订单可申请退票");
            return result;
        }
        // 不可退票活动直接拦截
        if (act.getRefundPolicy() != null && act.getRefundPolicy() == 0) {
            result.put("refundPolicy", 0);
            result.put("reason", "本场活动为「不可退票」，购票后不支持退票，请您购票前仔细阅读活动注意事项");
            return result;
        }
        // 演出已开始不可退
        if (act.getStartTime() != null && LocalDateTime.now().isAfter(act.getStartTime())) {
            result.put("reason", "演出已开始，无法申请退票");
            return result;
        }
        // 已核销入场的票不可退
        Long checkedCount = electronicTicketMapper.selectCount(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, orderId)
                        .eq(ElectronicTicket::getStatus, 1));
        if (checkedCount != null && checkedCount > 0) {
            result.put("reason", "该订单门票已核销入场，无法申请退票");
            return result;
        }
        // 已有待审核/已通过的退票工单
        Long existCount = refundOrderMapper.selectCount(
                new LambdaQueryWrapper<RefundOrder>()
                        .eq(RefundOrder::getOrderId, orderId)
                        .in(RefundOrder::getStatus, 0, 1));
        if (existCount != null && existCount > 0) {
            result.put("reason", "该订单已有退票申请正在处理中，请勿重复申请");
            return result;
        }

        // 可退：按退票阶梯费率计算手续费与实退金额
        BigDecimal feeRate = calcFeeRate(act.getId());
        BigDecimal refundAmount = order.getTotalAmount();
        BigDecimal feeAmount = refundAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = refundAmount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP);
        long daysLeft = java.time.Duration.between(LocalDateTime.now(), act.getStartTime()).toDays();

        result.put("refundable", true);
        result.put("refundPolicy", 1);
        result.put("feeRate", feeRate);
        result.put("feeAmount", feeAmount);
        result.put("actualAmount", actualAmount);
        result.put("daysLeft", daysLeft);
        result.put("feeTierDesc", buildFeeTierDesc(daysLeft));
        return result;
    }

    /**
     * 退票阶梯费率文案：与 calcFeeRate 的费率档位保持一致，用于退前透明告知用户
     */
    private String buildFeeTierDesc(long daysLeft) {
        if (daysLeft >= 30) {
            return "开场前 30 天以上退票，手续费 5%";
        }
        if (daysLeft >= 7) {
            return "开场前 7–30 天退票，手续费 10%";
        }
        return "开场前 7 天内退票，手续费 20%";
    }

    // ==================== 私有方法 ====================

    /**
     * 生成退票工单号：TKRF + 毫秒时间戳 + 5位单调序号。
     * 使用 AtomicLong 保证同毫秒内序号唯一，避免高并发下随机碰撞。
     */
    private String generateRefundNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        long seq = REFUND_NO_SEQ.incrementAndGet() % 100000;
        return "TKRF" + timestamp + String.format("%05d", seq);
    }

    /**
     * 计算手续费比例：暂定固定10%，可根据活动开始时间动态计算
     * 距离活动开始时间越近手续费越高
     */
    private BigDecimal calcFeeRate(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStartTime() == null) {
            return DEFAULT_FEE_RATE;
        }
        long days = java.time.Duration.between(LocalDateTime.now(), activity.getStartTime()).toDays();
        // 活动开始前7天内退票，手续费20%
        if (days < 7) {
            return new BigDecimal("0.20");
        }
        // 活动开始前30天内退票，手续费10%
        if (days < 30) {
            return new BigDecimal("0.10");
        }
        // 30天以上退票，手续费5%
        return new BigDecimal("0.05");
    }

    /**
     * 填充退票工单非数据库字段
     */
    private void fillRefundInfo(RefundOrder refundOrder) {
        if (refundOrder.getActivityId() != null) {
            Activity act = activityMapper.selectById(refundOrder.getActivityId());
            if (act != null) {
                refundOrder.setActivityTitle(act.getTitle());
            }
        }
        if (refundOrder.getUserId() != null) {
            User user = userMapper.selectById(refundOrder.getUserId());
            if (user != null) {
                refundOrder.setUsername(user.getUsername());
            }
        }
    }
}
