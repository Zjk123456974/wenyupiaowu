package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.TicketPrice;
import com.ticket.system.mapper.TicketPriceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ticket-price")
public class TicketPriceController {

    @Autowired
    private TicketPriceMapper ticketPriceMapper;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    @GetMapping("/activity/{activityId}")
    public Result<List<TicketPrice>> getByActivityId(@PathVariable Long activityId) {
        LambdaQueryWrapper<TicketPrice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketPrice::getActivityId, activityId)
               .eq(TicketPrice::getStatus, 1)
               .orderByAsc(TicketPrice::getSeatIndex);
        List<TicketPrice> prices = ticketPriceMapper.selectList(wrapper);
        return Result.success(prices);
    }

    @PostMapping
    public Result<TicketPrice> create(@RequestBody Map<String, Object> params) {
        checkAdmin();

        Long activityId = params.get("activityId") != null ? Long.valueOf(params.get("activityId").toString()) : null;
        String activityTitle = params.get("activityTitle") != null ? params.get("activityTitle").toString() : null;
        String seatType = params.get("seatType") != null ? params.get("seatType").toString() : null;
        BigDecimal price = params.get("price") != null ? new BigDecimal(params.get("price").toString()) : null;
        Integer totalStock = params.get("totalStock") != null ? Integer.valueOf(params.get("totalStock").toString()) : null;

        if (activityId == null || seatType == null || price == null) {
            throw new BusinessException("请填写必填字段");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("票价必须大于0");
        }
        if (totalStock != null && totalStock <= 0) {
            throw new BusinessException("票档库存必须大于0");
        }

        TicketPrice tp = new TicketPrice();
        tp.setActivityId(activityId);
        tp.setActivityTitle(activityTitle);
        tp.setSeatType(seatType);
        tp.setPrice(price);
        tp.setTotalStock(totalStock != null ? totalStock : 1000);
        tp.setSoldStock(0);
        tp.setSeatIndex(1);
        tp.setStatus(1);
        tp.setCreateTime(LocalDateTime.now());
        tp.setUpdateTime(LocalDateTime.now());

        ticketPriceMapper.insert(tp);
        return Result.success(tp);
    }

    @PutMapping("/{id}")
    public Result<TicketPrice> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        TicketPrice tp = ticketPriceMapper.selectById(id);
        if (tp == null) {
            throw new BusinessException("票档不存在");
        }

        String seatType = params.get("seatType") != null ? params.get("seatType").toString() : null;
        BigDecimal price = params.get("price") != null ? new BigDecimal(params.get("price").toString()) : null;
        Integer totalStock = params.get("totalStock") != null ? Integer.valueOf(params.get("totalStock").toString()) : null;

        if (seatType != null) tp.setSeatType(seatType);
        if (price != null) tp.setPrice(price);
        if (totalStock != null) {
            if (totalStock <= 0) {
                throw new BusinessException("票档库存必须大于0");
            }
            int sold = tp.getSoldStock() != null ? tp.getSoldStock() : 0;
            if (totalStock < sold) {
                throw new BusinessException("票档库存不能小于已售数量(" + sold + ")");
            }
            tp.setTotalStock(totalStock);
        }
        tp.setUpdateTime(LocalDateTime.now());
        // 已售/创建时间为系统维护字段，编辑票档时不允许整行回写旧值（防止并发覆盖已售库存计数）
        tp.setSoldStock(null);
        tp.setCreateTime(null);

        ticketPriceMapper.updateById(tp);
        return Result.success(tp);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        TicketPrice tp = ticketPriceMapper.selectById(id);
        if (tp == null) {
            throw new BusinessException("票档不存在");
        }
        // 已产生销售的票档删除会破坏已付订单的数据关联，禁止删除
        if (tp.getSoldStock() != null && tp.getSoldStock() > 0) {
            throw new BusinessException("该票档已有售出记录，不允许删除，请下架停售");
        }
        ticketPriceMapper.deleteById(id);
        return Result.success();
    }
}