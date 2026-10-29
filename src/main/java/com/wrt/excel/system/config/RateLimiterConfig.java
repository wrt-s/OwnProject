package com.wrt.excel.system.config;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 14:25
 * @description: 限流配置类
 **/
@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
@Data
public class RateLimiterConfig {

    private int defaultQps = 10;
    private Map<String, Integer> apis = new ConcurrentHashMap<>();

    /**
     * 默认限流器
     */
    @Bean
    public RateLimiter defaultRateLimiter() {
        return RateLimiter.create(defaultQps);
    }

    /**
     * 动态限流器工厂
     */
    @Bean
    public RateLimiterFactory rateLimiterFactory() {
        return new RateLimiterFactory();
    }

    /**
     * 限流器工厂类
     */
    public static class RateLimiterFactory {
        private final Map<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();

        public RateLimiter getRateLimiter(String key, double qps) {
            return limiterMap.computeIfAbsent(key, k -> RateLimiter.create(qps));
        }

        public RateLimiter getRateLimiter(String key) {
            return limiterMap.get(key);
        }
    }
}
