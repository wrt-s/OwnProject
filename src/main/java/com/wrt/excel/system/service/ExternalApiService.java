package com.wrt.excel.system.service;

import com.wrt.excel.system.exception.ApiException;
import com.wrt.excel.system.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 10:05
 * @description: 模拟外部API服务
 **/
@Service
@Slf4j
public class ExternalApiService {

    private final Random random = new Random();
    private final AtomicInteger callCount = new AtomicInteger(0);

    /**
     * 模拟调用外部API
     */
    public String callExternalApi(String requestId) {
        int currentCount = callCount.incrementAndGet();
        log.info("调用外部API，请求ID：{}，第{}次调用", requestId, currentCount);

        // 模拟网络延迟
        try {
            int delay = random.nextInt(500);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断", e);
        }

        // 模拟随机失败（40%的失败率，用于演示重试）
        int randomNum = random.nextInt(10);
        if (randomNum < 4) {
            log.error("外部API调用失败，请求ID：{}，随机数：{}", requestId, randomNum);
            throw new ApiException("外部API调用失败，请求ID：" + requestId);
        }

        // 模拟成功返回
        String result = String.format("成功 - 请求ID: %s, 时间: %d, 调用次数: %d",
                requestId, System.currentTimeMillis(), currentCount);
        log.info("外部API调用成功，请求ID：{}，结果：{}", requestId, result);
        return result;
    }

    /**
     * 处理单个项目
     */
    public String processItem(String item) {
        log.info("处理项目：{}", item);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断", e);
        }

        // 模拟处理失败
        if ("error".equals(item) || "fail".equals(item)) {
            throw new BusinessException("处理项目失败：" + item);
        }

        return "处理成功：" + item;
    }

    /**
     * 重置调用计数
     */
    public void resetCallCount() {
        callCount.set(0);
        log.info("重置API调用计数");
    }
}