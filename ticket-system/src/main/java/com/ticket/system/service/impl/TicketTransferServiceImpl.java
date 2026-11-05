package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.TicketTransfer;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.TicketTransferMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.MessageService;
import com.ticket.system.service.TicketTransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 电子票转赠服务（借鉴大麦「转赠」能力）
 */
@Service
public class TicketTransferServiceImpl implements TicketTransferService {

    @Resource
    private TicketTransferMapper ticketTransferMapper;

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private MessageService messageService;

    @Override
    @Transactional
    public TicketTransfer initiate(Long userId, Long orderId, String recipientPhone, List<Long> ticketIds) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        if (!StringUtils.hasText(recipientPhone) || !recipientPhone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的接收方手机号");
        }
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或不属于当前用户");
        }
        // 仅已支付订单的电子票可转赠
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("仅已支付订单的电子票可转赠");
        }

        LambdaQueryWrapper<ElectronicTicket> w = new LambdaQueryWrapper<ElectronicTicket>()
                .eq(ElectronicTicket::getOrderId, orderId);
        if (ticketIds != null && !ticketIds.isEmpty()) {
            w.in(ElectronicTicket::getId, ticketIds);
        }
        List<ElectronicTicket> tickets = electronicTicketMapper.selectList(w);
        if (tickets.isEmpty()) {
            throw new BusinessException("未找到可转赠的电子票");
        }
        for (ElectronicTicket t : tickets) {
            if (t.getStatus() != null && t.getStatus() != 0) {
                throw new BusinessException("仅未核销的电子票可转赠");
            }
            if (t.getTransferStatus() != null && t.getTransferStatus() != 0) {
                throw new BusinessException("存在已在转赠中或已转出的电子票，无法重复转赠");
            }
        }

        String code = generateTransferCode();
        TicketTransfer transfer = new TicketTransfer();
        transfer.setOrderId(orderId);
        transfer.setActivityId(order.getActivityId());
        transfer.setSenderUserId(userId);
        transfer.setRecipientPhone(recipientPhone);
        transfer.setTransferCode(code);
        transfer.setStatus(0);
        // 只序列化电子票 ID（如 "100389,100390"）；此前误用 String::valueOf 会把整个
        // ElectronicTicket 对象的 toString 写进来，导致 ticket_ids(varchar 512) 超长直接 500
        transfer.setTicketIds(tickets.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(",")));
        transfer.setCreateTime(LocalDateTime.now());
        transfer.setUpdateTime(LocalDateTime.now());
        ticketTransferMapper.insert(transfer);

        // 标记电子票为「转出中」
        for (ElectronicTicket t : tickets) {
            t.setTransferStatus(1);
            t.setUpdateTime(LocalDateTime.now());
            electronicTicketMapper.updateById(t);
        }
        return transfer;
    }

    @Override
    @Transactional
    public TicketTransfer accept(Long userId, String transferCode) {
        if (!StringUtils.hasText(transferCode)) {
            throw new BusinessException("请输入转赠码");
        }
        transferCode = transferCode.trim();
        TicketTransfer transfer = ticketTransferMapper.selectOne(
                new LambdaQueryWrapper<TicketTransfer>().eq(TicketTransfer::getTransferCode, transferCode));
        if (transfer == null) {
            throw new BusinessException("转赠码无效");
        }
        if (transfer.getStatus() != 0) {
            throw new BusinessException("该转赠已处理，无法重复接收");
        }
        if (transfer.getSenderUserId().equals(userId)) {
            throw new BusinessException("不能接收自己转出的电子票");
        }
        // 接收方必须是转赠码绑定的手机号对应的注册用户
        User recipient = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, transfer.getRecipientPhone()));
        if (recipient == null) {
            throw new BusinessException("接收方手机号尚未注册，请先注册并登录后再接收");
        }
        if (!recipient.getId().equals(userId)) {
            throw new BusinessException("请使用接收方账号（手机号 " + transfer.getRecipientPhone()
                    + "）登录后接收本次转赠");
        }

        List<Long> ids = parseIds(transfer.getTicketIds());
        for (Long tid : ids) {
            ElectronicTicket t = electronicTicketMapper.selectById(tid);
            if (t == null) {
                continue;
            }
            t.setUserId(recipient.getId());
            t.setTransferStatus(2);
            t.setUpdateTime(LocalDateTime.now());
            electronicTicketMapper.updateById(t);
        }

        transfer.setRecipientUserId(recipient.getId());
        transfer.setStatus(1);
        transfer.setAcceptTime(LocalDateTime.now());
        transfer.setUpdateTime(LocalDateTime.now());
        ticketTransferMapper.updateById(transfer);

        Activity act = activityMapper.selectById(transfer.getActivityId());
        String title = act != null ? act.getTitle() : "";
        messageService.sendMessage(transfer.getSenderUserId(), "转赠成功",
                "您转赠的电子票已被接收", 3);
        messageService.sendMessage(recipient.getId(), "收到转赠",
                "您已收到【" + title + "】的电子票转赠，可在「我的电子票」中查看", 3);
        return transfer;
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long transferId) {
        if (transferId == null) {
            throw new BusinessException("转赠记录ID不能为空");
        }
        TicketTransfer transfer = ticketTransferMapper.selectById(transferId);
        if (transfer == null) {
            throw new BusinessException("转赠记录不存在");
        }
        if (!transfer.getSenderUserId().equals(userId)) {
            throw new BusinessException("只能取消自己发起的转赠");
        }
        if (transfer.getStatus() != 0) {
            throw new BusinessException("该转赠已处理，无法取消");
        }
        transfer.setStatus(2);
        transfer.setUpdateTime(LocalDateTime.now());
        ticketTransferMapper.updateById(transfer);

        for (Long tid : parseIds(transfer.getTicketIds())) {
            ElectronicTicket t = electronicTicketMapper.selectById(tid);
            if (t != null && t.getTransferStatus() != null && t.getTransferStatus() == 1) {
                t.setTransferStatus(0);
                t.setUpdateTime(LocalDateTime.now());
                electronicTicketMapper.updateById(t);
            }
        }
    }

    @Override
    public List<TicketTransfer> getMySent(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return ticketTransferMapper.selectList(
                new LambdaQueryWrapper<TicketTransfer>()
                        .eq(TicketTransfer::getSenderUserId, userId)
                        .orderByDesc(TicketTransfer::getCreateTime));
    }

    /** 兼容兜底：防止异常状态下转出中的票长期挂起（可由定时任务调用） */
    @Transactional
    public void expireOverdue() {
        try {
            LocalDateTime deadline = LocalDateTime.now().minusHours(24);
            List<TicketTransfer> overdue = ticketTransferMapper.selectList(
                    new LambdaQueryWrapper<TicketTransfer>()
                            .eq(TicketTransfer::getStatus, 0)
                            .lt(TicketTransfer::getCreateTime, deadline));
            for (TicketTransfer transfer : overdue) {
                transfer.setStatus(3);
                transfer.setUpdateTime(LocalDateTime.now());
                ticketTransferMapper.updateById(transfer);
                for (Long tid : parseIds(transfer.getTicketIds())) {
                    ElectronicTicket t = electronicTicketMapper.selectById(tid);
                    if (t != null && t.getTransferStatus() != null && t.getTransferStatus() == 1) {
                        t.setTransferStatus(0);
                        t.setUpdateTime(LocalDateTime.now());
                        electronicTicketMapper.updateById(t);
                    }
                }
            }
        } catch (Exception e) {
            // 兜底任务异常不影响主流程
        }
    }

    private List<Long> parseIds(String ids) {
        List<Long> result = new ArrayList<>();
        if (!StringUtils.hasText(ids)) {
            return result;
        }
        for (String s : ids.split(",")) {
            try {
                result.add(Long.valueOf(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法ID
            }
        }
        return result;
    }

    private String generateTransferCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
