package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Attendee;
import com.ticket.system.entity.Orders;
import com.ticket.system.mapper.AttendeeMapper;
import com.ticket.system.mapper.OrdersMapper;
import com.ticket.system.service.AttendeeService;
import com.ticket.system.util.IdCardUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AttendeeServiceImpl implements AttendeeService {

    @Resource
    private AttendeeMapper attendeeMapper;

    @Resource
    private OrdersMapper ordersMapper;

    @Override
    @Transactional
    public Attendee addAttendee(Long userId, Attendee attendee) {
        if (!StringUtils.hasText(attendee.getName())) {
            throw new BusinessException("观演人姓名不能为空");
        }
        if (!StringUtils.hasText(attendee.getIdCard())) {
            throw new BusinessException("身份证号码不能为空");
        }
        // 归一化身份证号后统一存储与判重，防止 x/X、空白差异绕过重复校验
        String idCard = IdCardUtil.normalize(attendee.getIdCard());
        if (!IdCardUtil.isValid(idCard)) {
            throw new BusinessException("身份证号码格式不合法");
        }

        // 校验同一用户下身份证号不能重复
        Long existCount = attendeeMapper.selectCount(
                new LambdaQueryWrapper<Attendee>()
                        .eq(Attendee::getUserId, userId)
                        .eq(Attendee::getIdCard, idCard));
        if (existCount > 0) {
            throw new BusinessException("该身份证号已添加，请勿重复添加");
        }

        attendee.setName(attendee.getName().trim());
        attendee.setIdCard(idCard);
        attendee.setUserId(userId);
        attendee.setIsDefault(attendee.getIsDefault() != null ? attendee.getIsDefault() : 0);
        attendeeMapper.insert(attendee);

        // 如果设置为默认，取消其他默认
        if (attendee.getIsDefault() == 1) {
            clearOtherDefault(userId, attendee.getId());
        }

        log.info("用户{}添加观演人：{}", userId, attendee.getName());
        return attendee;
    }

    @Override
    @Transactional
    public Attendee updateAttendee(Long userId, Long id, Attendee attendee) {
        Attendee exist = getAndCheckOwnership(userId, id);

        if (StringUtils.hasText(attendee.getName())) {
            exist.setName(attendee.getName().trim());
        }
        if (StringUtils.hasText(attendee.getIdCard())) {
            String idCard = IdCardUtil.normalize(attendee.getIdCard());
            if (!IdCardUtil.isValid(idCard)) {
                throw new BusinessException("身份证号码格式不合法");
            }
            // 校验身份证号是否与其他观演人重复
            Long dupCount = attendeeMapper.selectCount(
                    new LambdaQueryWrapper<Attendee>()
                            .eq(Attendee::getUserId, userId)
                            .eq(Attendee::getIdCard, idCard)
                            .ne(Attendee::getId, id));
            if (dupCount > 0) {
                throw new BusinessException("该身份证号已被其他观演人使用");
            }
            exist.setIdCard(idCard);
        }
        if (attendee.getPhone() != null) {
            exist.setPhone(attendee.getPhone());
        }

        attendeeMapper.updateById(exist);
        return exist;
    }

    @Override
    @Transactional
    public void deleteAttendee(Long userId, Long id) {
        getAndCheckOwnership(userId, id);
        attendeeMapper.deleteById(id);
        log.info("用户{}删除观演人ID：{}", userId, id);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        getAndCheckOwnership(userId, id);
        clearOtherDefault(userId, id);
        Attendee update = new Attendee();
        update.setId(id);
        update.setIsDefault(1);
        attendeeMapper.updateById(update);
    }

    @Override
    public List<Attendee> getMyAttendees(Long userId) {
        return attendeeMapper.selectList(
                new LambdaQueryWrapper<Attendee>()
                        .eq(Attendee::getUserId, userId)
                        .orderByDesc(Attendee::getIsDefault)
                        .orderByDesc(Attendee::getCreateTime));
    }

    @Override
    public IPage<Attendee> getAttendeePage(Long userId, Integer page, Integer size, String keyword) {
        Page<Attendee> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Attendee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Attendee::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Attendee::getName, keyword)
                    .or().like(Attendee::getIdCard, keyword)
                    .or().like(Attendee::getPhone, keyword));
        }
        wrapper.orderByDesc(Attendee::getIsDefault)
               .orderByDesc(Attendee::getCreateTime);
        return attendeeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public boolean checkDuplicatePurchase(Long userId, Long activityId, String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return false;
        }
        // 检查同一场活动同一身份证号是否已有有效订单（待支付或已支付）
        Long count = ordersMapper.selectCount(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getActivityId, activityId)
                        .eq(Orders::getAttendeeIdCard, idCard)
                        .in(Orders::getStatus, 0, 1));
        return count > 0;
    }

    private Attendee getAndCheckOwnership(Long userId, Long id) {
        Attendee attendee = attendeeMapper.selectById(id);
        if (attendee == null) {
            throw new BusinessException("观演人信息不存在");
        }
        if (!attendee.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人观演人信息");
        }
        return attendee;
    }

    private void clearOtherDefault(Long userId, Long excludeId) {
        LambdaUpdateWrapper<Attendee> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Attendee::getUserId, userId)
               .ne(Attendee::getId, excludeId)
               .set(Attendee::getIsDefault, 0);
        attendeeMapper.update(null, wrapper);
    }
}
