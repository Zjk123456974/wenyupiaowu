package com.ticket.system.service;

import com.ticket.system.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 按分组获取配置
     */
    List<SystemConfig> getConfigsByGroup(String group);

    /**
     * 更新配置
     */
    SystemConfig updateConfig(Long id, SystemConfig config);

    /**
     * 根据key获取配置值
     */
    String getConfigValue(String key);

    /**
     * 根据key获取配置
     */
    SystemConfig getConfigByKey(String key);

    /**
     * 初始化默认配置
     */
    void initDefaultConfigs();
}
