package com.wrt.excel.order.controller;

import com.wrt.excel.order.config.WechatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * WeChat MP test-account server callback (URL verification + message receive stub).
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat")
@RequiredArgsConstructor
public class WechatCallbackController {

    private final WechatProperties props;

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@RequestParam(value = "signature", required = false) String signature,
                         @RequestParam(value = "timestamp", required = false) String timestamp,
                         @RequestParam(value = "nonce", required = false) String nonce,
                         @RequestParam(value = "echostr", required = false) String echostr) {
        if (!checkSignature(signature, timestamp, nonce)) {
            log.warn("WeChat callback GET signature invalid");
            return "invalid";
        }
        return echostr == null ? "" : echostr;
    }

    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receive(@RequestParam(value = "signature", required = false) String signature,
                          @RequestParam(value = "timestamp", required = false) String timestamp,
                          @RequestParam(value = "nonce", required = false) String nonce,
                          @RequestBody(required = false) String body) {
        if (!checkSignature(signature, timestamp, nonce)) {
            log.warn("WeChat callback POST signature invalid");
            return "invalid";
        }
        // Test account may push events; acknowledge without blocking.
        if (StringUtils.hasText(body)) {
            log.info("WeChat callback POST received, bytes={}", body.getBytes(StandardCharsets.UTF_8).length);
        }
        return "success";
    }

    private boolean checkSignature(String signature, String timestamp, String nonce) {
        String token = props.getToken();
        if (!StringUtils.hasText(token) || !StringUtils.hasText(signature)
                || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            return false;
        }
        try {
            String[] arr = new String[]{token, timestamp, nonce};
            Arrays.sort(arr);
            String raw = arr[0] + arr[1] + arr[2];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("WeChat signature check error: {}", e.getMessage());
            return false;
        }
    }
}
