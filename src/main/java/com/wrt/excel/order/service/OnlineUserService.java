package com.wrt.excel.order.service;

import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 在线用户服务：按 userId 心跳，支持家庭内点单方在线状态查询。
 */
@Slf4j
@Service
public class OnlineUserService {

    /** 心跳超时：约 90 秒无心跳视为离线（关页不调 logout 时也能掉线） */
    private static final int ONLINE_SECONDS = 90;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    private final Map<Long, OnlineInfo> onlineByUser = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> anonymousSessions = new ConcurrentHashMap<>();

    private int totalVisitors = 0;
    private int todayVisitors = 0;
    private String lastResetDate = LocalDateTime.now().toLocalDate().toString();

    @Data
    public static class OnlineInfo {
        private Long userId;
        private String nickname;
        private String role;
        private Long familyId;
        private String sessionId;
        private LocalDateTime lastSeen;
    }

    /** 兼容旧接口：仅 sessionId */
    public void recordVisit(String sessionId) {
        checkAndResetDailyStats();
        if (!anonymousSessions.containsKey(sessionId)) {
            totalVisitors++;
            todayVisitors++;
        }
        anonymousSessions.put(sessionId, LocalDateTime.now());
    }

    /** 登录用户心跳 */
    public void heartbeat(Long userId, String nickname, String role, Long familyId, String sessionId) {
        if (userId == null) {
            if (sessionId != null) recordVisit(sessionId);
            return;
        }
        checkAndResetDailyStats();
        OnlineInfo prev = onlineByUser.get(userId);
        if (prev == null) {
            totalVisitors++;
            todayVisitors++;
        }
        OnlineInfo info = new OnlineInfo();
        info.setUserId(userId);
        info.setNickname(nickname != null ? nickname : ("#" + userId));
        info.setRole(role != null ? role : User.ROLE_ORDERER);
        info.setFamilyId(familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID);
        info.setSessionId(sessionId);
        info.setLastSeen(LocalDateTime.now());
        onlineByUser.put(userId, info);
        if (sessionId != null) {
            anonymousSessions.put(sessionId, LocalDateTime.now());
        }
        log.debug("heartbeat userId={} role={} family={}", userId, role, familyId);
    }

    public void markOffline(Long userId) {
        if (userId == null) return;
        OnlineInfo info = onlineByUser.get(userId);
        if (info != null) {
            // 保留资料，将 lastSeen 推过超时阈值，立即视为离线（关页 sendBeacon）
            info.setLastSeen(LocalDateTime.now().minusSeconds(ONLINE_SECONDS + 1));
        }
    }

    public boolean isOnline(Long userId) {
        OnlineInfo info = onlineByUser.get(userId);
        if (info == null || info.getLastSeen() == null) return false;
        return info.getLastSeen().isAfter(LocalDateTime.now().minusSeconds(ONLINE_SECONDS));
    }

