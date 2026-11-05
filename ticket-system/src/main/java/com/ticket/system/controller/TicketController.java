package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.dto.GrabTicketRequest;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;
import com.ticket.system.service.TicketService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private RequestContext requestContext;

    @Resource
    private TicketService ticketService;

    /**
     * 抢票
     * <p>新版本建议直接提交 JSON body（支持一单多观演人）：
     * <pre>{ "activityId": 1, "quantity": 2, "ticketPriceId": 3,
     *   "attendees": [{ "attendeeName": "张三", "attendeeIdCard": "..." }, ...] }</pre>
     * 兼容旧版 query 参数提交（仅单观演人）：body 为空时读取 activityId/quantity/ticketPriceId/attendeeName/attendeeIdCard。
     */
    @PostMapping("/grab")
    public Result<Orders> grab(@RequestBody(required = false) GrabTicketRequest body,
                               @RequestParam(required = false) Long activityId,
                               @RequestParam(required = false) Integer quantity,
                               @RequestParam(required = false) Long ticketPriceId,
                               @RequestParam(required = false) String attendeeName,
                               @RequestParam(required = false) String attendeeIdCard) {
        Long userId = requestContext.getCurrentUserId();
        Long actId;
        Integer qty;
        Long tpId;
        List<OrderAttendee> attendees = new ArrayList<>();

        if (body != null && body.getActivityId() != null) {
            // 新协议：JSON body，观演人以数组形式提交
            actId = body.getActivityId();
            qty = body.getQuantity();
            tpId = body.getTicketPriceId();
            if (body.getAttendees() != null) {
                for (GrabTicketRequest.AttendeeItem item : body.getAttendees()) {
                    if (item == null) {
                        continue;
                    }
                    boolean hasName = StringUtils.hasText(item.getAttendeeName());
                    boolean hasIdCard = StringUtils.hasText(item.getAttendeeIdCard());
                    if (!hasName && !hasIdCard) {
                        continue;
                    }
                    if (!hasName || !hasIdCard) {
                        throw new BusinessException("观演人姓名与身份证号需成对填写");
                    }
                    OrderAttendee a = new OrderAttendee();
                    a.setAttendeeName(item.getAttendeeName().trim());
                    a.setAttendeeIdCard(item.getAttendeeIdCard().trim());
                    attendees.add(a);
                }
            }
        } else {
            // 旧协议：query 参数单观演人
            actId = activityId;
            qty = quantity;
            tpId = ticketPriceId;
            if (StringUtils.hasText(attendeeName) || StringUtils.hasText(attendeeIdCard)) {
                if (!StringUtils.hasText(attendeeName) || !StringUtils.hasText(attendeeIdCard)) {
                    throw new BusinessException("观演人姓名与身份证号需同时填写");
                }
                OrderAttendee a = new OrderAttendee();
                a.setAttendeeName(attendeeName.trim());
                a.setAttendeeIdCard(attendeeIdCard.trim());
                attendees.add(a);
            }
        }

        if (actId == null || qty == null || qty <= 0) {
            throw new BusinessException("活动与购票数量不能为空");
        }

        Orders order = ticketService.grabTicket(userId, actId, qty, tpId, attendees, body.getSeats());
        return Result.success("抢票成功", order);
    }
}
