package com.ticket.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.Activity;
import com.ticket.system.entity.Favorite;
import com.ticket.system.mapper.ActivityMapper;
import com.ticket.system.mapper.FavoriteMapper;
import com.ticket.system.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public void addFavorite(Long userId, Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getActivityId, activityId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已收藏该活动");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setActivityId(activityId);
        favoriteMapper.insert(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long activityId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getActivityId, activityId);
        if (favoriteMapper.delete(wrapper) == 0) {
            throw new BusinessException("未收藏该活动");
        }
    }

    @Override
    public IPage<Favorite> getMyFavorites(Long userId, Integer page, Integer size) {
        Page<Favorite> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .orderByDesc(Favorite::getCreateTime);
        IPage<Favorite> result = favoriteMapper.selectPage(pageParam, wrapper);

        // 填充活动信息
        for (Favorite favorite : result.getRecords()) {
            Activity activity = activityMapper.selectById(favorite.getActivityId());
            if (activity != null) {
                favorite.setActivityTitle(activity.getTitle());
                favorite.setCoverImage(activity.getCoverImage());
                favorite.setActivityStartTime(activity.getStartTime());
                favorite.setPrice(activity.getPrice());
                favorite.setVenue(activity.getVenue());
                favorite.setCategoryName(activity.getCategoryName());
                favorite.setStatus(activity.getStatus());
                favorite.setTotalTickets(activity.getTotalTickets());
                favorite.setSoldTickets(activity.getSoldTickets());
                favorite.setWishCount(activity.getWishCount());
            }
        }
        return result;
    }

    @Override
    public boolean isFavorite(Long userId, Long activityId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getActivityId, activityId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
