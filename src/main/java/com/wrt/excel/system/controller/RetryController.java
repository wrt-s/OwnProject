package com.wrt.excel.system.controller;

import com.wrt.excel.system.service.AsyncRetryService;
import com.wrt.excel.system.service.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 10:08
 * @description: 重试控制器
 **/
@RestController
@RequestMapping("/api/retry")
@Slf4j
public class RetryController {

    @Autowired
    private RetryService retryService;

    @Autowired
    private AsyncRetryService asyncRetryService;

    /**
     * 测试同步重试
     */
    @GetMapping("/sync/{param}")
    public String testSyncRetry(@PathVariable String param) {
        log.info("接收到同步重试请求，参数：{}", param);
        return retryService.syncRetryMethod(param);
    }

    /**
     * 测试带恢复的重试
     */
    @GetMapping("/recover/{param}")
    public String testRecoverRetry(@PathVariable String param) {
        log.info("接收到可恢复重试请求，参数：{}", param);
        return retryService.retryWithRecover(param);
    }

    /**
     * 测试条件重试
     */
    @GetMapping("/conditional/{param}")
    public String testConditionalRetry(@PathVariable String param) {
        log.info("接收到条件重试请求，参数：{}", param);
        return retryService.conditionalRetry(param);
    }

    /**
     * 测试异步重试
     */
    @GetMapping("/async/{requestId}")
    public CompletableFuture<String> testAsyncRetry(@PathVariable String requestId) {
        log.info("接收到异步重试请求，请求ID：{}", requestId);
        return asyncRetryService.asyncRetryWithFuture(requestId);
    }

    /**
     * 测试批量异步处理
     */
    @PostMapping("/batch")
    public CompletableFuture<List<String>> testBatchProcess(@RequestBody List<String> items) {
        log.info("接收到批量处理请求，项目数量：{}", items.size());
        return asyncRetryService.batchProcess(items);
    }

    /**
     * 测试带超时的异步重试
     */
    @GetMapping("/async/timeout/{requestId}")
    public CompletableFuture<String> testAsyncRetryWithTimeout(@PathVariable String requestId) {
        log.info("接收到带超时的异步重试请求，请求ID：{}", requestId);
        return asyncRetryService.asyncRetryWithTimeout(requestId);
    }

    /**
     * 并发测试
     */
    @GetMapping("/concurrent")
    public String testConcurrent() {
        log.info("开始并发测试");

        long startTime = System.currentTimeMillis();

        // 同时发起10个异步请求
        List<CompletableFuture<String>> futures = Arrays.asList(
                asyncRetryService.asyncRetryWithFuture("req-1"),
                asyncRetryService.asyncRetryWithFuture("req-2"),
                asyncRetryService.asyncRetryWithFuture("req-3"),
                asyncRetryService.asyncRetryWithFuture("req-4"),
                asyncRetryService.asyncRetryWithFuture("req-5"),
                asyncRetryService.asyncRetryWithFuture("req-6"),
                asyncRetryService.asyncRetryWithFuture("req-7"),
                asyncRetryService.asyncRetryWithFuture("req-8"),
                asyncRetryService.asyncRetryWithFuture("req-9"),
                asyncRetryService.asyncRetryWithFuture("req-10")
        );

        // 等待所有请求完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        long endTime = System.currentTimeMillis();
        log.info("并发测试完成，耗时：{}ms", endTime - startTime);

        return "并发测试完成，耗时：" + (endTime - startTime) + "ms";
    }
}