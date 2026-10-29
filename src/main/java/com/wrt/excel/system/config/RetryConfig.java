package com.wrt.excel.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 09:55
 * @description: 重试和异步配置类
 **/
@Configuration
@EnableRetry
@EnableAsync
@Slf4j
public class RetryConfig {

    /**
     * 自定义线程池配置
     */
    @Bean("customExecutor")
    public Executor customExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数（根据CPU核心数调整）
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        // 最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // 队列容量
        executor.setQueueCapacity(200);
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        // 线程名前缀
        executor.setThreadNamePrefix("retry-async-");

        // 拒绝策略：当线程池和队列都满时，由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("自定义线程池初始化完成，核心线程数：{}", executor.getCorePoolSize());
        return executor;
    }

    /**
     * 备用线程池（用于不同业务场景）
     */
    @Bean("backupExecutor")
    public Executor backupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("backup-retry-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 自定义重试模板
     */
    @Bean("customRetryTemplate")
    public RetryTemplate customRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 设置重试策略：最多重试3次
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        // 设置退避策略：指数退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);   // 初始延迟1秒
        backOffPolicy.setMultiplier(2);            // 倍数2
        backOffPolicy.setMaxInterval(10000);       // 最大延迟10秒

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // 添加重试监听器
        retryTemplate.registerListener(new org.springframework.retry.RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(org.springframework.retry.RetryContext context,
                                                         org.springframework.retry.RetryCallback<T, E> callback) {
                log.info("开始重试，当前重试次数：{}", context.getRetryCount());
                return true;
            }

            @Override
            public <T, E extends Throwable> void close(org.springframework.retry.RetryContext context,
                                                       org.springframework.retry.RetryCallback<T, E> callback,
                                                       Throwable throwable) {
                log.info("重试结束，总重试次数：{}", context.getRetryCount());
            }

            @Override
            public <T, E extends Throwable> void onError(org.springframework.retry.RetryContext context,
                                                         org.springframework.retry.RetryCallback<T, E> callback,
                                                         Throwable throwable) {
                log.error("重试失败，当前重试次数：{}，异常：{}",
                        context.getRetryCount(), throwable.getMessage());
            }
        });

        return retryTemplate;
    }
}