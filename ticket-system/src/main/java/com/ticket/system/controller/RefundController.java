package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.RefundOrder;
import com.ticket.system.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 退票控制器
 */
@RestController
@RequestMapping("/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @Autowired
    private RequestContext requestContext;

    /**
     * 权限校验：管理员或运营
     */
    private void checkAdminOrOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无操作权限");
        }
    }

    // ==================== 用户接口 ====================

    /**
     * 申请退票
     */
    @PostMapping("/apply")
    public Result<RefundOrder> applyRefund(@RequestBody Map<String, Object> params) {
        Long userId = requestContext.getCurrentUserId();
        Long orderId = params.get("orderId") != null ? Long.valueOf(params.get("orderId").toString()) : null;
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        RefundOrder refundOrder = refundService.applyRefund(userId, orderId, reason);
        return Result.success(refundOrder);
    }

    /**
     * 我的退票记录
     */
    @GetMapping("/my-refunds")
    public Result<List<RefundOrder>> myRefunds() {
        Long userId = requestContext.getCurrentUserId();
        List<RefundOrder> list = refundService.getMyRefunds(userId);
        return Result.success(list);
    }

    /**
     * 退票费用预览（退前透明展示手续费与实退金额）
     */
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam Long orderId) {
        Long userId = requestContext.getCurrentUserId();
        return Result.success(refundService.previewRefund(userId, orderId));
    }

    // ==================== 管理接口 ====================

    /**
     * 退票工单分页
     */
    @GetMapping("/admin/page")
    public Result<IPage<RefundOrder>> refundPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        checkAdminOrOperator();
        // 运营仅看自己活动的退票工单；管理员传 null 看全部
        Long scopeOperatorId = requestContext.getCurrentRole() != null && requestContext.getCurrentRole() == 1
                ? requestContext.getCurrentUserId() : null;
        IPage<RefundOrder> result = refundService.getRefundPage(page, size, status, keyword, scopeOperatorId);
        return Result.success(result);
    }

    /**
     * 审核通过
     */
    @OperLog(module = "退票管理", operation = "审批退票")
    @PutMapping("/admin/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        checkAdminOrOperator();
        Long auditorId = requestContext.getCurrentUserId();
        String auditorName = requestContext.getCurrentUsername();
        String remark = params != null && params.get("remark") != null ? params.get("remark").toString() : null;
        refundService.approveRefund(auditorId, auditorName, id, remark);
        return Result.success();
    }

    /**
     * 审核驳回
     */
    @OperLog(module = "退票管理", operation = "驳回退票")
    @PutMapping("/admin/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        checkAdminOrOperator();
        Long auditorId = requestContext.getCurrentUserId();
        String auditorName = requestContext.getCurrentUsername();
        // 兼容 remark / rejectReason 两种字段名
        String remark = null;
        if (params != null) {
            Object remarkVal = params.get("remark") != null ? params.get("remark") : params.get("rejectReason");
            remark = remarkVal != null ? remarkVal.toString() : null;
        }
        refundService.rejectRefund(auditorId, auditorName, id, remark);
        return Result.success();
    }
}
