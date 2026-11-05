package com.ticket.system.aspect;

import com.ticket.system.annotation.OperLog;
import com.ticket.system.config.RequestContext;
import com.ticket.system.entity.OperationLog;
import com.ticket.system.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志AOP切面
 * 拦截带有@OperLog注解的方法，自动记录操作日志
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Resource
    private OperationLogMapper operationLogMapper;

    @Resource
    private RequestContext requestContext;

    /**
     * 切点：所有标注了@OperLog注解的方法
     */
    @Pointcut("@annotation(com.ticket.system.annotation.OperLog)")
    public void operLogPointCut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operLogPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class);

        // 构建操作日志对象
        OperationLog logEntity = new OperationLog();
        logEntity.setModule(operLog.module());
        logEntity.setOperation(operLog.operation());
        logEntity.setMethod(method.getDeclaringClass().getName() + "." + method.getName());

        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setIp(getIp(request));
            logEntity.setDevice(request.getHeader("User-Agent"));
        }

        // 获取请求参数
        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                StringBuilder params = new StringBuilder();
                for (Object arg : args) {
                    if (arg != null) {
                        String argStr = arg.toString();
                        // 限制参数长度，避免过长
                        if (argStr.length() > 2000) {
                            argStr = argStr.substring(0, 2000) + "...";
                        }
                        if (params.length() > 0) {
                            params.append(", ");
                        }
                        params.append(argStr);
                    }
                }
                String paramStr = params.toString();
                if (paramStr.length() > 2000) {
                    paramStr = paramStr.substring(0, 2000) + "...";
                }
                logEntity.setRequestParams(paramStr);
            }
        } catch (Exception e) {
            logEntity.setRequestParams("参数序列化失败");
        }

        // 获取操作人信息（从RequestContext获取，可能为空如登录接口）
        try {
            Long userId = requestContext.getCurrentUserId();
            String username = requestContext.getCurrentUsername();
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
        } catch (Exception e) {
            // 未登录或无法获取用户信息，置空
            log.debug("无法获取当前用户信息：{}", e.getMessage());
        }

        // 执行目标方法
        Object result;
        try {
            result = point.proceed();
            logEntity.setResult(1);
        } catch (Throwable e) {
            // 方法执行失败
            logEntity.setResult(0);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000) + "...";
            }
            logEntity.setErrorMsg(errorMsg);
            throw e;
        } finally {
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime(costTime);
            logEntity.setCreateTime(LocalDateTime.now());

            // 异步保存日志，避免影响主业务流程
            try {
                operationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    /**
     * 获取当前HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("获取HttpServletRequest失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取客户端IP
     * 先从X-Forwarded-For header获取，再从request.getRemoteAddr()
     */
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多次代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
