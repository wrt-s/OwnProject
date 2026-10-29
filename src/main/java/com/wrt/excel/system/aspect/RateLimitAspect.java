package com.wrt.excel.system.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.wrt.excel.system.config.RateLimiterConfig;
import com.wrt.excel.system.interfance.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 14:33
 * @description: 限流切面
 **/
@Aspect
@Component
@Order(1)
@Slf4j
public class RateLimitAspect {

    private final ConcurrentHashMap<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();
    private final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private RateLimiterConfig.RateLimiterFactory rateLimiterFactory;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取限流Key
        String key = getKey(joinPoint, rateLimit);

        // 获取或创建限流器
        RateLimiter rateLimiter = limiterMap.computeIfAbsent(key,
                k -> RateLimiter.create(rateLimit.value()));

        // 尝试获取令牌
        boolean acquired;
        if (rateLimit.timeout() > 0) {
            acquired = rateLimiter.tryAcquire(rateLimit.timeout(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            acquired = rateLimiter.tryAcquire();
        }

        if (!acquired) {
            log.warn("Rate limit exceeded for key: {}, qps: {}", key, rateLimit.value());
            throw new RuntimeException ("请求过于频繁，请稍后重试");
        }

        log.debug("Rate limit acquired for key: {}, qps: {}", key, rateLimit.value());
        return joinPoint.proceed();
    }

    /**
     * 解析限流Key（支持SpEL表达式）
     */
    private String getKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String keyExpression = rateLimit.key();
        if (keyExpression.isEmpty()) {
            // 默认使用方法签名作为Key
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            return signature.getMethod().toString();
        }

        // 解析SpEL表达式
        EvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}