package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.ActivityTag;
import com.ticket.system.entity.ActivityTagRelation;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.ActivityTagMapper;
import com.ticket.system.mapper.ActivityTagRelationMapper;
import com.ticket.system.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TagServiceImpl implements TagService {

    @Resource
    private ActivityTagMapper tagMapper;

    @Resource
    private ActivityTagRelationMapper relationMapper;

    @Resource
    private ActivityMapper activityMapper;

    @Override
    public List<ActivityTag> getAllTags() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<ActivityTag>()
                        .orderByAsc(ActivityTag::getSortOrder)
                        .orderByDesc(ActivityTag::getCreateTime));
    }

    @Override
    public List<ActivityTag> getEnabledTags() {
        return tagMapper.listEnabledTags();
    }

    @Override
    @Transactional
    public ActivityTag addTag(ActivityTag tag) {
        if (!StringUtils.hasText(tag.getName())) {
            throw new BusinessException("标签名称不能为空");
        }
        Long existCount = tagMapper.selectCount(
                new LambdaQueryWrapper<ActivityTag>().eq(ActivityTag::getName, tag.getName()));
        if (existCount > 0) {
            throw new BusinessException("标签名称已存在");
        }
        if (tag.getColor() == null) {
            tag.setColor("#409EFF");
        }
        if (tag.getSortOrder() == null) {
            tag.setSortOrder(0);
        }
        if (tag.getStatus() == null) {
            tag.setStatus(1);
        }
        tagMapper.insert(tag);
        log.info("新增标签：{}", tag.getName());
        return tag;
    }

    @Override
    @Transactional
    public ActivityTag updateTag(Long id, ActivityTag tag) {
        ActivityTag exist = tagMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("标签不存在");
        }
        if (StringUtils.hasText(tag.getName())) {
            Long dupCount = tagMapper.selectCount(
                    new LambdaQueryWrapper<ActivityTag>()
                            .eq(ActivityTag::getName, tag.getName())
                            .ne(ActivityTag::getId, id));
            if (dupCount > 0) {
                throw new BusinessException("标签名称已存在");
            }
            exist.setName(tag.getName());
        }
        if (tag.getColor() != null) {
            exist.setColor(tag.getColor());
        }
        if (tag.getSortOrder() != null) {
            exist.setSortOrder(tag.getSortOrder());
        }
        if (tag.getStatus() != null) {
            exist.setStatus(tag.getStatus());
        }
        tagMapper.updateById(exist);
        return exist;
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        // 检查标签是否被活动使用
        Long useCount = relationMapper.selectCount(
                new LambdaQueryWrapper<ActivityTagRelation>().eq(ActivityTagRelation::getTagId, id));
        if (useCount > 0) {
            throw new BusinessException("该标签已被活动使用，无法删除，请先解除关联");
        }
        tagMapper.deleteById(id);
        log.info("删除标签ID：{}", id);
    }

    @Override
    @Transactional
    public void bindActivityTags(Long activityId, List<Long> tagIds) {
        // 先删除旧关联
        relationMapper.delete(
                new LambdaQueryWrapper<ActivityTagRelation>()
                        .eq(ActivityTagRelation::getActivityId, activityId));
        // 再插入新关联
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                ActivityTagRelation relation = new ActivityTagRelation();
                relation.setActivityId(activityId);
                relation.setTagId(tagId);
                relationMapper.insert(relation);
            }
        }
        log.info("绑定活动{}的标签：{}", activityId, tagIds);
    }

    @Override
    public List<Long> getActivityTagIds(Long activityId) {
        return relationMapper.selectTagIdsByActivityId(activityId);
    }

    @Override
    public List<ActivityTag> getActivityTags(Long activityId) {
        List<Long> tagIds = relationMapper.selectTagIdsByActivityId(activityId);
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectList(
                new LambdaQueryWrapper<ActivityTag>()
                        .in(ActivityTag::getId, tagIds)
                        .eq(ActivityTag::getStatus, 1)
                        .orderByAsc(ActivityTag::getSortOrder));
    }

    @Override
    public List<Map<String, Object>> getTagStatistics() {
        List<ActivityTag> tags = tagMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityTag tag : tags) {
            Long count = relationMapper.selectCount(
                    new LambdaQueryWrapper<ActivityTagRelation>().eq(ActivityTagRelation::getTagId, tag.getId()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tag.getId());
            item.put("name", tag.getName());
            item.put("color", tag.getColor());
            item.put("status", tag.getStatus());
            item.put("activityCount", count);
            result.add(item);
        }
        return result;
    }
}
