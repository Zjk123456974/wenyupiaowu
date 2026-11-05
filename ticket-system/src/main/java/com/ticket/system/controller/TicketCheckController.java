package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.ElectronicTicket;
import com.ticket.system.entity.TicketCheckRecord;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.TicketCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 电子票核销与检票管理控制器
 *
 * <p>核销端权限模型（本项目不引入第二套系统，核销端就是本系统内的一个客户端）：</p>
 * <ul>
 *   <li>管理员（role=2）：可核销全部活动的电子票，数据范围为全量；</li>
 *   <li>活动运营（role=1 且 operatorType=1）：可核销且仅可核销自己负责的活动（activity.operator_id = 本人）；</li>
 *   <li>普通运营（role=1 且 operatorType=0）：无核销权限，其余运营功能保持不变。</li>
 * </ul>
 */
@RestController
@RequestMapping("/ticket-check")
public class TicketCheckController {

    @Autowired
    private TicketCheckService ticketCheckService;

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private UserMapper userMapper;

    /**
     * 解析当前登录人的核销数据范围。
     *
     * @return null 表示不限范围（管理员）；非 null 表示仅限该运营负责的活动
     */
    private Long resolveScopeOperatorId() {
        Integer role = requestContext.getCurrentRole();
        if (role == null) {
            throw new BusinessException(401, "未登录");
        }
        // 管理员：全量核销
        if (role == 2) {
            return null;
        }
        if (role == 1) {
            Long userId = requestContext.getCurrentUserId();
            User user = userId != null ? userMapper.selectById(userId) : null;
            if (user == null) {
                throw new BusinessException(401, "登录状态已失效，请重新登录");
            }
            if (!Integer.valueOf(1).equals(user.getOperatorType())) {
                throw new BusinessException(403, "普通运营无电子票核销权限，请使用活动运营账号操作");
            }
            return user.getId();
        }
        throw new BusinessException(403, "无电子票核销权限");
    }

    /**
     * 获取我的电子票（用户端）
     */
    @GetMapping("/my-tickets")
    public Result<List<ElectronicTicket>> getMyTickets() {
        Long userId = requestContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        List<ElectronicTicket> tickets = ticketCheckService.getMyTickets(userId);
        return Result.success(tickets);
    }

    /**
     * 电子票详情（用户端）
     */
    @GetMapping("/detail/{id}")
    public Result<ElectronicTicket> getTicketDetail(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        ElectronicTicket ticket = ticketCheckService.getTicketDetail(userId, id);
        return Result.success(ticket);
    }

    /**
     * 可核销的活动列表（核销端活动下拉）
     * 管理员返回全部已发布活动；活动运营仅返回自己负责的活动。
     */
    @GetMapping("/my-activities")
    public Result<List<Activity>> getMyCheckActivities() {
        Long scope = resolveScopeOperatorId();
        return Result.success(ticketCheckService.getCheckableActivities(scope));
    }

    /**
     * 扫码核销（活动运营/管理员）
     * 请求体包含 verifyCode（可为后端签名的时效令牌）
     */
    @OperLog(module = "票务核销", operation = "核销电子票")
    @PostMapping("/verify")
    public Result<TicketCheckRecord> verifyTicket(@RequestBody Map<String, Object> params,
                                                  HttpServletRequest request) {
        Long scope = resolveScopeOperatorId();

        String verifyCode = params.get("verifyCode") != null ? params.get("verifyCode").toString() : null;
        String device = params.get("device") != null ? params.get("device").toString() : null;
        String ip = request.getRemoteAddr();

        // 核销入口当前绑定的活动（前端已支持选择活动），用于防止 A 场次票被扫进 B 场次
        Long activityId = params.get("activityId") != null
                ? Long.valueOf(params.get("activityId").toString()) : null;

        Long checkUserId = requestContext.getCurrentUserId();
        String checkUsername = requestContext.getCurrentUsername();

        TicketCheckRecord record = ticketCheckService.checkTicket(verifyCode, activityId, checkUserId,
                checkUsername, device, ip, scope);
        return Result.success("核销成功", record);
    }

    /**
     * 核销记录分页查询（活动运营/管理员）
     */
    @GetMapping("/records")
    public Result<IPage<TicketCheckRecord>> getCheckRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long activityId) {
        Long scope = resolveScopeOperatorId();
        IPage<TicketCheckRecord> records = ticketCheckService.getCheckRecords(page, size, activityId, scope);
        return Result.success(records);
    }

    /**
     * 入场统计（活动运营/管理员）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getCheckStatistics(
            @RequestParam(required = false) Long activityId) {
        Long scope = resolveScopeOperatorId();
        Map<String, Object> statistics = ticketCheckService.getCheckStatistics(activityId, scope);
        return Result.success(statistics);
    }
}
