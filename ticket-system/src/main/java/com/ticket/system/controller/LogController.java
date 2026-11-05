package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.LoginLog;
import com.ticket.system.entity.OperationLog;
import com.ticket.system.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * 系统日志控制器 - 管理员专用
 */
@RestController
@RequestMapping("/admin/log")
public class LogController {

    @Autowired
    private RequestContext requestContext;

    @Resource
    private LogService logService;

    /**
     * 校验管理员权限
     */
    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 操作日志分页查询
     */
    @GetMapping("/operation")
    public Result<IPage<OperationLog>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module) {
        checkAdmin();
        IPage<OperationLog> result = logService.getOperationLogs(page, size, username, module);
        return Result.success(result);
    }

    /**
     * 登录日志分页查询
     */
    @GetMapping("/login")
    public Result<IPage<LoginLog>> getLoginLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        IPage<LoginLog> result = logService.getLoginLogs(page, size, username, status);
        return Result.success(result);
    }

    /**
     * 导出操作日志Excel
     */
    @GetMapping("/export/operation")
    public void exportOperationLogs(
            HttpServletResponse response,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module) throws Exception {
        checkAdmin();
        logService.exportOperationLogs(response, username, module);
    }

    /**
     * 导出登录日志Excel
     */
    @GetMapping("/export/login")
    public void exportLoginLogs(
            HttpServletResponse response,
            @RequestParam(required = false) String username) throws Exception {
        checkAdmin();
        logService.exportLoginLogs(response, username);
    }
}
