package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.annotation.OperLog;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.SystemConfig;
import com.ticket.system.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private RequestContext requestContext;

    private void checkAdmin() {
        Integer role = requestContext.getCurrentRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 获取所有配置
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> list(@RequestParam(required = false) String group) {
        checkAdmin();
        if (group != null && !group.isEmpty()) {
            return Result.success(systemConfigService.getConfigsByGroup(group));
        }
        return Result.success(systemConfigService.getAllConfigs());
    }

    /**
     * 更新配置
     */
    @OperLog(module = "系统配置", operation = "修改系统配置")
    @PutMapping("/{id}")
    public Result<SystemConfig> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        checkAdmin();
        return Result.success("更新成功", systemConfigService.updateConfig(id, config));
    }

    /**
     * 根据key获取配置值
     */
    @GetMapping("/value/{key}")
    public Result<Map<String, String>> getValue(@PathVariable String key) {
        checkAdmin();
        String value = systemConfigService.getConfigValue(key);
        return Result.success(Map.of("key", key, "value", value != null ? value : ""));
    }

    /**
     * 公开站点信息（用户端无需登录即可读取，用于渲染站点名称/Logo/客服电话/备案号）
     * 仅返回白名单内的展示性配置，避免泄露内部参数
     */
    private static final List<String> PUBLIC_KEYS = List.of(
            "system.name", "system.logo", "system.icp", "system.service_phone");

    @GetMapping("/public")
    public Result<Map<String, String>> publicSiteInfo() {
        Map<String, String> site = new java.util.LinkedHashMap<>();
        for (String key : PUBLIC_KEYS) {
            String value = systemConfigService.getConfigValue(key);
            site.put(key.replaceFirst("^system\\.", ""), value != null ? value : "");
        }
        return Result.success(site);
    }
}
