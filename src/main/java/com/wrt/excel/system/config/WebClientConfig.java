package com.wrt.excel.system.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 14:43
 * @description:
 **/
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // 配置 Reactor Netty HttpClient
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.example.com")
                .build();
    }
}
