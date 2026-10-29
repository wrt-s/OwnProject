package com.wrt.excel.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WeChat MP test-account config (secrets via ENV).
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {
    private boolean enabled = true;
    private String appId = "";
    private String appSecret = "";
    private String token = "";
    private String chefOpenid = "";
    private String ordererOpenid = "";
    private String templateNewOrder = "";
    private String templateStatus = "";
}
