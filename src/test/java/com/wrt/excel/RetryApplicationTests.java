package com.wrt.excel;

import com.wrt.excel.system.service.AsyncRetryService;
import com.wrt.excel.system.service.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
class RetryApplicationTests {

    @Autowired
    private RetryService retryService;

    @Autowired
    private AsyncRetryService asyncRetryService;

    @Test
    void testSyncRetry() {
        String result = retryService.syncRetryMethod("test");
        log.info("同步重试结果：{}", result);
    }

    @Test
    void testRecoverRetry() {
        String result = retryService.retryWithRecover("error");
        log.info("可恢复重试结果：{}", result);
    }

    @Test
    void testAsyncRetry() throws Exception {
        // 1. 调用异步重试方法，返回一个 CompletableFuture 对象
        CompletableFuture<String> future = asyncRetryService.asyncRetryWithFuture("test-001");
        // 2. 阻塞等待异步任务完成，最多等待30秒
        String result = future.get(30, TimeUnit.SECONDS);
        // 3. 打印结果
        log.info("异步重试结果：{}", result);
    }

    @Test
    void testBatchProcess() throws Exception {
        List<String> items = Arrays.asList("item1", "item2", "item3", "error", "item5");

        CompletableFuture<List<String>> future = asyncRetryService
                .batchProcess(items);

        List<String> results = future.get(30, TimeUnit.SECONDS);
        log.info("批量处理结果：{}", results);
    }
}