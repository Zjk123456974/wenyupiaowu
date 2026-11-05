package com.ticket.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.common.Result;
import com.ticket.system.entity.User;
import com.ticket.system.mapper.UserMapper;
import com.ticket.system.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        try {
            String token = request.getHeader("Authorization");
            if (StringUtils.isBlank(token)) {
                // 兼容 EventSource(SSE) 等无法携带请求头的场景，支持 ?token= 查询参数
                String queryToken = request.getParameter("token");
                if (StringUtils.isNotBlank(queryToken)) {
                    token = "Bearer " + queryToken;
                }
            }
            if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
                return false;
            }
            token = token.substring(7);
            if (!jwtUtil.validateToken(token)) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已过期，请重新登录");
                return false;
            }

            Long userId = jwtUtil.getUserId(token);
            // 每次请求实时核对账号是否存在且未被封禁，保证封禁/删除即时生效（而非仅登录时校验一次）
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
            if (user == null) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "账号不存在或已被删除，请重新登录");
                return false;
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "账号已被封禁，原因："
                        + (StringUtils.isBlank(user.getBanReason()) ? "无" : user.getBanReason()));
                return false;
            }

            request.setAttribute("userId", userId);
            request.setAttribute("username", jwtUtil.getUsername(token));
            request.setAttribute("role", jwtUtil.getRole(token));
            return true;
        } catch (Exception e) {
            // 防御兜底：拦截器内任何未预期的异常都转为标准 JSON，
            // 绝不放任异常逃逸到 Tomcat 变成裸 500 错误页（前端只能看到“系统内部错误”）
            try {
                writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
            } catch (Exception ignore) {
                // 响应已提交或写出失败，无法再返回 JSON，记录到 stderr 供排查
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 拦截器内直接写 JSON 响应。
     * <p>
     * 注意：这里不能 throw BusinessException——拦截器抛出的异常不会经过
     * {@code @ControllerAdvice}/{@code @ExceptionHandler}，会一路逃逸到 Tomcat
     * 变成裸 500 错误页，前端只能看到“系统内部错误”，且无法触发 401 的重新登录逻辑。
     * <p>
     * 改为直接写标准 Result JSON，HTTP 状态码与 body code 保持一致：
     * 前端 request.js 对 HTTP 401 已有成熟处理（登出 + 跳登录页 + 友好提示），
     * 对 403 则展示 body 中的具体原因（如封禁理由）。
     */
    private void writeJson(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.error(httpStatus, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
