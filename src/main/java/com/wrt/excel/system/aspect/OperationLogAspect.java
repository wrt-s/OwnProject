package com.wrt.excel.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrt.excel.order.entity.OperationLog;
import com.wrt.excel.order.service.OperationLogService;
import com.wrt.excel.system.interfance.OperationLogAnnotation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 操作日志切面
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    @Qualifier("operationLogExecutor")
    private Executor operationLogExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogAnnotation operationLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 创建操作日志对象
        OperationLog logEntity = new OperationLog();
        logEntity.setModule(operationLogAnnotation.module());
        logEntity.setOperationType(operationLogAnnotation.operationType());
        logEntity.setDescription(operationLogAnnotation.description());
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIpAddress(getIpAddress(request));
            
            // 尝试从Session中获取用户信息（Love 使用 LOVE_* 键）
            try {
                Object userId = request.getSession().getAttribute("LOVE_USER_ID");
                if (userId == null) userId = request.getSession().getAttribute("userId");
                Object username = request.getSession().getAttribute("LOVE_NICKNAME");
                if (username == null) username = request.getSession().getAttribute("username");
                if (username == null) username = request.getSession().getAttribute("LOVE_PHONE");
                if (userId != null) {
                    logEntity.setUserId(Long.valueOf(userId.toString()));
                }
                if (username != null) {
                    logEntity.setUsername(username.toString());
                }
            } catch (Exception e) {
                log.debug("获取用户信息失败", e);
            }
        }
        
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        logEntity.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        
        // 记录请求参数
        if (operationLogAnnotation.recordParams()) {
            try {
                String params = getParams(joinPoint);
                logEntity.setRequestParams(params);
            } catch (Exception e) {
                log.warn("记录请求参数失败", e);
            }
        }
        
        // 默认状态为成功
        logEntity.setStatus(1);
        
        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 若返回 Map 带业务 code，按 code 判断成功失败（不抛异常的失败也会记失败）
            try {
                if (result instanceof java.util.Map) {
                    Object code = ((java.util.Map<?, ?>) result).get("code");
                    if (code != null) {
                        int c = Integer.parseInt(String.valueOf(code));
                        if (c != 200 && c != 0) {
                            logEntity.setStatus(0);
                            Object msg = ((java.util.Map<?, ?>) result).get("message");
                            if (msg != null) {
                                logEntity.setErrorMsg(String.valueOf(msg));
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
                // ignore parse errors
            }

            // 记录响应结果
            if (operationLogAnnotation.recordResult() && result != null) {
                try {
                    String resultStr = objectMapper.writeValueAsString(result);
                    // 限制结果长度，避免过长
                    if (resultStr.length() > 2000) {
                        resultStr = resultStr.substring(0, 2000) + "...";
                    }
                    logEntity.setResponseResult(resultStr);
                } catch (Exception e) {
                    log.warn("记录响应结果失败", e);
                }
            }
        } catch (Throwable throwable) {
            // 记录异常信息
            logEntity.setStatus(0);
            logEntity.setErrorMsg(throwable.getMessage());
            log.error("操作执行失败: {}", operationLogAnnotation.description(), throwable);
            throw throwable;
        } finally {
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime(costTime);
            
            // 异步保存日志（避免影响主业务流程）
            saveLogAsync(logEntity);
        }
        
        return result;
    }

    /**
     * 获取请求参数
     */
    private String getParams(ProceedingJoinPoint joinPoint) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        Map<String, Object> paramsMap = new HashMap<>();
        for (int i = 0; i < parameterNames.length; i++) {
            paramsMap.put(parameterNames[i], args[i]);
        }
        
        return objectMapper.writeValueAsString(paramsMap);
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，第一个为真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 异步保存日志（使用线程池）
     */
    private void saveLogAsync(OperationLog logEntity) {
        operationLogExecutor.execute(() -> {
            try {
                operationLogService.saveLog(logEntity);
            } catch (Exception e) {
                log.error("异步保存操作日志失败", e);
            }
        });
    }
}
