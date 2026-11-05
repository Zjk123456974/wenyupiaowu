package com.ticket.system.service.impl;

import com.ticket.system.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * Lua脚本：原子性扣减库存
     * 先判断库存是否充足，充足则扣减返回剩余库存，不足返回-1
     */
    private static final String DECREASE_STOCK_LUA =
            "local stock = tonumber(redis.call('GET', KEYS[1])) " +
            "if stock == nil then return -2 end " +
            "if stock >= tonumber(ARGV[1]) then " +
            "  redis.call('DECRBY', KEYS[1], ARGV[1]) " +
            "  return tonumber(redis.call('GET', KEYS[1])) " +
            "else " +
            "  return -1 " +
            "end";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void initStock(Long activityId, Integer totalTickets) {
        String key = STOCK_KEY_PREFIX + activityId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(totalTickets));
        log.info("初始化活动{}的Redis库存: {}", activityId, totalTickets);
    }

    @Override
    public Integer getStock(Long activityId) {
        String key = STOCK_KEY_PREFIX + activityId;
        String stock = stringRedisTemplate.opsForValue().get(key);
        return stock != null ? Integer.parseInt(stock) : null;
    }

    @Override
    public Long decreaseStock(Long activityId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + activityId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREASE_STOCK_LUA, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(quantity));
        log.info("活动{}扣减库存{}，结果: {}", activityId, quantity, result);
        return result;
    }

    @Override
    public Long increaseStock(Long activityId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + activityId;
        Long result = stringRedisTemplate.opsForValue().increment(key, quantity);
        log.info("活动{}增加库存{}，当前库存: {}", activityId, quantity, result);
        return result;
    }
}
