package com.ticket.system.service.impl;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.entity.LoginLog;
import com.ticket.system.entity.OperationLog;
import com.ticket.system.mapper.LoginLogMapper;
import com.ticket.system.mapper.OperationLogMapper;
import com.ticket.system.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LogServiceImpl implements LogService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public void recordLoginLog(Long userId, String username, Integer loginType, Integer status, String message) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginType(loginType);
        loginLog.setStatus(status);
        loginLog.setMessage(message);

        // 获取请求信息
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
                String ip = getIp(request);
                loginLog.setIp(ip);
                String userAgent = request.getHeader("User-Agent");
                loginLog.setDevice(userAgent);
                loginLog.setBrowser(parseBrowser(userAgent));
            }
        } catch (Exception e) {
            log.debug("获取登录请求信息失败：{}", e.getMessage());
        }

        loginLogMapper.insert(loginLog);
    }

    @Override
    public IPage<OperationLog> getOperationLogs(Integer page, Integer size, String username, String module) {
        Page<OperationLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.isNotBlank(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return operationLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public IPage<LoginLog> getLoginLogs(Integer page, Integer size, String username, Integer status) {
        Page<LoginLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(LoginLog::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(LoginLog::getStatus, status);
        }
        wrapper.orderByDesc(LoginLog::getCreateTime);
        return loginLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void exportOperationLogs(HttpServletResponse response, String username, String module) throws Exception {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.isNotBlank(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        List<List<Object>> rows = new ArrayList<>();
        for (OperationLog opLog : logs) {
            List<Object> row = new ArrayList<>();
            row.add(opLog.getId());
            row.add(opLog.getUserId() != null ? opLog.getUserId() : "");
            row.add(opLog.getUsername() != null ? opLog.getUsername() : "");
            row.add(opLog.getModule() != null ? opLog.getModule() : "");
            row.add(opLog.getOperation() != null ? opLog.getOperation() : "");
            row.add(opLog.getMethod() != null ? opLog.getMethod() : "");
            row.add(opLog.getRequestUrl() != null ? opLog.getRequestUrl() : "");
            row.add(opLog.getIp() != null ? opLog.getIp() : "");
            row.add(opLog.getResult() != null && opLog.getResult() == 1 ? "成功" : "失败");
            row.add(opLog.getErrorMsg() != null ? opLog.getErrorMsg() : "");
            row.add(opLog.getCostTime() != null ? opLog.getCostTime() : 0);
            row.add(opLog.getCreateTime() != null ? opLog.getCreateTime().format(DTF) : "");
            rows.add(row);
        }

        String[] headers = {"ID", "操作人ID", "操作人", "操作模块", "操作描述", "请求方法", "请求URL", "IP地址", "操作结果", "错误信息", "耗时(ms)", "操作时间"};
        writeExcel(response, "操作日志", headers, rows);
    }

    @Override
    public void exportLoginLogs(HttpServletResponse response, String username) throws Exception {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(LoginLog::getUsername, username);
        }
        wrapper.orderByDesc(LoginLog::getCreateTime);
        List<LoginLog> logs = loginLogMapper.selectList(wrapper);

        List<List<Object>> rows = new ArrayList<>();
        for (LoginLog loginLog : logs) {
            List<Object> row = new ArrayList<>();
            row.add(loginLog.getId());
            row.add(loginLog.getUserId() != null ? loginLog.getUserId() : "");
            row.add(loginLog.getUsername() != null ? loginLog.getUsername() : "");
            row.add(loginLog.getLoginType() != null && loginLog.getLoginType() == 1 ? "登录" : "退出");
            row.add(loginLog.getIp() != null ? loginLog.getIp() : "");
            row.add(loginLog.getBrowser() != null ? loginLog.getBrowser() : "");
            row.add(loginLog.getLocation() != null ? loginLog.getLocation() : "");
            row.add(loginLog.getStatus() != null && loginLog.getStatus() == 1 ? "成功" : "失败");
            row.add(loginLog.getMessage() != null ? loginLog.getMessage() : "");
            row.add(loginLog.getCreateTime() != null ? loginLog.getCreateTime().format(DTF) : "");
            rows.add(row);
        }

        String[] headers = {"ID", "用户ID", "用户名", "类型", "IP地址", "浏览器", "登录地点", "状态", "消息", "时间"};
        writeExcel(response, "登录日志", headers, rows);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取当前HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("获取HttpServletRequest失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取客户端IP
     */
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从User-Agent解析浏览器名称
     */
    private String parseBrowser(String userAgent) {
        if (StringUtils.isBlank(userAgent)) {
            return "未知";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            return "Safari";
        }
        if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "IE";
        }
        return "其他";
    }

    /**
     * 写Excel（参考FinanceServiceImpl的writeExcel方法模式）
     */
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
}