package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.User;
import com.ticket.system.service.LogService;
import com.ticket.system.service.UserService;
import com.ticket.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private JwtUtil jwtUtil;

    @Lazy
    @Resource
    private LogService logService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> params) {
        userService.register(params.get("username"), params.get("password"), params.get("phone"));
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        // 注意：这里绝不能打印 params，其中包含明文密码，会泄露到日志
        String username = params.get("username");
        try {
            String token = userService.login(username, params.get("password"));
            Long userId = jwtUtil.getUserId(token);
            String loginUsername = jwtUtil.getUsername(token);
            Integer role = jwtUtil.getRole(token);
            // 从数据库获取用户真实昵称和头像
            User user = userService.getUserInfo(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", userId);
            data.put("username", loginUsername);
            data.put("role", role);
            // 运营细分类型：0 普通运营 / 1 活动运营（非运营账号恒为 0）
            data.put("operatorType", user != null && user.getOperatorType() != null ? user.getOperatorType() : 0);
            data.put("nickname", user != null && user.getNickname() != null ? user.getNickname() : loginUsername);
            data.put("avatar", user != null ? user.getAvatar() : null);
            // 记录登录成功日志
            try {
                logService.recordLoginLog(userId, loginUsername, 1, 1, "登录成功");
            } catch (Exception e) {
                System.out.println("记录登录日志失败：" + e.getMessage());
            }
            return Result.success("登录成功", data);
        } catch (BusinessException e) {
            // 记录登录失败日志
            try {
                logService.recordLoginLog(null, username, 1, 0, e.getMessage());
            } catch (Exception ex) {
                System.out.println("记录登录日志失败：" + ex.getMessage());
            }
            throw e;
        }
    }

    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = requestContext.getCurrentUserId();
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody Map<String, String> params) {
        Long userId = requestContext.getCurrentUserId();
        userService.updateUserInfo(userId, params.get("nickname"), params.get("avatar"),
                params.get("phone"), params.get("bio"));
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = requestContext.getCurrentUserId();
        userService.changePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, String> params) {
        Long userId = requestContext.getCurrentUserId();
        userService.resetPassword(userId, params.get("phone"), params.get("newPassword"));
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getUserStatus() {
        Long userId = requestContext.getCurrentUserId();
        Integer status = userService.getUserStatus(userId);
        return Result.success(status);
    }
}
