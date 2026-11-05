package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.TicketCheckRecord;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import com.ticket.system.mapper.OrderAttendeeMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.TicketCheckRecordMapper;
import com.ticket.system.service.OrderService;
import com.ticket.system.service.TicketCheckService;
import com.ticket.system.service.impl.SeatService;
import com.ticket.system.util.QrSignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 电子票核销与检票管理服务实现
 */
@Slf4j
@Service
public class TicketCheckServiceImpl implements TicketCheckService {

    /** 电子票号序号发生器：同毫秒内单调递增，杜绝高并发下随机碰撞 */
    private static final AtomicLong TICKET_NO_SEQ = new AtomicLong(0);

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private TicketCheckRecordMapper ticketCheckRecordMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private OrderAttendeeMapper orderAttendeeMapper;

    @Resource
    private com.ticket.system.mapper.RefundOrderMapper refundOrderMapper;

    @Lazy
    @Resource
    private OrderService orderService;

    @Resource
    private QrSignUtil qrSignUtil;

    @Resource
    private SeatService seatService;

    /**
     * 订单支付成功后生成电子票
     * 根据订单的购票数量生成对应数量的电子票，每张票生成唯一的 ticket_no 和 verify_code
     */
    @Override
    @Transactional
    public void generateTickets(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 仅已支付订单生成电子票
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("订单未支付，无法生成电子票");
        }

