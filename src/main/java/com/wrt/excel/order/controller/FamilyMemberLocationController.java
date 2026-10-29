package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.FamilyMemberLocation;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.FamilyMemberLocationService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 家人位置 API
 * - POST /api/family-location/report  任意已登录角色上报自己位置
 * - GET  /api/family-location/list    仅 CHEF 可看家庭列表
 */
@Slf4j
@RestController
@RequestMapping("/api/family-location")
public class FamilyMemberLocationController {

    @Autowired
    private FamilyMemberLocationService locationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.wrt.excel.order.service.AmapRegeoService amapRegeoService;

    /**
     * 上报/更新自己的位置（点单方与做餐方均可，静默调用）
     * body: { lat, lng, accuracy? }
     */
    @PostMapping("/report")
    public Map<String, Object> report(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "请先登录");
                return result;
            }
            Double lat = toDouble(body != null ? body.get("lat") : null);
            Double lng = toDouble(body != null ? body.get("lng") : null);
            Double accuracy = toDouble(body != null ? body.get("accuracy") : null);
            if (lat == null || lng == null) {
                result.put("code", 400);
                result.put("message", "缺少经纬度");
                return result;
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            FamilyMemberLocation before = locationService.getByFamilyUser(fid, user.getId());
            FamilyMemberLocationService.UpsertOutcome outcome = locationService.upsertWithStatus(
                    user.getId(), fid, user.getNickname(), user.normalizedRole(),
                    lat, lng, accuracy);
            FamilyMemberLocation saved = outcome.location;
            boolean changed = outcome.coordsChanged;
            if (!changed && before != null && saved != null
                    && before.getLat() != null && before.getLng() != null
                    && saved.getLat() != null && saved.getLng() != null) {
                double meters = FamilyMemberLocationService.haversineMeters(
                        before.getLat(), before.getLng(), saved.getLat(), saved.getLng());
                changed = meters >= 8.0;
            } else if (changed && before != null && before.getLat() != null && before.getLng() != null
                    && saved != null && saved.getLat() != null && saved.getLng() != null) {
                double meters = FamilyMemberLocationService.haversineMeters(
                        before.getLat(), before.getLng(), saved.getLat(), saved.getLng());
                if (meters < 8.0) changed = false;
            }
            String locStatus = outcome.locStatus != null ? outcome.locStatus : (changed ? "updated" : "unchanged");
            String message = "位置已更新";
            if ("coarse_rejected".equals(locStatus)) {
                message = "已拒绝粗略定位，保留上次精确位置";
            } else if ("stale_good".equals(locStatus)) {
                message = "沿用精确位置";
            } else if (!changed || "unchanged".equals(locStatus)) {
                message = "位置未变";
            }
            result.put("code", 200);
            result.put("message", message);
            result.put("changed", changed);
            result.put("locStatus", locStatus);
            result.put("data", saved);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("上报家人位置失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * CHEF 查看家庭成员位置列表。
     * 可选 query: originLat, originLng（刷新时用刚拿到的自己坐标算距离）
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) Double originLat,
                                    @RequestParam(required = false) Double originLng,
                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "请先登录");
                return result;
            }
            if (!user.isChef()) {
                result.put("code", 403);
                result.put("message", "仅做餐方可查看家人位置");
                return result;
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            List<Map<String, Object>> list = locationService.listFamilyForChef(
                    fid, user.getId(), originLat, originLng);
            Map<String, Object> data = new HashMap<>();
            data.put("members", list);
            data.put("selfUserId", user.getId());
            data.put("familyId", fid);
            data.put("originLat", originLat);
            data.put("originLng", originLng);
            // 列表拉取时刻（Asia/Shanghai），便于 UI 标明「刚刚从服务器拉到的最新」
            String fetchedAt = java.time.LocalDateTime.now(FamilyMemberLocationService.ZONE)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            data.put("fetchedAt", fetchedAt);
            data.put("amapConfigured", amapRegeoService != null && amapRegeoService.isConfigured());
            data.put("note", "刷新会拉取全员在服务端的最新上报；无法远程强制对方手机重新定位。");
            result.put("code", 200);
            result.put("data", data);
            result.put("count", list.size());
        } catch (Exception e) {
            log.error("获取家人位置列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}