package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ElectronicTicketMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 选座服务（对标大麦「在线选座」）：
 * <ul>
 *   <li>座位平面图由 activity.seat_layout（JSON）描述，仅在 seatMapEnabled=1 时启用；</li>
 *   <li>已售座位以 electronic_ticket.seat_code（status≠2）为权威来源，避免 Redis 重启丢失；</li>
 *   <li>待支付锁单占用以 Redis 单座 key（带 TTL）实现，超时自动释放，杜绝“占座不买”锁死库存；</li>
 *   <li>选座仅作用于 seatMapEnabled=1 的活动，其它活动（默认）购票流程完全不变。</li>
 * </ul>
 */
@Slf4j
@Service
public class SeatService {

    private static final String LOCK_PREFIX = "seat:lock:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private ActivityMapper activityMapper;

    @Resource
    private ElectronicTicketMapper electronicTicketMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取某活动的座位图：是否启用、布局、已售座位、锁定座位
     */
    public Map<String, Object> getSeatMap(Long activityId) {
        Map<String, Object> result = new HashMap<>();
        Activity act = activityMapper.selectById(activityId);
        int enabled = (act != null && act.getSeatMapEnabled() != null && act.getSeatMapEnabled() == 1) ? 1 : 0;
        result.put("seatMapEnabled", enabled);
        result.put("layout", act != null ? act.getSeatLayout() : null);
        if (enabled == 0 || act == null || act.getSeatLayout() == null) {
            return result;
        }

        // 已售座位：真实选座订单 + 按销量比例确定性补齐（详见 computeOccupied）
        List<String> occupied = computeOccupied(act, activityId);

        // 锁定座位（待支付锁单）：扫描 Redis 单座锁
        List<String> locked = new ArrayList<>();
        try {
            String pattern = LOCK_PREFIX + activityId + ":*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null) {
                String prefix = LOCK_PREFIX + activityId + ":";
                for (String k : keys) {
                    if (k.startsWith(prefix)) {
                        locked.add(k.substring(prefix.length()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询锁定座位失败 activityId={}", activityId, e);
        }

        result.put("occupied", occupied);
        result.put("locked", locked);
        return result;
    }

    /**
     * 校验座位是否合法（在布局内）并返回非法座位；同时校验未被占用/锁定
     */
    public void validateSeats(Long activityId, Activity act, List<String> seats) {
        if (seats == null || seats.isEmpty()) {
            throw new com.ticket.system.common.BusinessException("请先选择座位");
        }
        Set<String> validCodes = buildValidCodes(act.getSeatLayout());
        for (String code : seats) {
            if (code == null || !validCodes.contains(code)) {
                throw new com.ticket.system.common.BusinessException("座位 " + code + " 不在可选座位范围内");
            }
        }
        // 已售校验（数据库真实座位 + 按销量比例补齐的占用座位，前端灰掉的都不能选）
        Set<String> occupied = new HashSet<>(computeOccupied(act, activityId));
        for (String code : seats) {
            if (occupied.contains(code)) {
                throw new com.ticket.system.common.BusinessException("您选择的座位 " + code + " 已被售出，请重新选择");
            }
        }
    }

    /**
     * 尝试锁定座位（待支付锁单），全部成功返回 true；任一失败则释放已锁定的，返回 false
     */
    public boolean tryLockSeats(Long activityId, List<String> seats, int ttlSeconds) {
        List<String> acquired = new ArrayList<>();
        for (String code : seats) {
            String key = LOCK_PREFIX + activityId + ":" + code;
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
            if (ok == null || !ok) {
                for (String a : acquired) {
                    stringRedisTemplate.delete(LOCK_PREFIX + activityId + ":" + a);
                }
                return false;
            }
            acquired.add(code);
        }
        return true;
    }

    /**
     * 释放座位锁（取消/超时订单时调用，锁本身也带 TTL 自动过期）
     */
    public void releaseLocks(Long activityId, List<String> seats) {
        if (seats == null) {
            return;
        }
        for (String code : seats) {
            stringRedisTemplate.delete(LOCK_PREFIX + activityId + ":" + code);
        }
    }

    /**
     * 座位编码 -> 可读文案（如 "A区 3排5座"）
     */
    public String seatCodeToInfo(Long activityId, String seatCode) {
        if (seatCode == null) {
            return null;
        }
        Activity act = activityMapper.selectById(activityId);
        if (act == null || act.getSeatLayout() == null) {
            return seatCode;
        }
        try {
            JsonNode root = MAPPER.readTree(act.getSeatLayout());
            JsonNode sections = root.get("sections");
            if (sections != null && sections.isArray()) {
                for (JsonNode sec : sections) {
                    String id = sec.get("id").asText();
                    String name = sec.has("name") ? sec.get("name").asText() : id;
                    // 编码格式： {id}-{row}-{col}
                    String[] parts = seatCode.split("-");
                    if (parts.length == 3 && parts[0].equals(id)) {
                        return name + " " + parts[1] + "排" + parts[2] + "座";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("座位编码转文案失败 seatCode={}", seatCode, e);
        }
        return seatCode;
    }

    /**
     * 计算某活动当前「已售/被占用」的座位编码集合：
     * 1) 真实选座订单（electronic_ticket，status≠2 且 seat_code 非空）—— 权威来源；
     * 2) 按比例补齐：座位图是场馆真实容量，但种子/历史订单未必记录 seat_code。
     *    用「已售比例 × 座位图总座位数」推算应占用的座位数，将前面的座位确定性地标记为已售，
     *    保证「已售 77%」时座位图上也恰好约 77% 灰掉、只剩约 23% 可选。
     * getSeatMap 与 validateSeats 共用，确保“前端灰掉”的座位后端一定拦得住。
     */
    public List<String> computeOccupied(Activity act, Long activityId) {
        LinkedHashSet<String> occupied = new LinkedHashSet<>();
        // 1) 真实选座订单
        try {
            List<ElectronicTicket> tickets = electronicTicketMapper.selectList(
                    new LambdaQueryWrapper<ElectronicTicket>()
                            .eq(ElectronicTicket::getActivityId, activityId)
                            .ne(ElectronicTicket::getStatus, 2)
                            .isNotNull(ElectronicTicket::getSeatCode)
                            .select(ElectronicTicket::getSeatCode));
            for (ElectronicTicket t : tickets) {
                if (t.getSeatCode() != null) {
                    occupied.add(t.getSeatCode());
                }
            }
        } catch (Exception e) {
            log.warn("查询已售座位失败 activityId={}", activityId, e);
        }
        // 2) 按比例补齐
        List<String> ordered = buildOrderedCodes(act.getSeatLayout());
        int layoutTotal = ordered.size();
        if (layoutTotal > 0 && act.getTotalTickets() != null && act.getTotalTickets() > 0) {
            int sold = act.getSoldTickets() == null ? 0 : act.getSoldTickets();
            int targetOccupied = (int) Math.round((double) sold / act.getTotalTickets() * layoutTotal);
            int need = targetOccupied - occupied.size();
            if (need > 0) {
                for (String code : ordered) {
                    if (need <= 0) {
                        break;
                    }
                    if (occupied.add(code)) {
                        need--;
                    }
                }
            }
        }
        return new ArrayList<>(occupied);
    }

    /**
     * 按 分区→排→列 的固定顺序生成全部合法座位编码（用于确定性补齐，保证前后端一致）
     */
    private List<String> buildOrderedCodes(String layout) {
        List<String> codes = new ArrayList<>();
        if (layout == null) {
            return codes;
        }
        try {
            JsonNode root = MAPPER.readTree(layout);
            JsonNode sections = root.get("sections");
            if (sections != null && sections.isArray()) {
                for (JsonNode sec : sections) {
                    String id = sec.get("id").asText();
                    int rows = sec.has("rows") ? sec.get("rows").asInt() : 0;
                    int cols = sec.has("cols") ? sec.get("cols").asInt() : 0;
                    for (int r = 1; r <= rows; r++) {
                        for (int c = 1; c <= cols; c++) {
                            codes.add(id + "-" + r + "-" + c);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析座位布局失败 layout={}", layout, e);
        }
        return codes;
    }

    private Set<String> buildValidCodes(String layout) {        Set<String> codes = new HashSet<>();
        if (layout == null) {
            return codes;
        }
        try {
            JsonNode root = MAPPER.readTree(layout);
            JsonNode sections = root.get("sections");
            if (sections != null && sections.isArray()) {
                for (JsonNode sec : sections) {
                    String id = sec.get("id").asText();
                    int rows = sec.has("rows") ? sec.get("rows").asInt() : 0;
                    int cols = sec.has("cols") ? sec.get("cols").asInt() : 0;
                    for (int r = 1; r <= rows; r++) {
                        for (int c = 1; c <= cols; c++) {
                            codes.add(id + "-" + r + "-" + c);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析座位布局失败 layout={}", layout, e);
        }
        return codes;
    }
}
