package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.User;
import com.ticket.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    @GetMapping("/page")
    public Result<IPage<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        IPage<User> result = userService.getUserPage(page, size, keyword, role, status);
        return Result.success(result);
    }

    @OperLog(module = "用户管理", operation = "编辑用户")

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        checkAdmin();
        String nickname = params.get("nickname") != null ? params.get("nickname").toString() : null;
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        Integer role = params.get("role") != null ? Integer.valueOf(params.get("role").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;
        Integer operatorType = params.get("operatorType") != null ? Integer.valueOf(params.get("operatorType").toString()) : null;
        String banReason = params.get("banReason") != null ? params.get("banReason").toString() : null;
        userService.updateUser(id, nickname, phone, role, status, banReason, operatorType);
        return Result.success();
    }

    @OperLog(module = "用户管理", operation = "封禁用户")

    @PutMapping("/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id,
                                @RequestParam(required = false) String banReason) {
        checkAdmin();
        userService.banUser(id, banReason);
        return Result.success();
    }

    @OperLog(module = "用户管理", operation = "解封用户")

    @PutMapping("/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        checkAdmin();
        userService.unbanUser(id);
        return Result.success();
    }

    @OperLog(module = "用户管理", operation = "删除用户")

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        checkAdmin();
        userService.deleteUser(id);
        return Result.success();
    }

    @OperLog(module = "用户管理", operation = "新建运营账号")

    @PostMapping("/operator")
    public Result<Void> registerOperator(@RequestBody Map<String, String> params) {
        checkAdmin();
        Integer operatorType = params.get("operatorType") != null
                ? Integer.valueOf(params.get("operatorType")) : 0;
        userService.registerOperator(params.get("username"), params.get("password"),
                params.get("phone"), params.get("nickname"), operatorType);
        return Result.success();
    }

    @OperLog(module = "用户管理", operation = "审核运营账号")

    @PutMapping("/operator/{id}/review")
    public Result<Void> reviewOperator(@PathVariable Long id,
                                       @RequestParam(required = false) Boolean approved) {
        checkAdmin();
        if (approved == null) {
            throw new BusinessException("审核结果不能为空");
        }
        userService.reviewOperator(id, approved);
        return Result.success();
    }
}
