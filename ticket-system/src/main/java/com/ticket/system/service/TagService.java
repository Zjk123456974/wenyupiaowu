package com.ticket.system.service;

import com.ticket.system.entity.ActivityTag;

import java.util.List;
import java.util.Map;

public interface TagService {

    /**
     * 获取所有标签
     */
    List<ActivityTag> getAllTags();

    /**
     * 获取启用标签列表（前端用）
     */
    List<ActivityTag> getEnabledTags();

    /**
     * 新增标签
     */
    ActivityTag addTag(ActivityTag tag);

    /**
     * 编辑标签
     */
    ActivityTag updateTag(Long id, ActivityTag tag);

    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 绑定活动标签
     */
    void bindActivityTags(Long activityId, List<Long> tagIds);

    /**
     * 获取活动的标签ID列表
     */
    List<Long> getActivityTagIds(Long activityId);

    /**
     * 获取活动的标签详情列表
     */
    List<ActivityTag> getActivityTags(Long activityId);

    /**
     * 标签统计数据（每个标签关联多少活动）
     */
    List<Map<String, Object>> getTagStatistics();
}
