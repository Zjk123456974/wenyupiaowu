package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.SystemConfig;
import com.ticket.system.mapper.SystemConfigMapper;
import com.ticket.system.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getConfigGroup));
    }

    @Override
    public List<SystemConfig> getConfigsByGroup(String group) {
        return systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigGroup, group));
    }

    @Override
    @Transactional
    public SystemConfig updateConfig(Long id, SystemConfig config) {
        SystemConfig exist = systemConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("配置项不存在");
        }
        if (StringUtils.hasText(config.getConfigValue())) {
            exist.setConfigValue(config.getConfigValue());
        }
        if (StringUtils.hasText(config.getConfigName())) {
            exist.setConfigName(config.getConfigName());
        }
        if (StringUtils.hasText(config.getRemark())) {
            exist.setRemark(config.getRemark());
        }
        systemConfigMapper.updateById(exist);
        log.info("更新系统配置：key={}, value={}", exist.getConfigKey(), exist.getConfigValue());
        return exist;
    }

    @Override
    public String getConfigValue(String key) {
        SystemConfig config = getConfigByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public SystemConfig getConfigByKey(String key) {
        return systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, key));
    }

    @Override
    @Transactional
    public void initDefaultConfigs() {
        if (systemConfigMapper.selectCount(null) > 0) {
            return;
        }
        String[][] defaults = {
                {"system.name", "智能文娱票务管理系统", "系统名称", "system", "平台显示名称"},
                {"system.logo", "", "网站LOGO", "system", "网站Logo图片地址"},
                {"system.icp", "京ICP备00000000号", "备案信息", "system", "网站备案号"},
                {"system.service_phone", "400-888-8888", "客服电话", "system", "平台客服联系电话"},
                {"order.timeout", "15", "订单超时时间(分钟)", "system", "待支付订单自动取消时间"},
                {"stock.warn_threshold", "100", "库存预警阈值", "system", "票档剩余库存低于此值触发预警"},
                {"ticket.rate_limit_interval", "3", "抢票限流间隔(秒)", "system", "同一用户抢票最小间隔时间"},
                {"upload.max_size", "10", "图片上传大小限制(MB)", "upload", "单张图片最大上传大小"},
                {"upload.allowed_types", "jpg,jpeg,png,gif,webp", "允许上传的图片格式", "upload", "图片文件允许的扩展名"},
                {"upload.path", "uploads/", "文件存储路径", "upload", "上传文件存储目录"},
                {"task.order_timeout_check", "60", "订单超时检查间隔(秒)", "task", "定时任务检查待支付订单超时的间隔"},
                {"task.status_auto_update", "300", "状态自动更新间隔(秒)", "task", "定时任务自动更新活动状态的间隔"}
        };
        for (String[] d : defaults) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(d[0]);
            config.setConfigValue(d[1]);
            config.setConfigName(d[2]);
            config.setConfigGroup(d[3]);
            config.setRemark(d[4]);
            systemConfigMapper.insert(config);
        }
        log.info("系统默认配置初始化完成，共{}项", defaults.length);
    }
}
