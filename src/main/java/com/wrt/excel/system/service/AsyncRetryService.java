package com.wrt.excel.system.service;

import com.wrt.excel.system.exception.ApiException;
import com.wrt.excel.system.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 09:56
 * @description: 异步重试服务
 **/
@Service
@Slf4j
public class AsyncRetryService {

    @Autowired
    private ExternalApiService externalApiService;

    /**
     * 异步方法 + 重试
     * 使用自定义线程池
     */
    @Async("customExecutor")
    @Retryable(
            value = {ApiException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000)
    )
    public CompletableFuture<String> asyncRetryWithFuture(String requestId) {
        log.info("异步重试方法执行，请求ID：{}，线程：{}",
                requestId, Thread.currentThread().getName());

        try {
            // 调用外部API
            String result = externalApiService.callExternalApi(requestId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("API调用失败，请求ID：{}", requestId, e);
            throw new ApiException("API调用失败", e);
        }
    }

    /**
     * 异步方法的恢复处理
     */
    @Recover
    public CompletableFuture<String> recover(ApiException e, String requestId) {
        log.error("异步重试最终失败，请求ID：{}，异常：{}", requestId, e.getMessage());
        return CompletableFuture.completedFuture("降级处理_" + requestId);
    }

    /**
     * 批量异步处理
     */
    @Async("customExecutor")
    @Retryable(
            value = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public CompletableFuture<List<String>> batchProcess(List<String> items) {
        log.info("批量处理开始，数量：{}，线程：{}", items.size(), Thread.currentThread().getName());

        List<String> results = new ArrayList<>();
        for (String item : items) {
            try {
                String result = externalApiService.processItem(item);
                results.add(result);
            } catch (Exception e) {
                log.error("处理项目失败：{}", item, e);
                throw new BusinessException("批量处理失败", e);
            }
        }

        return CompletableFuture.completedFuture(results);
    }

    /**
     * 带超时控制的异步重试
     */
    @Async("customExecutor")
    @Retryable(
            value = {ApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public CompletableFuture<String> asyncRetryWithTimeout(String requestId) {
        log.info("带超时控制的异步重试，请求ID：{}", requestId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟耗时操作
                TimeUnit.SECONDS.sleep(2);
                return externalApiService.callExternalApi(requestId);
            } catch (Exception e) {
                throw new ApiException("超时或失败", e);
            }
        }).orTimeout(3, TimeUnit.SECONDS);
    }
}