package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Announcement;
import com.ticket.system.mapper.AnnouncementMapper;
import com.ticket.system.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public IPage<Announcement> getAnnouncementList(Integer page, Integer size) {
        Page<Announcement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 1)
               .orderByDesc(Announcement::getIsTop)
               .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public IPage<Announcement> getAdminPage(Integer page, Integer size, Integer status) {
        Page<Announcement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        wrapper.orderByDesc(Announcement::getIsTop)
               .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Announcement getAnnouncementDetail(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        return announcement;
    }

    @Override
    public void addAnnouncement(String title, String content, Integer isTop) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsTop(isTop != null ? isTop : 0);
        announcement.setStatus(1);
        announcementMapper.insert(announcement);
    }

    @Override
    public void updateAnnouncement(Long id, String title, String content, Integer isTop) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (title != null) {
            announcement.setTitle(title);
        }
        if (content != null) {
            announcement.setContent(content);
        }
        if (isTop != null) {
            announcement.setIsTop(isTop);
        }
        announcementMapper.updateById(announcement);
    }

    @Override
    public void deleteAnnouncement(Long id) {
        if (announcementMapper.selectById(id) == null) {
            throw new BusinessException("公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    @Override
    public void topAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        // 置顶/取消置顶切换
        announcement.setIsTop(announcement.getIsTop() == 1 ? 0 : 1);
        announcementMapper.updateById(announcement);
    }

    @Override
    public void offlineAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        announcement.setStatus(0);
        announcementMapper.updateById(announcement);
    }

    @Override
    public void onlineAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        announcement.setStatus(1);
        announcementMapper.updateById(announcement);
    }
}
