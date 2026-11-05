package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Message;

public interface MessageService {

    void sendMessage(Long userId, String title, String content, Integer type);

    /**
     * 分页查询我的消息
     *
     * @param userId 用户ID
     * @param page   当前页
     * @param size   每页大小
     * @param isRead 已读状态过滤：true=仅已读，false=仅未读，null=全部
     */
    IPage<Message> getMyMessages(Long userId, Integer page, Integer size, Boolean isRead);

    void markAsRead(Long userId, Long messageId);

    void markAllRead(Long userId);

    void clearMessages(Long userId);

    Long getUnreadCount(Long userId);
}
