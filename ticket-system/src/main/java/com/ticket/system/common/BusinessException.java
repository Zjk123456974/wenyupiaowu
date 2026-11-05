package com.ticket.system.common;

public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        // 业务异常（如“订单不存在”“库存不足”）属于客户端可感知的失败，用 400 而非 500，
        // 避免前端与监控把正常的业务拒绝当成服务器故障
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
