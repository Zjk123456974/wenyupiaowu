package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.TicketCheckRecord;

import java.util.List;
import java.util.Map;

/**
 * 电子票核销与检票管理服务
 */
public interface TicketCheckService {

    /**
     * 订单支付成功后生成电子票
     *
     * @param orderId 订单ID
     */
    void generateTickets(Long orderId);

    /**
     * 扫码核销
     *
     * @param verifyCode       电子票核销验证码（或后端签名的时效令牌）
     * @param activityId       当前核销入口绑定的活动ID（用于防“A场次票核销进B场次”，可为空表示不限定）
     * @param checkUserId      核销人员ID
     * @param checkUsername    核销人员用户名
     * @param device           核销设备
     * @param ip               核销IP
     * @param scopeOperatorId  核销范围：null 表示不限（管理员）；非 null 表示仅可核销该运营负责的活动
     * @return 核销记录
     */
    TicketCheckRecord checkTicket(String verifyCode, Long activityId, Long checkUserId, String checkUsername,
                                  String device, String ip, Long scopeOperatorId);

    /**
     * 可核销的活动列表（核销端活动下拉）
     *
     * @param scopeOperatorId null 表示全部（管理员）；非 null 表示仅该运营负责的活动
     * @return 已发布活动列表
     */
    List<Activity> getCheckableActivities(Long scopeOperatorId);

    /**
     * 核销记录分页查询
     *
     * @param page            当前页
     * @param size            每页大小
     * @param activityId      活动ID（可选过滤条件）
     * @param scopeOperatorId 数据范围：null 不限；非 null 仅该运营负责的活动
     * @return 核销记录分页结果
     */
    IPage<TicketCheckRecord> getCheckRecords(Integer page, Integer size, Long activityId, Long scopeOperatorId);

    /**
     * 入场统计
     *
     * @param activityId      活动ID
     * @param scopeOperatorId 数据范围：null 不限；非 null 仅该运营负责的活动
     * @return 统计数据：总购票人数、已核销人数、未入场人数
     */
    Map<String, Object> getCheckStatistics(Long activityId, Long scopeOperatorId);

    /**
     * 获取用户电子票列表
     *
     * @param userId 用户ID
     * @return 电子票列表
     */
    List<ElectronicTicket> getMyTickets(Long userId);

    /**
     * 电子票详情
     *
     * @param userId   用户ID
     * @param ticketId 电子票ID
     * @return 电子票详情
     */
    ElectronicTicket getTicketDetail(Long userId, Long ticketId);
}
