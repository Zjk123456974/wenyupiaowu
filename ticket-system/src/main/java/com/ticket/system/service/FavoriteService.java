package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.Favorite;

public interface FavoriteService {

    void addFavorite(Long userId, Long activityId);

    void removeFavorite(Long userId, Long activityId);

    IPage<Favorite> getMyFavorites(Long userId, Integer page, Integer size);

    boolean isFavorite(Long userId, Long activityId);
}
