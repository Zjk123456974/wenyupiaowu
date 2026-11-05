package com.ticket.system.service;

import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;

import java.util.List;

public interface TicketService {

    /**
     * 限时抢票（核心高并发方法，匿名购票）
     */
    Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId);

    /**
     * 限时抢票（兼容旧版单实名观演人，quantity 为 1 或与观演人数量一致）
     */
    Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                      String attendeeName, String attendeeIdCard);

    /**
     * 限时抢票（支持一单多观演人，一票一证）
     *
     * @param attendees 观演人列表，为空表示匿名购票；非空时数量必须等于 quantity
     */
    Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                      List<OrderAttendee> attendees);

    /**
     * 限时抢票（支持选座场次：传入已选座位编码列表，数量必须等于 quantity）
     *
     * @param attendees 观演人列表
     * @param seats     选座座位编码；选座场次必填，非选座场次传 null
     */
    Orders grabTicket(Long userId, Long activityId, Integer quantity, Long ticketPriceId,
                      List<OrderAttendee> attendees, List<String> seats);

    /**
     * 获取用户某活动已购数量
     */
    Integer getUserTicketCount(Long userId, Long activityId);
}
