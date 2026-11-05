package com.ticket.system.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 业务异常（未登录/无权限/参数不合法等）属于可预期的分支，用 WARN 记录即可，避免刷 ERROR 日志掩盖真实故障
        if (e.getCode() != null && e.getCode() >= 500) {
            log.error("业务异常: {}", e.getMessage());
        } else {
            log.warn("业务异常: {}", e.getMessage());
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("唯一键冲突: {}", e.getMessage());
        return Result.error("用户名已存在，请更换后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.error("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        log.error("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 路径变量/请求参数类型不匹配（典型场景：前端把 undefined、null 之类的字符串拼进了 URL）
     * 返回 400 并给出明确提示，避免被兜底成“系统内部错误”而难以排查。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配: 参数={}, 值={}, 期望类型={}",
                e.getName(), e.getValue(), e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName());
        if ("undefined".equals(String.valueOf(e.getValue())) || "null".equals(String.valueOf(e.getValue()))) {
            return Result.error(400, "请求参数缺失或无效，请刷新页面后重试");
        }
        return Result.error(400, "请求参数格式不正确：" + e.getName());
    }

    /**
     * 请求方法不支持（典型场景：把 GET 接口用 POST 调，或反之）
     * 返回 405 并给出明确提示，避免被兜底成“系统内部错误”而难以排查。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.error(405, "请求方式不正确：" + e.getMethod());
    }

    /**
     * 缺少必填的请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(400, "缺少必要的请求参数：" + e.getParameterName());
    }

    /**
     * 请求体格式不被支持（典型场景：前端以表单编码提交，后端却用 @RequestBody 接收 JSON）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的请求内容类型: {}", e.getContentType());
        return Result.error(415, "请求格式不正确，请检查提交方式");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error("系统内部错误，请联系管理员");
    }
}
