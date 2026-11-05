package com.ticket.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 抢票请求 DTO（支持一单多观演人，一票一证）
 */
@Data
public class GrabTicketRequest {

    /** 活动ID */
    private Long activityId;

    /** 购票数量 */
    private Integer quantity;

    /** 票档ID（可空，使用活动统一票价） */
    private Long ticketPriceId;

    /** 实名观演人列表；为空表示匿名购票，非空时数量必须等于 quantity */
    private List<AttendeeItem> attendees;

    /** 选座座位编码列表（仅选座场次使用，如 ["A-1-2","A-1-3"]），数量必须等于 quantity */
    private List<String> seats;

    /**
     * 观演人（一张票对应一位观演人）
     */
    @Data
    public static class AttendeeItem {
        private String attendeeName;
        private String attendeeIdCard;
    }
}
