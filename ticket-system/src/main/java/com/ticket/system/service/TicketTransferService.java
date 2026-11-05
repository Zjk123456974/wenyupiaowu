package com.ticket.system.service;

import com.ticket.system.entity.TicketTransfer;

import java.util.List;

public interface TicketTransferService {

    /** 发起转赠：校验订单/票状态后生成转赠码并标记电子票为转出中 */
    TicketTransfer initiate(Long userId, Long orderId, String recipientPhone, List<Long> ticketIds);

    /** 接收转赠：凭转赠码领取，电子票归属转移给接收方 */
    TicketTransfer accept(Long userId, String transferCode);

    /** 发起人取消转赠（仅待接收状态可取消，回滚电子票状态） */
    void cancel(Long userId, Long transferId);

    /** 查询我发起的转赠记录（用于取消/查看状态） */
    List<TicketTransfer> getMySent(Long userId);
}
