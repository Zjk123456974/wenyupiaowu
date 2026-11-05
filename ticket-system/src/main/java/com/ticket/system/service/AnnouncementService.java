package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Announcement;

public interface AnnouncementService {

    IPage<Announcement> getAnnouncementList(Integer page, Integer size);

    IPage<Announcement> getAdminPage(Integer page, Integer size, Integer status);

    Announcement getAnnouncementDetail(Long id);

    void addAnnouncement(String title, String content, Integer isTop);

    void updateAnnouncement(Long id, String title, String content, Integer isTop);

    void deleteAnnouncement(Long id);

    void topAnnouncement(Long id);

    void offlineAnnouncement(Long id);

    void onlineAnnouncement(Long id);
}
