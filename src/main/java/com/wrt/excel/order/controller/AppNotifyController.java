package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.AppDevice;
import com.wrt.excel.order.entity.AppNotification;
import com.wrt.excel.order.service.AppNotifyService;
import com.wrt.excel.order.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App 设备登记 + 站内通知列表/轮询（需登录 Session）。
 */
@Slf4j
@RestController
@RequestMapping("/api/app")
public class AppNotifyController {

    @Autowired
    private AppNotifyService appNotifyService;

    @PostMapping("/device/register")
    public Map<String, Object> registerDevice(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            String deviceId = str(body, "deviceId");
            String platform = str(body, "platform");
            String appVersion = str(body, "appVersion");
            AppDevice d = appNotifyService.registerDevice(uid, deviceId, platform, appVersion);
            result.put("code", 200);
            result.put("data", d);
            result.put("message", "ok");
        } catch (Exception e) {
            log.warn("device register failed: {}", e.getMessage());
            result.put("code", 400);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/notifications")
    public Map<String, Object> list(@RequestParam(required = false, defaultValue = "30") Integer limit,
                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            List<AppNotification> list = appNotifyService.listRecent(uid, limit == null ? 30 : limit);
            result.put("code", 200);
            result.put("data", list);
            result.put("unread", appNotifyService.unreadCount(uid));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Object> unreadCount(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            result.put("code", 200);
            result.put("unread", appNotifyService.unreadCount(uid));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/notifications/poll")
    public Map<String, Object> poll(@RequestParam(required = false, defaultValue = "0") Long afterId,
                                    @RequestParam(required = false, defaultValue = "20") Integer limit,
                                    @RequestParam(required = false) String deviceId,
                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            if (deviceId != null && !deviceId.isBlank()) {
                appNotifyService.touchDevice(uid, deviceId);
            }
            List<AppNotification> list = appNotifyService.pollAfter(uid, afterId, limit == null ? 20 : limit);
            result.put("code", 200);
            result.put("data", list);
            result.put("unread", appNotifyService.unreadCount(uid));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/notifications/read")
    public Map<String, Object> markRead(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Object raw = body == null ? null : body.get("ids");
            List<Long> ids = new java.util.ArrayList<>();
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    if (o == null) continue;
                    try {
                        ids.add(Long.valueOf(String.valueOf(o)));
                    } catch (Exception ignored) {}
                }
            }
            int n = appNotifyService.markRead(uid, ids);
            result.put("code", 200);
            result.put("updated", n);
            result.put("unread", appNotifyService.unreadCount(uid));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllRead(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            int n = appNotifyService.markAllRead(uid);
            result.put("code", 200);
            result.put("updated", n);
            result.put("unread", 0);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return null;
        return String.valueOf(body.get(key));
    }
}
