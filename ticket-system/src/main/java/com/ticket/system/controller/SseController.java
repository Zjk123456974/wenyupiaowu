package com.ticket.system.controller;

import com.ticket.system.common.BusinessException;
import com.ticket.system.config.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Autowired
    private RequestContext requestContext;

    /**
     * SSE消息订阅接口，用户登录后建立连接
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long userId;
        try {
            userId = requestContext.getCurrentUserId();
        } catch (Exception e) {
            throw new BusinessException(401, "未登录");
        }

        // 超时时间设置为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 仅在“当前存储的正是本 emitter”时才移除，避免旧连接关闭时误删已替换的新连接
        // （用户刷新/多标签场景下，新连接会覆盖 map 中的引用，旧连接的完成回调若按 key 删除会删掉新连接）
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        emitters.put(userId, emitter);
        return emitter;
    }

    /**
     * 向指定用户推送SSE消息
     */
    public void sendToUser(Long userId, String eventName, String data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * 向所有在线用户推送SSE消息
     */
    public void sendToAll(String eventName, String data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }
}
