package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Attendee;

import java.util.List;

public interface AttendeeService {

    /**
     * 添加观演人
     */
    Attendee addAttendee(Long userId, Attendee attendee);

    /**
     * 修改观演人信息
     */
    Attendee updateAttendee(Long userId, Long id, Attendee attendee);

    /**
     * 删除观演人
     */
    void deleteAttendee(Long userId, Long id);

    /**
     * 设置默认观演人
     */
    void setDefault(Long userId, Long id);

    /**
     * 获取用户观演人列表
     */
    List<Attendee> getMyAttendees(Long userId);

    /**
     * 分页查询观演人
     */
    IPage<Attendee> getAttendeePage(Long userId, Integer page, Integer size, String keyword);

    /**
     * 校验同场活动是否重复购票（按身份证）
     */
    boolean checkDuplicatePurchase(Long userId, Long activityId, String idCard);
}
