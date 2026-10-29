package com.wrt.excel.system.service;

import com.google.common.util.concurrent.RateLimiter;
import com.wrt.excel.system.config.RateLimiterConfig;
import com.wrt.excel.system.interfance.RateLimit;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 14:29
 * @description: 限流-省平台接口调用
 **/
@Service
@Slf4j
public class ProvincialApiService {

    @Autowired
    private RateLimiterConfig.RateLimiterFactory rateLimiterFactory;

    // 方式1：使用限流器工厂
    private RateLimiter uploadLimiter;

    @PostConstruct
    public void init() {
        uploadLimiter = rateLimiterFactory.getRateLimiter("upload-api", 5);
    }

    /**
     * 方式1：手动控制限流
     */
    public String uploadReport1(Object params) {
        if (!uploadLimiter.tryAcquire()) {
            log.warn("Upload API rate limit exceeded");
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }

        return callProvincialApi("/report/upload", params);
    }

    /**
     * 方式3：使用注解（推荐）
     */
    @RateLimit(value = 5, timeout = 100)
    public String uploadReport3(Object params) {
        return callProvincialApi("/report/upload", params);
    }

    /**
     * 动态限流示例（根据用户ID）
     */
    @RateLimit(value = 10, key = "#userId")
    public String uploadReportByUser(String userId, Object params) {
        return callProvincialApi("/report/upload", params);
    }

    /**
     * 调用省平台接口
     */
    private String callProvincialApi(String path, Object params) {
        String url = "http://www.baidu.com" + path;
        try {
            log.info("Calling provincial API: {}", url);
            // 创建 HttpClient 配置连接超时和响应超时
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(10));

            // 创建 WebClient
            WebClient webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();


            // 发送 POST 请求并阻塞等待结果
            return webClient.post()
                    .uri(url)
                    .bodyValue(params)  // 使用 params 参数
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();  // 阻塞直到返回结果

        } catch (Exception e) {
            log.error("Call provincial API failed", e);
            throw new RuntimeException("调用省平台接口失败");
        }
    }
}