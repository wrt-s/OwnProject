package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.OnlineUserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线用户：心跳 + 做餐方查看点单方在线状态
 */
@Slf4j
@RestController
@RequestMapping("/api/online")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private AuthService authService;

    /** 兼容旧调用：匿名/登录均可 */
    @PostMapping("/record")
    public Map<String, Object> recordVisit(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.sessionUserId(session);
            if (userId != null) {
                User user = authService.currentUser(session);
                if (user != null) {
                    onlineUserService.heartbeat(
                            user.getId(),
                            user.getNickname(),
                            user.normalizedRole(),
                            user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID,
                            session.getId()
                    );
                } else {
                    onlineUserService.recordVisit(session.getId());
                }
            } else {
                onlineUserService.recordVisit(session.getId());
            }
            result.put("code", 200);
            result.put("message", "success");
        } catch (Exception e) {
            log.error("记录访问失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 登录后心跳（推荐前端定时调用） */
    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            onlineUserService.heartbeat(
                    user.getId(),
                    user.getNickname(),
                    user.normalizedRole(),
                    user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID,
                    session.getId()
            );
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of("online", true));
        } catch (Exception e) {
            log.error("心跳失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 关页/切后台时前端 sendBeacon 标记离线（不销毁 Session） */
    @PostMapping("/offline")
    public Map<String, Object> markOffline(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.sessionUserId(session);
            if (userId != null) {
                onlineUserService.markOffline(userId);
            }
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of("online", false));
        } catch (Exception e) {
            log.error("标记离线失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 做餐方查看点单方在线状态（同家庭）
     */
    @GetMapping("/partner")
    public Map<String, Object> partnerStatus(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            if (!user.isChef() && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                result.put("code", 403);
                result.put("message", "仅做餐方可查看点单方在线状态");
                return result;
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            Map<String, Object> summary = onlineUserService.getOrdererStatusSummary(fid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", summary);
        } catch (Exception e) {
            log.error("查询点单方在线状态失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 当前家庭在线成员列表（需登录） */
    @GetMapping("/family")
    public Map<String, Object> familyOnline(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = authService.currentUser(session);
            if (user == null) {
                result.put("code", 401);
                result.put("message", "未登录");
                return result;
            }
            Long fid = user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID;
            List<Map<String, Object>> list = onlineUserService.getFamilyPartnerStatus(fid, user.normalizedRole());
            // 同时附带全部在线（同家庭）
            List<Map<String, Object>> allOnline = onlineUserService.listOnlineUsers().stream()
                    .filter(m -> ObjectsEqualsFamily(m.get("familyId"), fid))
                    .toList();
            Map<String, Object> data = new HashMap<>();
            data.put("orderers", list);
            data.put("onlineMembers", allOnline);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);
        } catch (Exception e) {
            log.error("查询家庭在线失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private static boolean ObjectsEqualsFamily(Object a, Long fid) {
        if (a == null) return fid == null || AuthService.DEFAULT_FAMILY_ID.equals(fid);
        try {
            return Long.valueOf(String.valueOf(a)).equals(fid);
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getOnlineStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", onlineUserService.getOnlineStats());
        } catch (Exception e) {
            log.error("获取在线统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/count")
    public Map<String, Object> getOnlineCount() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", onlineUserService.getOnlineCount());
        } catch (Exception e) {
            log.error("获取在线人数失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
