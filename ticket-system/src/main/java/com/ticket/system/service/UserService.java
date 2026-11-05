package com.ticket.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ticket.system.entity.User;

public interface UserService {

    void register(String username, String password, String phone);

    String login(String username, String password);

    User getUserInfo(Long userId);

    void updateUserInfo(Long userId, String nickname, String avatar, String phone, String bio);

    void resetPassword(Long userId, String phone, String newPassword);

    Integer getUserStatus(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);

    IPage<User> getUserPage(Integer page, Integer size, String keyword, Integer role, Integer status);

    void updateUser(Long userId, String nickname, String phone, Integer role, Integer status, String banReason, Integer operatorType);

    void banUser(Long userId, String banReason);

    void unbanUser(Long userId);

    void deleteUser(Long userId);

    void registerOperator(String username, String password, String phone, String nickname, Integer operatorType);

    void reviewOperator(Long userId, Boolean approved);
}
