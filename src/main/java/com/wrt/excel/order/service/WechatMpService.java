package com.wrt.excel.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrt.excel.order.config.WechatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WeChat MP test-account: access_token cache + template message push.
 * Failures are logged only; callers must not let exceptions block order flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long TOKEN_REFRESH_SKEW_MS = 5 * 60 * 1000L;

    private final WechatProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public boolean isConfigured() {
        return props.isEnabled()
                && notBlank(props.getAppId())
                && notBlank(props.getAppSecret())
                && notBlank(props.getChefOpenid())
                && notBlank(props.getOrdererOpenid())
                && notBlank(props.getTemplateNewOrder())
                && notBlank(props.getTemplateStatus());
    }

    /** New order -> notify chef (做餐方). */
    public void notifyNewOrder(String orderNo, String userLabel, String dishSummary,
                               String priceText, String remark) {
        if (!isConfigured()) {
            log.debug("WeChat MP not configured, skip new-order template");
            return;
        }
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", kv(safe(userLabel, "点单方")));
            data.put("dish", kv(safe(dishSummary, "-")));
            data.put("price", kv(safe(priceText, "-")));
            data.put("time", kv(now()));
            data.put("remark", kv(safe(remark, "新订单 " + nullToEmpty(orderNo))));
            sendTemplate(props.getChefOpenid(), props.getTemplateNewOrder(), data);
        } catch (Exception e) {
            log.error("WeChat new-order template failed (ignored): {}", e.getMessage());
        }
    }

    /** Status change -> notify orderer; on cancel also notify chef. */
    public void notifyStatusChange(String orderNo, String statusCode, String dishSummary, String remark,
                                   boolean cancelled) {
        if (!isConfigured()) {
            log.debug("WeChat MP not configured, skip status template");
            return;
        }
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", kv(statusText(statusCode)));
            data.put("dish", kv(safe(dishSummary, "-")));
            data.put("time", kv(now()));
            data.put("remark", kv(safe(remark, "订单 " + nullToEmpty(orderNo))));
            sendTemplate(props.getOrdererOpenid(), props.getTemplateStatus(), data);
            if (cancelled && notBlank(props.getChefOpenid())) {
                sendTemplate(props.getChefOpenid(), props.getTemplateStatus(), data);
            }
        } catch (Exception e) {
            log.error("WeChat status template failed (ignored): {}", e.getMessage());
        }
    }

    public String statusText(String status) {
        if (status == null) return "未知";
        switch (status.trim().toLowerCase()) {
            case "pending":
            case "confirmed":
            case "received":
                return "待接单";
            case "cooking":
            case "preparing":
                return "制作中";
            case "ready":
            case "delivered":
                return "待取餐";
            case "completed":
                return "已完成";
            case "cancelled":
            case "canceled":
                return "已取消";
            default:
                return status;
        }
    }

    private void sendTemplate(String openid, String templateId, Map<String, Object> data) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        String respBody = resp.getBody();
        JsonNode node = objectMapper.readTree(respBody == null ? "{}" : respBody);
        int errcode = node.path("errcode").asInt(0);
        if (errcode == 40001 || errcode == 42001) {
            cachedToken.set(null);
            accessToken = getAccessToken();
            url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
            resp = restTemplate.postForEntity(url, entity, String.class);
            respBody = resp.getBody();
            node = objectMapper.readTree(respBody == null ? "{}" : respBody);
            errcode = node.path("errcode").asInt(0);
        }
        if (errcode != 0) {
            log.warn("WeChat template send failed errcode={} errmsg={} openidTail={}",
                    errcode, node.path("errmsg").asText(""), tail(openid));
        } else {
            log.info("WeChat template sent ok msgid={} openidTail={}",
                    node.path("msgid").asText(""), tail(openid));
        }
    }

    private synchronized String getAccessToken() throws Exception {
        CachedToken cur = cachedToken.get();
        long nowMs = System.currentTimeMillis();
        if (cur != null && cur.expireAtMs - TOKEN_REFRESH_SKEW_MS > nowMs && notBlank(cur.token)) {
            return cur.token;
        }
        if (!notBlank(props.getAppId()) || !notBlank(props.getAppSecret())) {
            throw new IllegalStateException("WeChat appId/appSecret missing");
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + props.getAppId()
                + "&secret=" + props.getAppSecret();
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        JsonNode node = objectMapper.readTree(resp.getBody() == null ? "{}" : resp.getBody());
        if (node.has("errcode") && node.get("errcode").asInt() != 0) {
            throw new IllegalStateException("getAccessToken failed: " + node.path("errmsg").asText());
        }
        String token = node.path("access_token").asText(null);
        int expiresIn = node.path("expires_in").asInt(7200);
        if (!notBlank(token)) {
            throw new IllegalStateException("empty access_token");
        }
        cachedToken.set(new CachedToken(token, nowMs + expiresIn * 1000L));
        log.info("WeChat access_token refreshed, expiresIn={}s", expiresIn);
        return token;
    }

    private static Map<String, String> kv(String value) {
        Map<String, String> m = new LinkedHashMap<>();
        String v = value == null ? "" : value;
        if (v.length() > 200) {
            v = v.substring(0, 197) + "...";
        }
        m.put("value", v);
        return m;
    }

    private static String now() {
        return LocalDateTime.now().format(TIME_FMT);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String safe(String s, String fallback) {
        return notBlank(s) ? s.trim() : fallback;
    }

    private static String tail(String openid) {
        if (!notBlank(openid) || openid.length() < 6) return "?";
        return openid.substring(openid.length() - 6);
    }

    private static final class CachedToken {
        final String token;
        final long expireAtMs;
        CachedToken(String token, long expireAtMs) {
            this.token = token;
            this.expireAtMs = expireAtMs;
        }
    }
}
