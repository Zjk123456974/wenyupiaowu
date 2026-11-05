package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.RefundOrder;

import java.util.List;
import java.util.Map;

public interface RefundService {

    /**
     * 用户申请退票
     */
    RefundOrder applyRefund(Long userId, Long orderId, String reason);

    /**
     * 退票工单分页查询（管理端 / 运营端复用）
     * @param scopeOperatorId 非 null 时仅返回归属该运营的活动的退票工单
     */
    IPage<RefundOrder> getRefundPage(Integer page, Integer size, Integer status, String keyword, Long scopeOperatorId);

    /**
     * 审核通过
     */
    void approveRefund(Long auditorId, String auditorName, Long refundId, String remark);

    /**
     * 审核驳回
     */
    void rejectRefund(Long auditorId, String auditorName, Long refundId, String remark);

    /**
     * 执行退款（审核通过后调用）
     */
    void executeRefund(Long refundId);

    /**
     * 用户查看自己的退票记录
     */
    List<RefundOrder> getMyRefunds(Long userId);

    /**
     * 退票费用预览（退前透明展示手续费比例、手续费金额与实退金额，对标大麦“退票前明确告知”）
     */
    Map<String, Object> previewRefund(Long userId, Long orderId);
}
