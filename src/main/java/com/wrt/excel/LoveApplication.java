package com.wrt.excel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry      // 启用重试功能
@EnableAsync      // 启用异步功能
@EnableScheduling // 启用定时任务功能
@SpringBootApplication
public class LoveApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoveApplication.class, args);
    }

}