    public int getOnlineCount() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(ONLINE_SECONDS);
        long auth = onlineByUser.values().stream()
                .filter(i -> i.getLastSeen() != null && i.getLastSeen().isAfter(threshold))
                .count();
        long anon = anonymousSessions.values().stream().filter(t -> t.isAfter(threshold)).count();
        return (int) (auth + anon);
    }

    public Map<String, Object> getOnlineStats() {
        checkAndResetDailyStats();
        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineCount", getOnlineCount());
        stats.put("todayVisitors", todayVisitors);
        stats.put("totalVisitors", totalVisitors);
        stats.put("peakOnlineToday", getOnlineCount());
        stats.put("authenticatedOnline", listOnlineUsers().size());
        return stats;
    }

    public List<Map<String, Object>> listOnlineUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(ONLINE_SECONDS);
        return onlineByUser.values().stream()
                .filter(i -> i.getLastSeen() != null && i.getLastSeen().isAfter(threshold))
                .sorted(Comparator.comparing(OnlineInfo::getLastSeen).reversed())
                .map(this::toPublic)
                .collect(Collectors.toList());
    }

    /**
     * 家庭成员在线状态（含离线的点单方，便于做餐方查看）。
     * 优先返回同 familyId 的 ORDERER；若无心跳记录则 online=false。
     */
    public List<Map<String, Object>> getFamilyPartnerStatus(Long familyId, String viewerRole) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(ONLINE_SECONDS);

        // 优先家庭表中的点单方（无心跳也能拿到 userId，供爱心币查询）
        Map<Long, OnlineInfo> ordererMap = new LinkedHashMap<>();
        if (userMapper != null) {
            List<User> familyUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getFamilyId, fid));
            for (User u : familyUsers) {
                if (u == null) continue;
                String r = u.normalizedRole();
                if (!User.ROLE_ORDERER.equals(r)) continue;
                OnlineInfo live = onlineByUser.get(u.getId());
                if (live != null) {
                    ordererMap.put(u.getId(), live);
                } else {
                    OnlineInfo stub = new OnlineInfo();
                    stub.setUserId(u.getId());
                    stub.setNickname(u.getNickname() != null ? u.getNickname() : ("#" + u.getId()));
                    stub.setRole(User.ROLE_ORDERER);
                    stub.setFamilyId(fid);
                    stub.setLastSeen(null);
                    ordererMap.put(u.getId(), stub);
                }
            }
        }
        // 补充仅有心跳、尚无写入/角色未规范化的点单方
        onlineByUser.values().stream()
                .filter(i -> Objects.equals(i.getFamilyId(), fid))
                .filter(i -> {
                    String r = i.getRole() == null ? "" : i.getRole().toUpperCase();
                    return User.ROLE_ORDERER.equals(r) || "USER".equals(r);
                })
                .forEach(i -> ordererMap.putIfAbsent(i.getUserId(), i));

        List<Map<String, Object>> result = new ArrayList<>();
        if (!ordererMap.isEmpty()) {
            for (OnlineInfo info : ordererMap.values()) {
                Map<String, Object> m = toPublic(info);
                boolean online = info.getLastSeen() != null && info.getLastSeen().isAfter(threshold);
                m.put("online", online);
                m.put("status", online ? "online" : "offline");
                result.add(m);
            }
        } else {
            Map<String, Object> placeholder = new HashMap<>();
            placeholder.put("role", User.ROLE_ORDERER);
            placeholder.put("roleLabel", "点单方");
            placeholder.put("online", false);
            placeholder.put("status", "offline");
            placeholder.put("nickname", "点单方");
            placeholder.put("lastSeen", null);
            placeholder.put("lastSeenText", "暂无记录");
            result.add(placeholder);
        }
        return result;
    }

    /** 点单方汇总（做餐方 UI 用）：取同家庭最近活跃的 ORDERER */
    public Map<String, Object> getOrdererStatusSummary(Long familyId) {
        List<Map<String, Object>> list = getFamilyPartnerStatus(familyId, User.ROLE_CHEF);
        Map<String, Object> summary = new HashMap<>();
        if (list.isEmpty()) {
            summary.put("online", false);
            summary.put("status", "offline");
            summary.put("nickname", "点单方");
            summary.put("lastSeen", null);
            summary.put("lastSeenText", "暂无记录");
            summary.put("partners", list);
            return summary;
        }
        // 优先在线的
        Map<String, Object> best = list.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("online")))
                .findFirst()
                .orElse(list.get(0));
        summary.putAll(best);
        summary.put("partners", list);
        return summary;
    }

    private Map<String, Object> toPublic(OnlineInfo info) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", info.getUserId());
        m.put("nickname", info.getNickname());
        String role = info.getRole() != null ? info.getRole() : User.ROLE_ORDERER;
        if ("ADMIN".equalsIgnoreCase(role)) role = User.ROLE_CHEF;
        if ("USER".equalsIgnoreCase(role)) role = User.ROLE_ORDERER;
        m.put("role", role);
        m.put("roleLabel", User.ROLE_CHEF.equals(role) ? "做餐方" : "点单方");
        m.put("familyId", info.getFamilyId());
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(ONLINE_SECONDS);
        boolean online = info.getLastSeen() != null && info.getLastSeen().isAfter(threshold);
        m.put("online", online);
        m.put("status", online ? "online" : "offline");
        m.put("lastSeen", info.getLastSeen() != null ? info.getLastSeen().format(FMT) : null);
        m.put("lastSeenText", formatLastSeen(info.getLastSeen(), online));
        return m;
    }

    private String formatLastSeen(LocalDateTime lastSeen, boolean online) {
        if (lastSeen == null) return "暂无记录";
        if (online) return "在线";
        long minutes = java.time.Duration.between(lastSeen, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "刚刚离开";
        if (minutes < 60) return minutes + " 分钟前";
        long hours = minutes / 60;
        if (hours < 24) return hours + " 小时前";
        return lastSeen.format(FMT);
    }

    public int getTodayVisitors() {
        checkAndResetDailyStats();
        return todayVisitors;
    }

    public int getTotalVisitors() {
        return totalVisitors;
    }

    public void cleanupExpiredSessions() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        onlineByUser.entrySet().removeIf(e -> e.getValue().getLastSeen() == null
                || e.getValue().getLastSeen().isBefore(thirtyMinutesAgo));
        anonymousSessions.entrySet().removeIf(e -> e.getValue().isBefore(thirtyMinutesAgo));
    }

    private void checkAndResetDailyStats() {
        String currentDate = LocalDateTime.now().toLocalDate().toString();
        if (!currentDate.equals(lastResetDate)) {
            todayVisitors = 0;
            lastResetDate = currentDate;
            log.info("重置每日在线统计: {}", currentDate);
        }
    }
}
