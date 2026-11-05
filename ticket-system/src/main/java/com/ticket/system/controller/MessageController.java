package com.ticket.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.common.Result;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.Message;
import com.ticket.system.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private RequestContext requestContext;

    /**
     * 消息列表
     * <p>isRead 使用 Boolean 接收：前端传 true/false，Spring 可直接转换；
     * 若用 Integer 接收会把 "true"/"false" 当成数字解析而抛 NumberFormatException。</p>
     */
    @GetMapping("/list")
    public Result<IPage<Message>> getMessageList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean isRead) {
        Long userId = requestContext.getCurrentUserId();
        IPage<Message> result = messageService.getMyMessages(userId, page, size, isRead);
        return Result.success(result);
    }

    /**
     * 未读数
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = requestContext.getCurrentUserId();
        Long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = requestContext.getCurrentUserId();
        messageService.markAsRead(userId, id);
        return Result.success();
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = requestContext.getCurrentUserId();
        messageService.markAllRead(userId);
        return Result.success();
    }

    /**
     * 清空消息
     */
    @DeleteMapping("/clear")
    public Result<Void> clearMessages() {
        Long userId = requestContext.getCurrentUserId();
        messageService.clearMessages(userId);
        return Result.success();
    }
}
