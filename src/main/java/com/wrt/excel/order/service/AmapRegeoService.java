package com.wrt.excel.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 高德 Web 逆地理：经纬度 → 位置名称 + 街道/完整地址。
 * 未配置 AMAP_WEB_KEY 时静默降级（返回空，调用方只展示经纬度）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapRegeoService {

    private final ObjectMapper objectMapper;

    @Value("${amap.web-key:}")
    private String webKey;

    @Value("${amap.regeo-url:https://restapi.amap.com/v3/geocode/regeo}")
    private String regeoUrl;

    @Value("${amap.timeout-seconds:8}")
    private int timeoutSeconds;

    public boolean isConfigured() {
        return StringUtils.hasText(webKey);
    }

    /**
     * @return map keys: placeName, street, address；空 map 表示失败/未配置
     */
    public Map<String, String> reverse(double lat, double lng) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!isConfigured()) {
            return out;
        }
        if (Double.isNaN(lat) || Double.isNaN(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            return out;
        }
        try {
            // 高德 location = 经度,纬度
            String location = lng + "," + lat;
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.min(5000, timeoutSeconds * 1000))
                    .responseTimeout(Duration.ofSeconds(timeoutSeconds));
            WebClient client = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
            String body = client.get()
                    .uri(regeoUrl + "?key={key}&location={loc}&extensions=all&radius=1000&homeorcorp=0",
                            webKey, location)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds + 2));
            if (!StringUtils.hasText(body)) {
                return out;
            }
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !"1".equals(text(root.get("status")))) {
                log.warn("高德逆地理失败 status={} info={}", text(root != null ? root.get("status") : null),
                        text(root != null ? root.get("info") : null));
                return out;
            }
            JsonNode regeo = root.get("regeocode");
            if (regeo == null || regeo.isNull()) {
                return out;
            }
            String formatted = text(regeo.get("formatted_address"));
            JsonNode ac = regeo.get("addressComponent");
            String street = null;
            String township = null;
            String neighborhood = null;
            if (ac != null && !ac.isNull()) {
                String road = text(ac.get("street"));
                String number = null;
                JsonNode sn = ac.get("streetNumber");
                if (sn != null && sn.isObject()) {
                    if (!StringUtils.hasText(road)) {
                        road = text(sn.get("street"));
                    }
                    number = text(sn.get("number"));
                } else {
                    number = text(sn);
                }
                street = joinNonEmpty(" ", road, number);
                township = text(ac.get("township"));
                JsonNode nb = ac.get("neighborhood");
                if (nb != null && nb.isObject()) {
                    neighborhood = text(nb.get("name"));
                }
                JsonNode building = ac.get("building");
                if (building != null && building.isObject() && !StringUtils.hasText(neighborhood)) {
                    neighborhood = text(building.get("name"));
                }
            }
            String placeName = null;
            JsonNode pois = regeo.get("pois");
            if (pois != null && pois.isArray() && pois.size() > 0) {
                placeName = text(pois.get(0).get("name"));
            }
            if (!StringUtils.hasText(placeName)) {
                placeName = StringUtils.hasText(neighborhood) ? neighborhood : township;
            }
            if (!StringUtils.hasText(placeName) && StringUtils.hasText(formatted)) {
                placeName = shortName(formatted);
            }
            if (StringUtils.hasText(placeName)) out.put("placeName", placeName);
            if (StringUtils.hasText(street)) out.put("street", street);
            else if (StringUtils.hasText(township)) out.put("street", township);
            if (StringUtils.hasText(formatted)) out.put("address", formatted);
        } catch (Exception e) {
            log.warn("高德逆地理异常: {}", e.toString());
        }
        return out;
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isArray()) {
            if (n.size() == 0) return null;
            return text(n.get(0));
        }
        String s = n.asText(null);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "[]".equals(s)) return null;
        return s;
    }

    private static String joinNonEmpty(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!StringUtils.hasText(p)) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String shortName(String formatted) {
        if (!StringUtils.hasText(formatted)) return null;
        String s = formatted.trim();
        int idx = Math.max(s.lastIndexOf('区'), Math.max(s.lastIndexOf('县'), s.lastIndexOf('市')));
        if (idx >= 0 && idx + 1 < s.length()) {
            return s.substring(idx + 1).trim();
        }
        return s.length() > 24 ? s.substring(0, 24) : s;
    }
}