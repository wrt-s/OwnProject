package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.FamilyMemberLocation;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.FamilyMemberLocationMapper;
import com.wrt.excel.order.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 家人位置：上报 upsert + CHEF 家庭列表（含相对距离）
 */
@Slf4j
@Service
public class FamilyMemberLocationService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final double EARTH_RADIUS_M = 6371000.0;

    /** Prefer keeping a recent GPS-quality fix over coarse network (~100-150m). */
    private static final double GOOD_ACCURACY_M = 50.0;
    private static final double COARSE_REJECT_M = 100.0;
    private static final long GOOD_GPS_KEEP_MINUTES = 10;

    /** Result of upsert with locStatus for H5 toast. */
    public static class UpsertOutcome {
        public final FamilyMemberLocation location;
        public final String locStatus; // updated | unchanged | stale_good | coarse_rejected
        public final boolean coordsChanged;
        public UpsertOutcome(FamilyMemberLocation location, String locStatus, boolean coordsChanged) {
            this.location = location;
            this.locStatus = locStatus;
            this.coordsChanged = coordsChanged;
        }
    }

    @Autowired
    private FamilyMemberLocationMapper locationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmapRegeoService amapRegeoService;

    public FamilyMemberLocation getByFamilyUser(Long familyId, Long userId) {
        if (familyId == null || userId == null) return null;
        return locationMapper.selectOne(new LambdaQueryWrapper<FamilyMemberLocation>()
                .eq(FamilyMemberLocation::getFamilyId, familyId)
                .eq(FamilyMemberLocation::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * Upsert 当前用户最新位置
     */
    public FamilyMemberLocation upsert(Long userId, Long familyId, String nickname, String role,
                                       double lat, double lng, Double accuracy) {
        return upsertWithStatus(userId, familyId, nickname, role, lat, lng, accuracy).location;
    }

    public UpsertOutcome upsertWithStatus(Long userId, Long familyId, String nickname, String role,
                                          double lat, double lng, Double accuracy) {
        if (userId == null) throw new IllegalArgumentException("用户未登录");
        if (Double.isNaN(lat) || Double.isNaN(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw new IllegalArgumentException("经纬度无效");
        }
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        LocalDateTime now = LocalDateTime.now(ZONE);
        FamilyMemberLocation existing = getByFamilyUser(fid, userId);
        if (existing == null) {
            FamilyMemberLocation row = new FamilyMemberLocation();
            row.setFamilyId(fid);
            row.setUserId(userId);
            row.setNickname(nickname);
            row.setRole(role);
            row.setLat(lat);
            row.setLng(lng);
            row.setAccuracy(accuracy);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            fillAddress(row, lat, lng);
            locationMapper.insert(row);
            return new UpsertOutcome(row, "updated", true);
        }
        existing.setNickname(nickname);
        existing.setRole(role);
        // Accuracy fusion: prefer recent good GPS; never let coarse network overwrite it
        Double fusedLat = lat;
        Double fusedLng = lng;
        Double fusedAcc = accuracy;
        Double oldLat = existing.getLat();
        Double oldLng = existing.getLng();
        Double oldAcc = existing.getAccuracy();
        boolean hadAddress = org.springframework.util.StringUtils.hasText(existing.getPlaceName())
                || org.springframework.util.StringUtils.hasText(existing.getAddress());
        double moved = (oldLat != null && oldLng != null)
                ? haversineMeters(oldLat, oldLng, lat, lng) : Double.POSITIVE_INFINITY;
        LocalDateTime oldUpdated = existing.getUpdatedAt();
        boolean recentGoodGps = oldLat != null && oldLng != null
                && oldAcc != null && oldAcc > 0 && oldAcc <= GOOD_ACCURACY_M
                && oldUpdated != null
                && Duration.between(oldUpdated, now).toMinutes() <= GOOD_GPS_KEEP_MINUTES;
        boolean newCoarse = accuracy == null || accuracy <= 0 || accuracy > COARSE_REJECT_M;
        String locStatus = "updated";
        if (recentGoodGps && newCoarse) {
            // Keep previous good GPS; only refresh nickname/role — do not bump updatedAt
            log.info("keep recent good GPS userId={} oldAcc={} newAcc={} moved={}",
                    userId, oldAcc, accuracy, Math.round(moved));
            locationMapper.updateById(existing);
            return new UpsertOutcome(existing, "coarse_rejected", false);
        }
        if (oldLat != null && oldLng != null && oldAcc != null && accuracy != null
                && oldAcc > 0 && accuracy > 0
                && accuracy > COARSE_REJECT_M && oldAcc <= GOOD_ACCURACY_M) {
            // Always reject coarse overwrite of good GPS (even if far — 800m bias case)
            fusedLat = oldLat;
            fusedLng = oldLng;
            fusedAcc = oldAcc;
            locStatus = "coarse_rejected";
        } else if (oldLat != null && oldLng != null && oldAcc != null && accuracy != null
                && oldAcc > 0 && accuracy > 0
                && accuracy <= GOOD_ACCURACY_M
                && moved < Math.max(80.0, oldAcc + accuracy)) {
            // Both decent: inverse-accuracy blend (GPS-quality only)
            double wOld = 1.0 / Math.max(oldAcc, 1.0);
            double wNew = 1.0 / Math.max(accuracy, 1.0);
            double wSum = wOld + wNew;
            fusedLat = (oldLat * wOld + lat * wNew) / wSum;
            fusedLng = (oldLng * wOld + lng * wNew) / wSum;
            fusedAcc = 1.0 / Math.sqrt(wOld * wOld + wNew * wNew);
            locStatus = "updated";
        } else if (oldLat != null && oldLng != null && oldAcc != null && accuracy != null
                && accuracy > oldAcc * 1.8 && moved < Math.max(25.0, oldAcc)) {
            // Much worse new fix within old accuracy bubble → keep previous coords
            fusedLat = oldLat;
            fusedLng = oldLng;
            fusedAcc = oldAcc;
            locStatus = "unchanged";
        }
        boolean coordsKept = oldLat != null && fusedLat != null && fusedLng != null
                && Math.abs(fusedLat - oldLat) < 1e-9 && Math.abs(fusedLng - oldLng) < 1e-9
                && Objects.equals(fusedAcc, oldAcc);
        existing.setLat(fusedLat);
        existing.setLng(fusedLng);
        existing.setAccuracy(fusedAcc);
        if (!coordsKept) {
            existing.setUpdatedAt(now);
            if (!"coarse_rejected".equals(locStatus)) locStatus = "updated";
        } else if (!"coarse_rejected".equals(locStatus)) {
            locStatus = "unchanged";
        }
        // Skip reverse-geocode when we kept coords / already have address and small move
        if (!coordsKept && !(hadAddress && moved < 60.0)) {
            fillAddress(existing, fusedLat, fusedLng);
        }
        locationMapper.updateById(existing);
        return new UpsertOutcome(existing, locStatus, !coordsKept);
    }

    /** 高德逆地理写入 placeName/street/address；无 Key 或失败时清空旧文案避免误导 */
    private void fillAddress(FamilyMemberLocation row, double lat, double lng) {
        if (row == null) return;
        if (amapRegeoService == null || !amapRegeoService.isConfigured()) {
            // 无 Key：保留已有地址（若有），不报错
            return;
        }
        Map<String, String> geo = amapRegeoService.reverse(lat, lng);
        if (geo == null || geo.isEmpty()) {
            return;
        }
        if (StringUtils.hasText(geo.get("placeName"))) row.setPlaceName(geo.get("placeName"));
        if (StringUtils.hasText(geo.get("street"))) row.setStreet(geo.get("street"));
        if (StringUtils.hasText(geo.get("address"))) row.setAddress(geo.get("address"));
    }

    /** 列表时若缺地址且有 Key，补一次并落库 */
    private void ensureAddress(FamilyMemberLocation loc) {
        if (loc == null || loc.getLat() == null || loc.getLng() == null) return;
        if (StringUtils.hasText(loc.getPlaceName()) || StringUtils.hasText(loc.getAddress())) return;
        if (amapRegeoService == null || !amapRegeoService.isConfigured()) return;
        fillAddress(loc, loc.getLat(), loc.getLng());
        if (StringUtils.hasText(loc.getPlaceName()) || StringUtils.hasText(loc.getAddress()) || StringUtils.hasText(loc.getStreet())) {
            try {
                locationMapper.updateById(loc);
            } catch (Exception e) {
                log.warn("回填地址失败 userId={}: {}", loc.getUserId(), e.toString());
            }
        }
    }

    public List<User> listFamilyUsers(Long familyId) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, fid)
                .orderByAsc(User::getId));
    }

    /**
     * CHEF 家庭成员位置列表。originLat/Lng 优先用调用方刚刷新的坐标，否则用库中 CHEF 最新点。
     */
    public List<Map<String, Object>> listFamilyForChef(Long familyId, Long chefUserId,
                                                      Double originLat, Double originLng) {
        Long fid = familyId != null ? familyId : AuthService.DEFAULT_FAMILY_ID;
        List<User> users = listFamilyUsers(fid);
        List<FamilyMemberLocation> locs = locationMapper.selectList(new LambdaQueryWrapper<FamilyMemberLocation>()
                .eq(FamilyMemberLocation::getFamilyId, fid));
        Map<Long, FamilyMemberLocation> byUser = new HashMap<>();
        if (locs != null) {
            for (FamilyMemberLocation loc : locs) {
                if (loc != null && loc.getUserId() != null) {
                    byUser.put(loc.getUserId(), loc);
                }
            }
        }

        Double oLat = originLat;
        Double oLng = originLng;
        if (oLat == null || oLng == null) {
            FamilyMemberLocation selfLoc = byUser.get(chefUserId);
            if (selfLoc != null) {
                oLat = selfLoc.getLat();
                oLng = selfLoc.getLng();
            }
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            String nick = u.getNickname() != null ? u.getNickname() : "家人";
            String role = u.normalizedRole();
            m.put("nickname", nick);
            m.put("role", role);
            m.put("roleLabel", u.isChef() ? "做餐方" : "点单方");
            m.put("self", Objects.equals(u.getId(), chefUserId));

            FamilyMemberLocation loc = byUser.get(u.getId());
            if (loc == null || loc.getLat() == null || loc.getLng() == null) {
                m.put("hasLocation", false);
                m.put("statusText", "暂无位置");
                m.put("lat", null);
                m.put("lng", null);
                m.put("latText", null);
                m.put("lngText", null);
                m.put("placeName", null);
                m.put("street", null);
                m.put("address", null);
                m.put("addressText", null);
                m.put("mapUrl", null);
                m.put("accuracy", null);
                m.put("accuracyText", null);
                m.put("updatedAt", null);
                m.put("updatedAtText", null);
                m.put("agoText", null);
                m.put("lastSeenText", "暂无上报");
                m.put("distanceMeters", null);
                m.put("distanceText", null);
            } else {
                ensureAddress(loc);
                m.put("hasLocation", true);
                m.put("statusText", null);
                m.put("lat", loc.getLat());
                m.put("lng", loc.getLng());
                m.put("latText", formatCoord(loc.getLat()));
                m.put("lngText", formatCoord(loc.getLng()));
                m.put("placeName", loc.getPlaceName());
                m.put("street", loc.getStreet());
                m.put("address", loc.getAddress());
                m.put("addressText", buildAddressText(loc));
                m.put("mapUrl", buildAmapWebUrl(loc.getLat(), loc.getLng(), buildAddressText(loc)));
                m.put("accuracy", loc.getAccuracy());
                m.put("accuracyText", formatAccuracy(loc.getAccuracy()));
                m.put("updatedAt", loc.getUpdatedAt());
                m.put("updatedAtText", formatAbsolute(loc.getUpdatedAt()));
                String ago = formatAgo(loc.getUpdatedAt(), now);
                m.put("agoText", ago);
                m.put("lastSeenText", ago != null ? ("上次上报 · " + ago) : "已有位置");
                // 用库中快照昵称/角色（若有）覆盖展示
                if (loc.getNickname() != null && !loc.getNickname().isBlank()) {
                    m.put("nickname", loc.getNickname());
                }
                if (loc.getRole() != null && !loc.getRole().isBlank()) {
                    String r = loc.getRole().trim().toUpperCase();
                    m.put("role", r);
                    m.put("roleLabel", "CHEF".equals(r) || "ADMIN".equals(r) ? "做餐方" : "点单方");
                }
                if (oLat != null && oLng != null) {
                    double meters = haversineMeters(oLat, oLng, loc.getLat(), loc.getLng());
                    m.put("distanceMeters", Math.round(meters * 10.0) / 10.0);
                    m.put("distanceText", formatDistance(meters));
                } else {
                    m.put("distanceMeters", null);
                    m.put("distanceText", "无法计算距离（缺少参考点）");
                }
            }
            out.add(m);
        }
        return out;
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    public static String formatDistance(double meters) {
        if (meters < 0) return "—";
        if (meters < 1000) {
            return Math.round(meters) + " 米";
        }
        double km = meters / 1000.0;
        if (km < 10) {
            return String.format(Locale.CHINA, "%.1f 公里", km);
        }
        return Math.round(km) + " 公里";
    }

    public static String formatAgo(LocalDateTime updatedAt, LocalDateTime now) {
        if (updatedAt == null) return null;
        Duration d = Duration.between(updatedAt, now);
        long secs = Math.max(0, d.getSeconds());
        if (secs < 60) return "刚刚";
        long mins = secs / 60;
        if (mins < 60) return mins + " 分钟前";
        long hours = mins / 60;
        if (hours < 24) return hours + " 小时前";
        long days = hours / 24;
        if (days < 30) return days + " 天前";
        return updatedAt.toLocalDate().toString();
    }

    /** 经纬度展示：保留约 6 位小数（~1m 级） */

    public static String buildAddressText(FamilyMemberLocation loc) {
        if (loc == null) return null;
        if (StringUtils.hasText(loc.getPlaceName()) && StringUtils.hasText(loc.getStreet())) {
            return loc.getPlaceName() + " · " + loc.getStreet();
        }
        if (StringUtils.hasText(loc.getAddress())) return loc.getAddress();
        if (StringUtils.hasText(loc.getPlaceName())) return loc.getPlaceName();
        if (StringUtils.hasText(loc.getStreet())) return loc.getStreet();
        return null;
    }

    /** 高德网页 marker（H5 / 桌面回退）；App 内由前端优先调起 URI Scheme */
    public static String buildAmapWebUrl(Double lat, Double lng, String name) {
        if (lat == null || lng == null) return null;
        String n = StringUtils.hasText(name) ? name : "家人位置";
        try {
            String enc = java.net.URLEncoder.encode(n, java.nio.charset.StandardCharsets.UTF_8);
            return "https://uri.amap.com/marker?position=" + lng + "," + lat + "&name=" + enc;
        } catch (Exception e) {
            return "https://uri.amap.com/marker?position=" + lng + "," + lat;
        }
    }
    public static String formatCoord(Double v) {
        if (v == null || Double.isNaN(v)) return null;
        return String.format(Locale.US, "%.6f", v);
    }

    public static String formatAccuracy(Double meters) {
        if (meters == null || Double.isNaN(meters) || meters < 0) return null;
        if (meters < 1000) {
            long m = Math.round(meters);
            if (m <= 50) return m + " 米（较准）";
            if (m <= 100) return m + " 米";
            return m + " 米（较粗）";
        }
        return String.format(Locale.CHINA, "%.1f 公里", meters / 1000.0);
    }

    public static String formatAbsolute(LocalDateTime t) {
        if (t == null) return null;
        return t.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}