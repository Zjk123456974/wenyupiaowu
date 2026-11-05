package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.controller.SseController;
import com.ticket.system.entity.Message;
import com.ticket.system.mapper.MessageMapper;
import com.ticket.system.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SseController sseController;

    @Override
    public void sendMessage(Long userId, String title, String content, Integer type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setIsRead(0);
        messageMapper.insert(message);

        // 通过SSE实时推送给在线用户
        sseController.sendToUser(userId, "message", title + ": " + content);
    }

    @Override
    public IPage<Message> getMyMessages(Long userId, Integer page, Integer size, Boolean isRead) {
        Page<Message> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        if (isRead != null) {
            // 数据库 is_read 为 0/1 整型，这里把前端的布尔值转换后过滤
            wrapper.eq(Message::getIsRead, isRead ? 1 : 0);
        }
        wrapper.orderByDesc(Message::getCreateTime);
        return messageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void markAsRead(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException("消息不存在");
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Message::getUserId, userId)
               .eq(Message::getIsRead, 0)
               .set(Message::getIsRead, 1);
        messageMapper.update(null, wrapper);
    }

    @Override
    public void clearMessages(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        messageMapper.delete(wrapper);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId)
               .eq(Message::getIsRead, 0);
        return messageMapper.selectCount(wrapper);
    }
}
