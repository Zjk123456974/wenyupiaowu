package com.ticket.system.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Orders;
import com.ticket.system.entity.TransactionFlow;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.mapper.TransactionFlowMapper;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.FinanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 流水号单调序号，避免高并发下随机数碰撞导致唯一键冲突 */
    private static final AtomicLong FLOW_NO_SEQ = new AtomicLong(0);

    @Resource
    private TransactionFlowMapper transactionFlowMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Override
    @Transactional
    public void recordPayment(String orderNo, Long userId, Long activityId, BigDecimal amount, LocalDateTime payTime) {
        TransactionFlow flow = new TransactionFlow();
        flow.setFlowNo(generateFlowNo("FL"));
        flow.setOrderNo(orderNo);
        flow.setUserId(userId);
        flow.setActivityId(activityId);
        flow.setAmount(amount);
        flow.setFlowType(1);
        flow.setPayTime(payTime);
        flow.setRemark("订单支付收入");
        transactionFlowMapper.insert(flow);
        log.info("记录支付流水：orderNo={}, amount={}", orderNo, amount);
    }

    @Override
    @Transactional
    public void recordRefund(String orderNo, Long userId, Long activityId, BigDecimal amount, LocalDateTime payTime) {
        TransactionFlow flow = new TransactionFlow();
        flow.setFlowNo(generateFlowNo("RF"));
        flow.setOrderNo(orderNo);
        flow.setUserId(userId);
        flow.setActivityId(activityId);
        flow.setAmount(amount);
        flow.setFlowType(2);
        flow.setPayTime(payTime);
        flow.setRemark("订单退款支出");
        transactionFlowMapper.insert(flow);
        log.info("记录退款流水：orderNo={}, amount={}", orderNo, amount);
    }

    @Override
    @Transactional
    public void recordFee(String orderNo, Long userId, Long activityId, BigDecimal amount, LocalDateTime payTime) {
        TransactionFlow flow = new TransactionFlow();
        flow.setFlowNo(generateFlowNo("FF"));
        flow.setOrderNo(orderNo);
        flow.setUserId(userId);
        flow.setActivityId(activityId);
        flow.setAmount(amount);
        flow.setFlowType(3);
        flow.setPayTime(payTime);
        flow.setRemark("退票手续费");
        transactionFlowMapper.insert(flow);
        log.info("记录手续费流水：orderNo={}, amount={}", orderNo, amount);
    }

    @Override
    public Map<String, Object> getFinanceSummary(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<TransactionFlow> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(TransactionFlow::getPayTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TransactionFlow::getPayTime, endTime);
        }

        List<TransactionFlow> flows = transactionFlowMapper.selectList(wrapper);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        int incomeCount = 0;
        int refundCount = 0;

        for (TransactionFlow flow : flows) {
            if (flow.getFlowType() == 1) {
                totalIncome = totalIncome.add(flow.getAmount());
                incomeCount++;
            } else if (flow.getFlowType() == 2) {
                totalRefund = totalRefund.add(flow.getAmount());
                refundCount++;
            }
        }

        BigDecimal netRevenue = totalIncome.subtract(totalRefund);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalRefund", totalRefund);
        result.put("netRevenue", netRevenue);
        result.put("incomeCount", incomeCount);
        result.put("refundCount", refundCount);
        result.put("totalFlowCount", flows.size());
        return result;
    }

    @Override
    public Map<String, Object> getDailyReconciliation() {
        List<Map<String, Object>> dailyData = transactionFlowMapper.dailyReconciliation();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        int totalOrders = 0;

        for (Map<String, Object> row : dailyData) {
            BigDecimal income = toBigDecimal(row.get("income"));
            BigDecimal refund = toBigDecimal(row.get("refund"));
            totalIncome = totalIncome.add(income);
            totalRefund = totalRefund.add(refund);
            totalOrders += ((Number) row.get("orderCount")).intValue();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dailyList", dailyData);
        result.put("totalIncome", totalIncome);
        result.put("totalRefund", totalRefund);
        result.put("netRevenue", totalIncome.subtract(totalRefund));
        result.put("totalOrders", totalOrders);
        result.put("totalDays", dailyData.size());
        return result;
    }

    @Override
    public IPage<TransactionFlow> getFlowPage(Integer page, Integer size, Integer flowType,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        Page<TransactionFlow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TransactionFlow> wrapper = new LambdaQueryWrapper<>();
        if (flowType != null) {
            wrapper.eq(TransactionFlow::getFlowType, flowType);
        }
        if (startTime != null) {
            wrapper.ge(TransactionFlow::getPayTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TransactionFlow::getPayTime, endTime);
        }
        wrapper.orderByDesc(TransactionFlow::getPayTime);
        IPage<TransactionFlow> result = transactionFlowMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillFlowInfo);
        return result;
    }

    @Override
    public void exportOrders(HttpServletResponse response, LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(Orders::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Orders::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        List<Orders> orders = ordersMapper.selectList(wrapper);

        List<List<Object>> rows = new ArrayList<>();
        for (Orders order : orders) {
            Activity act = activityMapper.selectById(order.getActivityId());
            User user = userMapper.selectById(order.getUserId());
            List<Object> row = new ArrayList<>();
            row.add(order.getOrderNo());
            row.add(user != null ? user.getUsername() : "");
            row.add(act != null ? act.getTitle() : "");
            row.add(order.getSeatType() != null ? order.getSeatType() : "");
            row.add(order.getQuantity());
            row.add(order.getTotalAmount());
            row.add(orderStatusName(order.getStatus()));
            row.add(order.getCreateTime() != null ? order.getCreateTime().format(DTF) : "");
            row.add(order.getPayTime() != null ? order.getPayTime().format(DTF) : "");
            row.add(order.getAttendeeName() != null ? order.getAttendeeName() : "");
            rows.add(row);
        }

        String[] headers = {"订单号", "用户名", "活动名称", "票档类型", "数量", "总金额", "订单状态", "创建时间", "支付时间", "观演人"};
        writeExcel(response, "订单数据", headers, rows);
    }

    @Override
    public void exportUsers(HttpServletResponse response) throws Exception {
        List<User> users = userMapper.selectList(null);

        List<List<Object>> rows = new ArrayList<>();
        for (User user : users) {
            List<Object> row = new ArrayList<>();
            row.add(user.getId());
            row.add(user.getUsername());
            row.add(user.getNickname() != null ? user.getNickname() : "");
            row.add(user.getPhone() != null ? user.getPhone() : "");
            row.add(roleName(user.getRole()));
            row.add(userStatusName(user.getStatus()));
            row.add(user.getCreateTime() != null ? user.getCreateTime().format(DTF) : "");
            rows.add(row);
        }

        String[] headers = {"用户ID", "用户名", "昵称", "手机号", "角色", "状态", "注册时间"};
        writeExcel(response, "用户数据", headers, rows);
    }

    @Override
    public void exportActivities(HttpServletResponse response) throws Exception {
        List<Activity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreateTime));

        List<List<Object>> rows = new ArrayList<>();
        for (Activity act : activities) {
            List<Object> row = new ArrayList<>();
            row.add(act.getId());
            row.add(act.getTitle());
            row.add(act.getVenue() != null ? act.getVenue() : "");
            row.add(act.getStartTime() != null ? act.getStartTime().format(DTF) : "");
            row.add(act.getTotalTickets());
            row.add(act.getSoldTickets());
            row.add(act.getTotalTickets() != null && act.getSoldTickets() != null
                    ? act.getTotalTickets() - act.getSoldTickets() : 0);
            row.add(act.getPrice());
            row.add(activityStatusName(act.getStatus()));
            row.add(act.getViewCount());
            rows.add(row);
        }

        String[] headers = {"活动ID", "活动名称", "场馆", "演出时间", "总票数", "已售", "剩余", "票价", "状态", "浏览量"};
        writeExcel(response, "活动数据", headers, rows);
    }

    @Override
    public void exportFinance(HttpServletResponse response, LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        LambdaQueryWrapper<TransactionFlow> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(TransactionFlow::getPayTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TransactionFlow::getPayTime, endTime);
        }
        wrapper.orderByDesc(TransactionFlow::getPayTime);
        List<TransactionFlow> flows = transactionFlowMapper.selectList(wrapper);
        flows.forEach(this::fillFlowInfo);

        List<List<Object>> rows = new ArrayList<>();
        for (TransactionFlow flow : flows) {
            List<Object> row = new ArrayList<>();
            row.add(flow.getFlowNo());
            row.add(flow.getOrderNo());
            row.add(flow.getUsername() != null ? flow.getUsername() : "");
            row.add(flow.getActivityTitle() != null ? flow.getActivityTitle() : "");
            row.add(flow.getAmount());
            row.add(flowTypeName(flow.getFlowType()));
            row.add(flow.getPayTime() != null ? flow.getPayTime().format(DTF) : "");
            row.add(flow.getRemark() != null ? flow.getRemark() : "");
            rows.add(row);
        }

        String[] headers = {"流水编号", "订单号", "用户名", "活动名称", "金额", "类型", "交易时间", "备注"};
        writeExcel(response, "财务流水", headers, rows);
    }

    // ==================== 私有方法 ====================

    /**
     * 生成流水号：前缀 + 毫秒时间戳 + 5位单调序号。
     * 使用 AtomicLong 保证同毫秒内序号唯一，彻底避免高并发下随机碰撞导致的唯一键冲突。
     *
     * @param prefix 流水号前缀（FL=支付收入, RF=退款支出, FF=手续费）
     */
    private String generateFlowNo(String prefix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        long seq = FLOW_NO_SEQ.incrementAndGet() % 100000;
        return prefix + timestamp + String.format("%05d", seq);
    }

    private void fillFlowInfo(TransactionFlow flow) {
        if (flow.getUserId() != null) {
            User user = userMapper.selectById(flow.getUserId());
            if (user != null) {
                flow.setUsername(user.getUsername());
            }
        }
        if (flow.getActivityId() != null) {
            Activity act = activityMapper.selectById(flow.getActivityId());
            if (act != null) {
                flow.setActivityTitle(act.getTitle());
            }
        }
    }

    private void writeExcel(HttpServletResponse response, String fileName, String[] headers, List<List<Object>> rows) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode(fileName + ".xlsx", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        // 写表头
        for (int i = 0; i < headers.length; i++) {
            writer.writeCellValue(i, 0, headers[i]);
        }
        // 写数据
        for (int r = 0; r < rows.size(); r++) {
            List<Object> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                writer.writeCellValue(c, r + 1, row.get(c));
            }
        }
        // 设置表头样式
        CellStyle headStyle = writer.getCellStyle();
        Font font = writer.getWorkbook().createFont();
        font.setBold(true);
        headStyle.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            writer.getOrCreateCell(i, 0).setCellStyle(headStyle);
        }
        // 自动列宽
        for (int i = 0; i < headers.length; i++) {
            writer.getSheet().autoSizeColumn(i);
        }
        writer.flush(response.getOutputStream(), true);
        writer.close();
    }

    private String orderStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            case 3: return "已过期";
            case 5: return "已退款";
            default: return "未知";
        }
    }

    private String roleName(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case 0: return "普通用户";
            case 1: return "运营";
            case 2: return "管理员";
            default: return "未知";
        }
    }

    private String userStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 0: return "封禁";
            default: return "未知";
        }
    }

    private String activityStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已驳回";
            case 3: return "已上架";
            case 4: return "已下架";
            default: return "未知";
        }
    }

    private String flowTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "收入";
            case 2: return "退款";
            case 3: return "手续费";
            default: return "未知";
        }
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
