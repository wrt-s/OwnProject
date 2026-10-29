package com.wrt.excel.system.service;

import com.wrt.excel.system.exception.BusinessException;
import com.wrt.excel.system.exception.RemoteServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 09:55
 * @description: 基础重试服务
 **/
@Service
@Slf4j
public class RetryService {

    private int retryCount = 0;

    /**
     * 同步重试示例
     */
    @Retryable(
            value = {RemoteServiceException.class},//指定需要触发重试的异常类型
            maxAttempts = 3,//最大重试次数
            backoff = @Backoff(delay = 3000, multiplier = 2)//delay第一次重试的延迟时间，单位毫秒，即1秒后第一次重试 multiplier 延迟时间的倍数，每次重试的延迟时间会递增
    )
    public String syncRetryMethod(String param) {
        retryCount++;
        log.info("执行同步重试方法，参数：{}，重试次数：{}，线程：{}",
                param, retryCount, Thread.currentThread().getName());

        // 模拟远程调用失败
        if (retryCount < 3) {
            throw new RemoteServiceException("远程服务调用失败，当前重试次数：" + retryCount);
        }

        retryCount = 0; // 重置计数
        return "success: " + param;
    }

    /**
     * 带恢复方法的重试
     */
    @Retryable(
            value = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public String retryWithRecover(String param) {
        log.info("执行可恢复的重试方法，参数：{}", param);

        if (param == null || param.isEmpty()) {
            throw new BusinessException("参数无效");
        }

        if (param.equals("error")) {
            throw new BusinessException("业务处理失败");
        }

        return "processed: " + param;
    }

    /**
     * 恢复方法
     */
    @Recover
    public String recover(BusinessException e, String param) {
        log.error("重试失败后的恢复处理，参数：{}，异常：{}", param, e.getMessage());
        return "降级返回: " + param;
    }

    public String conditionalRetry(String param) {
        log.info("条件重试方法，参数：{}", param);

        if ("retryable".equals(param)) {
            throw new RuntimeException("可重试的异常");
        } else if ("nonRetryable".equals(param)) {
            throw new IllegalArgumentException("不可重试的异常");
        }

        return "success";
    }
}