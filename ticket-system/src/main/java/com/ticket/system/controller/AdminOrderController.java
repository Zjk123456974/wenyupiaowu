package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.OrderAttendee;
import com.ticket.system.entity.Orders;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import com.ticket.system.mapper.OrderAttendeeMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private OrderAttendeeMapper orderAttendeeMapper;

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private RequestContext requestContext;

    /**
     * 管理员或运营均可访问；运营仅能查看/处理归属自己活动的订单（由 scopeOperatorId 控制）
     */
    private void checkAdminOrOperator() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || (role != 1 && role != 2)) {
            throw new BusinessException(403, "无操作权限");
        }
    }

    /**
     * 订单的编辑/删除等强一致操作仅管理员可执行（运营不开放，避免误改库存/票/流水）
     */
    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 当前用户若是运营（role=1），必须只能处理归属自己活动的订单，否则越权
     */
    private void assertOrderScope(Long orderId) {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 1) {
            return; // 管理员不受限
        }
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        Activity activity = activityMapper.selectById(order.getActivityId());
        Long currentUserId = requestContext.getCurrentUserId();
        if (activity == null || !currentUserId.equals(activity.getOperatorId())) {
            throw new BusinessException(403, "无权处理非本人负责活动的订单");
        }
    }

    /**
     * 所有订单分页（管理员看全部，运营只看自己活动）
     */
    @GetMapping("/page")
    public Result<IPage<Orders>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        checkAdminOrOperator();
        // 运营按活动归属过滤；管理员传 null 表示全部
        Long scopeOperatorId = requestContext.getCurrentRole() != null && requestContext.getCurrentRole() == 1
                ? requestContext.getCurrentUserId() : null;
        IPage<Orders> result = orderService.getOrderPage(page, size, activityId, status, keyword, startTime, endTime,
                scopeOperatorId);
        return Result.success(result);
    }

    /**
     * 处理异常订单：支持具体处理方式（1恢复订单/2强制取消/3标记已解决）+ 处理备注
     */
    @OperLog(module = "订单管理", operation = "处理异常订单")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        checkAdminOrOperator();
        assertOrderScope(id);
        Integer handleType = params != null && params.get("handleType") != null
                ? Integer.parseInt(params.get("handleType").toString()) : null;
        String remark = params != null && params.get("remark") != null
                ? params.get("remark").toString() : null;
        Long operatorId = requestContext.getCurrentUserId();
        String operatorName = requestContext.getCurrentUsername();
        orderService.handleAbnormalOrder(id, handleType, remark, operatorId, operatorName);
        return Result.success("处理完成", null);
    }

    /**
     * 订单异常处理记录
     */
    @GetMapping("/{id}/handle-records")
    public Result<?> handleRecords(@PathVariable Long id) {
        checkAdminOrOperator();
        return Result.success(orderService.getHandleRecords(id));
    }

    /**
     * 更新订单信息
     */
    @OperLog(module = "订单管理", operation = "编辑订单")
    @PutMapping("/{id}")
    public Result<Orders> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 订单数量与库存、电子票张数强绑定，直接修改会导致数据不一致，一律禁止，请走“取消→重购”
        if (params.get("quantity") != null) {
            int quantity = Integer.parseInt(params.get("quantity").toString());
            if (quantity != order.getQuantity()) {
                throw new BusinessException("不允许直接修改订单数量，请取消原订单后重新购买，以保证库存一致");
            }
        }

        // 订单状态是复杂状态机的入口，直接改会造成库存/票/流水不一致，仅允许终态纠偏（取消/过期/退款之间的修正）
        if (params.get("status") != null) {
            int status = Integer.parseInt(params.get("status").toString());
            int oldStatus = order.getStatus();
            boolean oldTerminal = oldStatus == 2 || oldStatus == 3 || oldStatus == 5;
            boolean newTerminal = status == 2 || status == 3 || status == 5;
            if (!oldTerminal || !newTerminal) {
                throw new BusinessException("待支付/已支付订单的状态请使用取消、超时、退款等业务流程处理，禁止直接修改");
            }
            order.setStatus(status);
        }

        ordersMapper.updateById(order);
        return Result.success(order);
    }

    /**
     * 删除订单：仅允许删除已终态（取消/过期/退款）订单，并同步清理关联的观演人/电子票数据，
     * 避免删除已支付订单造成“票还在但订单没了”或库存无法释放的问题。
     */
    @OperLog(module = "订单管理", operation = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3 && order.getStatus() != 5) {
            throw new BusinessException("仅已取消/已过期/已退款的订单可删除，其他状态请先走取消/退款流程释放库存");
        }
        ordersMapper.deleteById(id);
        orderAttendeeMapper.delete(new LambdaQueryWrapper<OrderAttendee>()
                .eq(OrderAttendee::getOrderId, id));
        electronicTicketMapper.delete(new LambdaQueryWrapper<ElectronicTicket>()
                .eq(ElectronicTicket::getOrderId, id));
        return Result.success();
    }
}
