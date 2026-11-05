package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.TicketTransfer;
import com.ticket.system.service.TicketTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 电子票转赠接口（借鉴大麦「转赠」能力）
 * 用户端：发起转赠 / 接收转赠 / 取消转赠 / 查询我发起的转赠
 */
@RestController
@RequestMapping("/ticket/transfer")
public class TicketTransferController {

    @Autowired
    private TicketTransferService ticketTransferService;

    @Autowired
    private RequestContext requestContext;

    private Long currentUserId() {
        Long uid = requestContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(401, "未登录");
        }
        return uid;
    }

    /** 发起转赠 */
    @PostMapping("/initiate")
    public Result<TicketTransfer> initiate(@RequestBody Map<String, Object> params) {
        Long uid = currentUserId();
        Long orderId = params.get("orderId") != null ? Long.valueOf(params.get("orderId").toString()) : null;
        String recipientPhone = params.get("recipientPhone") != null ? params.get("recipientPhone").toString() : null;
        List<Long> ticketIds = parseTicketIds(params.get("ticketIds"));
        TicketTransfer transfer = ticketTransferService.initiate(uid, orderId, recipientPhone, ticketIds);
        return Result.success("转赠发起成功，请将转赠码发送给接收方", transfer);
    }

    /** 接收转赠 */
    @PostMapping("/accept")
    public Result<TicketTransfer> accept(@RequestBody Map<String, Object> params) {
        Long uid = currentUserId();
        String transferCode = params.get("transferCode") != null ? params.get("transferCode").toString() : null;
        TicketTransfer transfer = ticketTransferService.accept(uid, transferCode);
        return Result.success("转赠接收成功", transfer);
    }

    /** 取消转赠（发起人） */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody Map<String, Object> params) {
        Long uid = currentUserId();
        Long transferId = params.get("transferId") != null ? Long.valueOf(params.get("transferId").toString()) : null;
        ticketTransferService.cancel(uid, transferId);
        return Result.success();
    }

    /** 查询我发起的转赠 */
    @GetMapping("/my-sent")
    public Result<List<TicketTransfer>> mySent() {
        Long uid = currentUserId();
        return Result.success(ticketTransferService.getMySent(uid));
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseTicketIds(Object obj) {
        List<Long> ids = new ArrayList<>();
        if (obj == null) {
            return ids;
        }
        if (obj instanceof List) {
            for (Object o : (List<?>) obj) {
                if (o != null) {
                    try {
                        ids.add(Long.valueOf(o.toString()));
                    } catch (NumberFormatException ignored) {
                        // 忽略非法ID
                    }
                }
            }
        } else if (obj instanceof String) {
            for (String s : obj.toString().split(",")) {
                try {
                    ids.add(Long.valueOf(s.trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略非法ID
                }
            }
        }
        return ids;
    }
}