        // 幂等校验：若该订单已生成过电子票则跳过，避免重复生成
        Long existCount = electronicTicketMapper.selectCount(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getOrderId, orderId));
        if (existCount != null && existCount > 0) {
            log.info("订单{}已生成过电子票，跳过重复生成", order.getOrderNo());
            return;
        }

        // 观演人来源优先 order_attendee 子表（一票一证）；历史订单（子表无数据）回退 orders 冗余列
        List<OrderAttendee> attendees = orderAttendeeMapper.selectList(
                new LambdaQueryWrapper<OrderAttendee>()
                        .eq(OrderAttendee::getOrderId, orderId)
                        .orderByAsc(OrderAttendee::getId));
        if (attendees.isEmpty() && StringUtils.hasText(order.getAttendeeIdCard())) {
            OrderAttendee legacy = new OrderAttendee();
            legacy.setAttendeeName(order.getAttendeeName());
            legacy.setAttendeeIdCard(order.getAttendeeIdCard());
            attendees.add(legacy);
        }

        // 实名模式与场馆地图开关：决定每张票如何绑定观演人、是否生成座位定位
        Activity activity = activityMapper.selectById(order.getActivityId());
        int realNameMode = activity != null && activity.getRealNameMode() != null ? activity.getRealNameMode() : 0;
        boolean seatMap = activity != null && activity.getSeatMapEnabled() != null && activity.getSeatMapEnabled() == 1;
        String seatTypeForSeat = StringUtils.hasText(order.getSeatType()) ? order.getSeatType() : "普通区";

        Integer quantity = order.getQuantity() != null ? order.getQuantity() : 1;
        // 选座场次：解析订单里用户所选座位编码（仅 seatMapEnabled=1 且订单携带 seats 时生效）
        List<String> seatCodes = com.ticket.system.util.SeatUtil.parseSeats(order.getSeats());
        boolean seatSelected = seatMap && seatCodes != null && !seatCodes.isEmpty();
        for (int i = 0; i < quantity; i++) {
            ElectronicTicket ticket = new ElectronicTicket();
            ticket.setTicketNo(generateTicketNo());
            ticket.setOrderId(order.getId());
            ticket.setOrderNo(order.getOrderNo());
            ticket.setUserId(order.getUserId());
            ticket.setActivityId(order.getActivityId());
            ticket.setTicketPriceId(order.getTicketPriceId());
            ticket.setSeatType(order.getSeatType());
            // 观演人绑定：
            // 一票一实名(2)：每张票按位绑定一位观演人，杜绝“一人多票共用同一实名信息”
            // 一单一实名(1)：所有票共用购票人这 1 个实名
            // 免实名(0)：无实名，观演人留空（若填了观演人也按 1 就 1、多就按位绑定）
            if (realNameMode == 1 && !attendees.isEmpty()) {
                ticket.setAttendeeName(attendees.get(0).getAttendeeName());
                ticket.setAttendeeIdCard(attendees.get(0).getAttendeeIdCard());
            } else if (i < attendees.size() && attendees.get(i) != null) {
                ticket.setAttendeeName(attendees.get(i).getAttendeeName());
                ticket.setAttendeeIdCard(attendees.get(i).getAttendeeIdCard());
            }
            // 座位分配：优先使用用户所选座位（选座场次），否则场馆地图开启时按序分配区-排-座
            if (seatSelected && i < seatCodes.size()) {
                String code = seatCodes.get(i);
                ticket.setSeatCode(code);
                ticket.setSeatInfo(seatService.seatCodeToInfo(order.getActivityId(), code));
            } else if (seatMap) {
                int row = (i / 10) + 1;
                int seatNo = (i % 10) + 1;
                ticket.setSeatInfo(seatTypeForSeat + "-" + row + "排-" + seatNo + "座");
            }
            ticket.setVerifyCode(generateVerifyCode());
            ticket.setStatus(0); // 未核销
            ticket.setCreateTime(LocalDateTime.now());
            ticket.setUpdateTime(LocalDateTime.now());
            electronicTicketMapper.insert(ticket);
        }

        // 座位占用以 electronic_ticket.seat_code 为权威来源（SeatService.computeOccupied 实时计算），
        // 不再写 Redis seat:occupied 集合（此前为无读取方的死代码）

        log.info("订单{}生成电子票成功，数量：{}，实名观演人：{}，实名模式：{}，场馆地图：{}，选座：{}",
                order.getOrderNo(), quantity, attendees.size(), realNameMode, seatMap, seatSelected);
    }

    /**
     * 扫码核销
     */
    @Override
    @Transactional
    public TicketCheckRecord checkTicket(String verifyCode, Long targetActivityId, Long checkUserId, String checkUsername,
                                         String device, String ip, Long scopeOperatorId) {
        if (!StringUtils.hasText(verifyCode)) {
            throw new BusinessException("核销验证码不能为空");
        }

        // 支持后端签名的时效令牌：含 "." 视为令牌，先验签 + 验时效，再取出真实核销码
        String realCode = verifyCode;
        if (verifyCode.contains(".")) {
            if (!qrSignUtil.verify(verifyCode)) {
                throw new BusinessException("核销二维码无效或已过期，请让用户刷新二维码后重试");
            }
            realCode = qrSignUtil.extractVerifyCode(verifyCode);
            if (!StringUtils.hasText(realCode)) {
                throw new BusinessException("核销二维码解析失败，请重新出示");
            }
        }

        // 根据验证码查询电子票
        ElectronicTicket ticket = electronicTicketMapper.selectOne(
                new LambdaQueryWrapper<ElectronicTicket>()
                        .eq(ElectronicTicket::getVerifyCode, realCode));
        if (ticket == null) {
            throw new BusinessException("电子票不存在或验证码无效");
        }

        // 场次错检防护：核销入口绑定了活动时，票必须属于该活动，防止 A 场次的票核销进 B 场次
        if (targetActivityId != null && ticket.getActivityId() != null
                && !targetActivityId.equals(ticket.getActivityId())) {
            throw new BusinessException("该电子票不属于当前核验的活动场次，请核对票面与场次");
        }

        // 活动运营权限隔离：只能核销自己负责的活动（管理员 scopeOperatorId 为 null，不受限）
        if (scopeOperatorId != null) {
            Activity ownActivity = activityMapper.selectById(ticket.getActivityId());
            if (ownActivity == null) {
                throw new BusinessException("该电子票关联的活动不存在，无法核销");
            }
            if (!scopeOperatorId.equals(ownActivity.getOperatorId())) {
                throw new BusinessException(403, "该电子票所属活动不由你负责，无法核销");
            }
        }

        // 校验票状态
        Integer status = ticket.getStatus();
        if (status == null) {
            throw new BusinessException("电子票状态异常");
        }
        if (status == 1) {
            throw new BusinessException("该电子票已核销，请勿重复核销");
        }
        if (status == 2) {
            throw new BusinessException("该电子票已作废，无法核销");
        }
        if (status != 0) {
            throw new BusinessException("电子票状态异常，无法核销");
        }

        // 原子条件更新（仅当仍为“未核销”才置为“已核销”）：
        // 从“先读后写”改为“条件更新”，杜绝多检票口/同码多设备并发核销同一张票
        LocalDateTime checkTime = LocalDateTime.now();
        int updated = electronicTicketMapper.update(null, new LambdaUpdateWrapper<ElectronicTicket>()
                .eq(ElectronicTicket::getId, ticket.getId())
                .eq(ElectronicTicket::getStatus, 0)
                .set(ElectronicTicket::getStatus, 1)
                .set(ElectronicTicket::getCheckTime, checkTime)
                .set(ElectronicTicket::getUpdateTime, checkTime));
        if (updated == 0) {
            // 并发下状态已被其他请求改变，重新读取给出精确原因
            ElectronicTicket fresh = electronicTicketMapper.selectById(ticket.getId());
            Integer freshStatus = fresh != null ? fresh.getStatus() : null;
            if (freshStatus != null && freshStatus == 1) {
                throw new BusinessException("该电子票已核销，请勿重复核销");
            }
            if (freshStatus != null && freshStatus == 2) {
                throw new BusinessException("该电子票已作废，无法核销");
            }
            throw new BusinessException("电子票状态异常，无法核销");
        }

        // 查询活动标题
        Activity activity = activityMapper.selectById(ticket.getActivityId());
        String activityTitle = activity != null ? activity.getTitle() : "";

        // 插入核销记录
        TicketCheckRecord record = new TicketCheckRecord();
        record.setTicketId(ticket.getId());
        record.setTicketNo(ticket.getTicketNo());
        record.setActivityId(ticket.getActivityId());
        record.setActivityTitle(activityTitle);
        record.setAttendeeName(ticket.getAttendeeName());
        record.setCheckUserId(checkUserId);
        record.setCheckUsername(checkUsername);
        record.setCheckDevice(device);
        record.setCheckIp(ip);
        record.setCheckTime(checkTime);
        record.setCreateTime(checkTime);
        ticketCheckRecordMapper.insert(record);

        log.info("电子票核销成功，ticketNo={}，核销人={}，活动={}",
                ticket.getTicketNo(), checkUsername, activityTitle);
        return record;
    }

    /**
     * 可核销的活动列表（核销端活动下拉）
     */
    @Override
    public List<Activity> getCheckableActivities(Long scopeOperatorId) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 3)
                .orderByDesc(Activity::getCreateTime);
        if (scopeOperatorId != null) {
            // 活动运营：仅本人负责的活动
            wrapper.eq(Activity::getOperatorId, scopeOperatorId);
        }
        return activityMapper.selectList(wrapper);
    }

    /**
     * 查询某运营负责的全部活动ID
     */
    private List<Long> scopedActivityIds(Long scopeOperatorId) {
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getOperatorId, scopeOperatorId));
        List<Long> ids = new ArrayList<>();
        for (Activity activity : activities) {
            if (activity.getId() != null) {
                ids.add(activity.getId());
            }
        }
        return ids;
    }

    /**
     * 核销记录分页查询
     */
    @Override
    public IPage<TicketCheckRecord> getCheckRecords(Integer page, Integer size, Long activityId, Long scopeOperatorId) {
        Page<TicketCheckRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TicketCheckRecord> wrapper = new LambdaQueryWrapper<>();

        List<Long> scopedIds = scopeOperatorId != null ? scopedActivityIds(scopeOperatorId) : null;
        if (scopedIds != null) {
            if (activityId != null) {
                if (!scopedIds.contains(activityId)) {
                    throw new BusinessException(403, "无权查看该活动的核销记录");
                }
                wrapper.eq(TicketCheckRecord::getActivityId, activityId);
            } else if (scopedIds.isEmpty()) {
                // 该运营暂无负责的活动，返回空页而非全量数据
                pageParam.setRecords(new ArrayList<>());
                pageParam.setTotal(0);
                return pageParam;
            } else {
                wrapper.in(TicketCheckRecord::getActivityId, scopedIds);
            }
        } else if (activityId != null) {
            wrapper.eq(TicketCheckRecord::getActivityId, activityId);
        }

        wrapper.orderByDesc(TicketCheckRecord::getCheckTime);
        return ticketCheckRecordMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 入场统计
     */
    @Override
    public Map<String, Object> getCheckStatistics(Long activityId, Long scopeOperatorId) {
        Map<String, Object> statistics = new HashMap<>();

        List<Long> scopedIds = scopeOperatorId != null ? scopedActivityIds(scopeOperatorId) : null;
        boolean emptyScope = false;
        if (scopedIds != null) {
            if (activityId != null) {
                if (!scopedIds.contains(activityId)) {
                    throw new BusinessException(403, "无权查看该活动的入场统计");
                }
            } else if (scopedIds.isEmpty()) {
                emptyScope = true;
            }
        }

        long total = 0L;
        long checked = 0L;
        long unchecked = 0L;
        if (!emptyScope) {
            // 总购票人数（电子票总数，排除已作废）
            Long totalCount = electronicTicketMapper.selectCount(
                    new LambdaQueryWrapper<ElectronicTicket>()
                            .eq(activityId != null, ElectronicTicket::getActivityId, activityId)
                            .in(activityId == null && scopedIds != null, ElectronicTicket::getActivityId, scopedIds)
                            .ne(ElectronicTicket::getStatus, 2));
            // 已核销人数
            Long checkedCount = electronicTicketMapper.selectCount(
                    new LambdaQueryWrapper<ElectronicTicket>()
                            .eq(activityId != null, ElectronicTicket::getActivityId, activityId)
                            .in(activityId == null && scopedIds != null, ElectronicTicket::getActivityId, scopedIds)
                            .eq(ElectronicTicket::getStatus, 1));
            // 未入场人数（未核销）
            Long uncheckedCount = electronicTicketMapper.selectCount(
                    new LambdaQueryWrapper<ElectronicTicket>()
                            .eq(activityId != null, ElectronicTicket::getActivityId, activityId)
                            .in(activityId == null && scopedIds != null, ElectronicTicket::getActivityId, scopedIds)
                            .eq(ElectronicTicket::getStatus, 0));

            total = totalCount != null ? totalCount : 0L;
            checked = checkedCount != null ? checkedCount : 0L;
            unchecked = uncheckedCount != null ? uncheckedCount : 0L;
        }

        statistics.put("totalTickets", total);
        statistics.put("checkedTickets", checked);
        statistics.put("uncheckedTickets", unchecked);

        return statistics;
    }

    /**
     * 获取用户电子票列表
     */
    @Override
    public List<ElectronicTicket> getMyTickets(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        List<ElectronicTicket> tickets = electronicTicketMapper.selectList(
                new LambdaQueryWrapper<ElectronicTicket>()
                        // 本人持有的票，或本人作为订单主但票尚未转出(transfer_status!=2)的票（转赠出去后原持有者不可再用）
                        .and(w -> w.eq(ElectronicTicket::getUserId, userId)
                                .or(ww -> ww.inSql(ElectronicTicket::getOrderId,
                                                "SELECT id FROM orders WHERE user_id = " + userId)
                                            .ne(ElectronicTicket::getTransferStatus, 2)))
                        .orderByDesc(ElectronicTicket::getCreateTime));

        // 填充活动标题
        List<ElectronicTicket> result = new ArrayList<>();
        for (ElectronicTicket ticket : tickets) {
            fillTicketInfo(ticket);
            result.add(ticket);
        }
        return result;
    }

    /**
     * 电子票详情
     */
    @Override
    public ElectronicTicket getTicketDetail(Long userId, Long ticketId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (ticketId == null) {
            throw new BusinessException("电子票ID不能为空");
        }

        ElectronicTicket ticket = electronicTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException("电子票不存在");
        }
        // 票持有者可见；订单主在票未被转出(transfer_status!=2)时也可查看（转赠出去后原持有者不可再用）
        boolean owner = userId.equals(ticket.getUserId());
        boolean orderOwner = false;
        if (!owner) {
            Orders order = ordersMapper.selectById(ticket.getOrderId());
            orderOwner = order != null && userId.equals(order.getUserId())
                    && (ticket.getTransferStatus() == null || ticket.getTransferStatus() != 2);
        }
        if (!owner && !orderOwner) {
            throw new BusinessException(403, "无权查看该电子票");
        }
        fillTicketInfo(ticket);
        return ticket;
    }

    /**
     * 生成电子票号：ET + 毫秒时间戳 + 单调序号。
     * 毫秒级时间戳 + 进程内单调递增序号，避免高并发同一毫秒内随机数碰撞导致唯一键冲突。
     */
    private String generateTicketNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        long seq = TICKET_NO_SEQ.incrementAndGet() % 100000;
        return "ET" + timestamp + String.format("%05d", seq);
    }

    /**
     * 生成核销验证码：UUID（去除连字符）
     */
    private String generateVerifyCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 填充电子票非数据库字段（活动标题、场馆、演出时间，以及核销二维码时效令牌）
     */
    private void fillTicketInfo(ElectronicTicket ticket) {
        // 生成带时效签名的核销二维码令牌，供用户端渲染真实可扫码图片
        if (ticket.getVerifyCode() != null && ticket.getTicketNo() != null) {
            ticket.setQrToken(qrSignUtil.sign(ticket.getTicketNo(), ticket.getVerifyCode()));
            ticket.setQrExpireAt(System.currentTimeMillis() + qrSignUtil.getRemainingMillis(ticket.getQrToken()));
        }
        if (ticket.getActivityId() != null) {
            Activity activity = activityMapper.selectById(ticket.getActivityId());
            if (activity != null) {
                ticket.setActivityTitle(activity.getTitle());
                ticket.setVenue(activity.getVenue());
                ticket.setStartTime(activity.getStartTime());
            }
        }
        // 关联订单/退票工单状态，供票夹联动展示“退票处理中/已退款”等信息
        if (ticket.getOrderId() != null) {
            Orders order = ordersMapper.selectById(ticket.getOrderId());
            if (order != null) {
                ticket.setOrderStatus(order.getStatus());
            }
            List<com.ticket.system.entity.RefundOrder> refunds = refundOrderMapper.selectList(
                    new LambdaQueryWrapper<com.ticket.system.entity.RefundOrder>()
                            .eq(com.ticket.system.entity.RefundOrder::getOrderId, ticket.getOrderId())
                            .orderByDesc(com.ticket.system.entity.RefundOrder::getId));
            if (!refunds.isEmpty()) {
                ticket.setRefundStatus(refunds.get(0).getStatus());
            }
        }
    }
}
