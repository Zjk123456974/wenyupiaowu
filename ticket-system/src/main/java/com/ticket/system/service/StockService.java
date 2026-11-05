package com.ticket.system.service;

public interface StockService {

    /**
     * 初始化Redis库存
     */
    void initStock(Long activityId, Integer totalTickets);

    /**
     * 获取当前库存
     */
    Integer getStock(Long activityId);

    /**
     * 扣减库存（原子操作，Lua脚本保证防超卖）
     * @return 扣减后库存，-1表示库存不足
     */
    Long decreaseStock(Long activityId, Integer quantity);

    /**
     * 增加库存（退票/取消）
     */
    Long increaseStock(Long activityId, Integer quantity);
}
