package com.wrt.excel.system.interfance;

import java.lang.annotation.*;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * QPS限制
     */
    double value() default 10.0;

    /**
     * 超时时间（毫秒），0表示不等待直接失败
     */
    long timeout() default 0;

    /**
     * 限流Key（支持SpEL表达式）
     */
    String key() default "";
}