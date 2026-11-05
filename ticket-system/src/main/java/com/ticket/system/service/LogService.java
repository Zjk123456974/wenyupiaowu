package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.LoginLog;
import com.ticket.system.entity.OperationLog;

import javax.servlet.http.HttpServletResponse;

/**
 * 日志服务接口
 */
public interface LogService {

    /**
     * 记录登录日志
     *
     * @param userId    用户ID
     * @param username  用户名
     * @param loginType 登录类型 1-登录 2-退出
     * @param status    状态 1-成功 0-失败
     * @param message   提示消息
     */
    void recordLoginLog(Long userId, String username, Integer loginType, Integer status, String message);

    /**
     * 操作日志分页查询
     *
     * @param page     页码
     * @param size     每页数量
     * @param username 用户名（模糊查询）
     * @param module   操作模块
     * @return 分页结果
     */
    IPage<OperationLog> getOperationLogs(Integer page, Integer size, String username, String module);

    /**
     * 登录日志分页查询
     *
     * @param page     页码
     * @param size     每页数量
     * @param username 用户名（模糊查询）
     * @param status   状态 1-成功 0-失败
     * @return 分页结果
     */
    IPage<LoginLog> getLoginLogs(Integer page, Integer size, String username, Integer status);

    /**
     * 导出操作日志Excel
     *
     * @param response HttpServletResponse
     * @param username 用户名（模糊查询）
     * @param module   操作模块
     */
    void exportOperationLogs(HttpServletResponse response, String username, String module) throws Exception;

    /**
     * 导出登录日志Excel
     *
     * @param response HttpServletResponse
     * @param username 用户名（模糊查询）
     */
    void exportLoginLogs(HttpServletResponse response, String username) throws Exception;
}
