package com.ticket.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.system.common.BusinessException;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.service.UserService;
import com.ticket.system.util.JwtUtil;
import com.ticket.system.util.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:info:";
    private static final long USER_CACHE_TTL = 30;

    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String LOGIN_LOCK_PREFIX = "login:lock:";
    /** 连续失败 5 次锁定 15 分钟，降低撞库/爆破风险 */
    private static final int LOGIN_FAIL_THRESHOLD = 5;
    private static final long LOGIN_LOCK_MINUTES = 15;
    private static final long LOGIN_FAIL_WINDOW_MINUTES = 10;

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*]{6,20}$";

    @Override
    public void register(String username, String password, String phone) {
        if (StringUtils.isBlank(username)) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new BusinessException("用户名长度需在3-20个字符之间");
        }
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (!password.matches(PASSWORD_REGEX)) {
            throw new BusinessException("密码需6-20位，且必须包含字母和数字");
        }
        if (StringUtils.isBlank(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        if (!phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }

        LambdaQueryWrapper<User> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(nameWrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 同时检查已逻辑删除的记录（DB唯一索引仍会阻止插入）
        if (userMapper.countByUsernameIncludingDeleted(username) > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 一个手机号只能注册一个账号，避免同一实名手机号批量注册刷票小号
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(phoneWrapper) > 0) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setPhone(phone);
        user.setRole(0);
        user.setStatus(1);
        user.setNickname(username);
        userMapper.insert(user);
    }

    @Override
    public String login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }

        // 登录失败锁定校验：连续失败达到阈值后锁定账号一段时间，防撞库/暴力破解
        String lockKey = LOGIN_LOCK_PREFIX + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long remain = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            throw new BusinessException("登录失败次数过多，账号已临时锁定"
                    + (remain != null && remain > 0 ? "，请 " + Math.max(1, remain / 60) + " 分钟后再试" : ""));
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        boolean passwordOk = user != null && user.getPassword() != null
                && PasswordUtil.matches(password, user.getPassword());

        if (!passwordOk) {
            recordLoginFail(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被封禁，原因：" + (StringUtils.isBlank(user.getBanReason()) ? "无" : user.getBanReason()));
        }

        // 登录成功清除失败计数
        redisTemplate.delete(LOGIN_FAIL_PREFIX + username);

        // 老账号仍是旧 MD5 哈希：登录成功时静默升级为 PBKDF2，用户无感知
        if (PasswordUtil.needsUpgrade(user.getPassword())) {
            try {
                User upgrade = new User();
                upgrade.setId(user.getId());
                upgrade.setPassword(PasswordUtil.encode(password));
                userMapper.updateById(upgrade);
            } catch (Exception e) {
                // 升级失败不影响本次登录
                System.out.println("密码哈希升级失败 userId=" + user.getId() + "：" + e.getMessage());
            }
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * 记录一次登录失败；连续失败达阈值后锁定账号。
     */
    private void recordLoginFail(String username) {
        String failKey = LOGIN_FAIL_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            if (count != null && count >= LOGIN_FAIL_THRESHOLD) {
                redisTemplate.delete(failKey);
                redisTemplate.opsForValue().set(LOGIN_LOCK_PREFIX + username, "1", LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            // Redis 异常不应阻断登录主流程，仅打印日志兜底
        }
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUserInfo(Long userId, String nickname, String avatar, String phone, String bio) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.isNotBlank(phone) && !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        if (StringUtils.isNotBlank(nickname)) {
            updateUser.setNickname(nickname);
        }
        if (StringUtils.isNotBlank(avatar)) {
            updateUser.setAvatar(avatar);
        }
        if (StringUtils.isNotBlank(phone)) {
            updateUser.setPhone(phone);
        }
        if (bio != null) {
            updateUser.setBio(bio);
        }
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public void resetPassword(Long userId, String phone, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getPhone().equals(phone)) {
            throw new BusinessException("手机号不匹配");
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public Integer getUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user.getStatus();
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public IPage<User> getUserPage(Integer page, Integer size, String keyword, Integer role, Integer status) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> result = userMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    @Override
    public void updateUser(Long userId, String nickname, String phone, Integer role, Integer status, String banReason, Integer operatorType) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.isNotBlank(phone) && !phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        if (StringUtils.isNotBlank(nickname)) {
            updateUser.setNickname(nickname);
        }
        if (StringUtils.isNotBlank(phone)) {
            updateUser.setPhone(phone);
        }
        if (role != null) {
            updateUser.setRole(role);
        }
        if (status != null) {
            updateUser.setStatus(status);
        }
        // 运营细分类型：仅 role=1（运营）有效，0 普通运营 / 1 活动运营
        if (operatorType != null) {
            updateUser.setOperatorType(operatorType != null && operatorType == 1 ? 1 : 0);
        }
        updateUser.setBanReason(banReason);
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public void banUser(Long userId, String banReason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被封禁");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setStatus(0);
        updateUser.setBanReason(banReason);
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public void unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 1) {
            throw new BusinessException("用户未被封禁");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setStatus(1);
        updateUser.setBanReason(null);
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(userId);
        redisTemplate.delete(USER_CACHE_PREFIX + userId);
    }

    @Override
    public void registerOperator(String username, String password, String phone, String nickname, Integer operatorType) {
        if (StringUtils.isBlank(username)) {
            throw new BusinessException("用户名不能为空");
        }
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (StringUtils.isBlank(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        if (!phone.matches(PHONE_REGEX)) {
            throw new BusinessException("手机号格式不正确");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(phoneWrapper) > 0) {
            throw new BusinessException("该手机号已被使用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setPhone(phone);
        user.setNickname(StringUtils.isNotBlank(nickname) ? nickname : username);
        user.setRole(1);
        // 0 普通运营（功能与改造前一致）/ 1 活动运营（额外开放电子票核销端）
        user.setOperatorType(operatorType != null && operatorType == 1 ? 1 : 0);
        user.setStatus(0);
        userMapper.insert(user);
    }

    @Override
    public void reviewOperator(Long userId, Boolean approved) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != 1) {
            throw new BusinessException("该用户不是运营账号");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        if (approved) {
            updateUser.setStatus(1);
        } else {
            updateUser.setRole(0);
            updateUser.setStatus(1);
        }
        userMapper.updateById(updateUser);
        refreshUserCache(userId);
    }

    private void cacheUserInfo(User user) {
        try {
            redisTemplate.opsForValue().set(USER_CACHE_PREFIX + user.getId(),
                    cn.hutool.json.JSONUtil.toJsonStr(user), USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    private User getCachedUser(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(USER_CACHE_PREFIX + userId);
            if (StringUtils.isNotBlank(json)) {
                return cn.hutool.json.JSONUtil.toBean(json, User.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void refreshUserCache(Long userId) {
        redisTemplate.delete(USER_CACHE_PREFIX + userId);
        User user = userMapper.selectById(userId);
        if (user != null) {
            cacheUserInfo(user);
        }
    }
}
