package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;

import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;

public interface OrderService {

    /**
     * 创建订单
     */
    Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId);

    /**
     * 创建订单（含实名观演人信息）
     */
    Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId,
                       String attendeeName, String attendeeIdCard);

    /**
     * 创建订单（支持多个实名观演人，一票一证：每张票绑定一位观演人）
     *
     * @param attendeeList 实名观演人列表；为空/全空表示整单匿名购票；
     *                     非空时观演人数量必须等于 quantity，同一场活动同一身份证不可重复购买
     */
    Orders createOrder(Long userId, Long activityId, Integer quantity, Integer source, Long ticketPriceId,
                       List<OrderAttendee> attendeeList);

    /**
     * 我的订单列表
     */
    IPage<Orders> getMyOrders(Long userId, Integer page, Integer size, Integer status);

    /**
     * 订单详情
     */
    Orders getOrderDetail(Long userId, Long orderId);

    /**
     * 支付订单（模拟支付）
     */
    void payOrder(Long userId, Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 超时自动处理
     */
    void expireOrder(Long orderId);

    /**
     * 订单分页查询
     * @param scopeOperatorId 当不为 null 时，仅返回归属该运营（operator_id）活动的订单；
     *                       管理员传 null 表示查看全部（运营端订单管理复用此接口）
     */
    IPage<Orders> getOrderPage(Integer page, Integer size, Long activityId,
                               Integer status, String keyword, LocalDateTime startTime, LocalDateTime endTime,
                               Long scopeOperatorId);

    /**
     * 处理异常订单
     *
     * @param handleType  处理方式：1恢复订单 / 2强制取消 / 3标记已解决
     * @param remark      处理备注
     * @param operatorId  处理人ID
     * @param operatorName 处理人姓名
     */
    void handleAbnormalOrder(Long orderId, Integer handleType, String remark,
                             Long operatorId, String operatorName);

    /**
     * 查询订单异常处理记录
     */
    List<com.ticket.system.entity.OrderHandleRecord> getHandleRecords(Long orderId);

    /**
     * 申请退款
     */
    void refundOrder(Long userId, Long orderId);

    /**
     * 用户删除订单
     */
    void deleteOrder(Long userId, Long orderId);

    Map<String, Object> getElectronicTicket(Long userId, Long orderId);
}
