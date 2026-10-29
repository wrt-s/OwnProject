package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wrt.excel.order.entity.AppDevice;
import com.wrt.excel.order.entity.AppNotification;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.AppDeviceMapper;
import com.wrt.excel.order.mapper.AppNotificationMapper;
import com.wrt.excel.order.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * App / H5 站内通知：在微信模板同一事件点写入通知记录。
 * 不依赖 FCM；客户端轮询后可在前台弹系统通知。
 */
@Slf4j
@Service
public class AppNotifyService {

    /** Feature flag: phone system / in-app poll notifications OFF. WeChat templates stay in OrderService. */
    public static final boolean APP_SYSTEM_NOTIFY_ENABLED = false;

    public static final String TYPE_NEW_ORDER = "NEW_ORDER";
    public static final String TYPE_STATUS = "STATUS";
    public static final String TYPE_CANCEL = "CANCEL";

    @Autowired
    private AppNotificationMapper notificationMapper;

    @Autowired
    private AppDeviceMapper deviceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WechatMpService wechatMpService;

    public AppDevice registerDevice(Long userId, String deviceId, String platform, String appVersion) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId required");
        }
        String did = deviceId.trim();
        if (did.length() > 64) {
            did = did.substring(0, 64);
        }
        String plat = (platform == null || platform.isBlank()) ? "android" : platform.trim().toLowerCase();
        if (plat.length() > 32) {
            plat = plat.substring(0, 32);
        }
        String ver = appVersion == null ? null : appVersion.trim();
        if (ver != null && ver.length() > 32) {
            ver = ver.substring(0, 32);
        }
        LocalDateTime now = LocalDateTime.now();
        AppDevice existing = deviceMapper.selectOne(new LambdaQueryWrapper<AppDevice>()
                .eq(AppDevice::getUserId, userId)
                .eq(AppDevice::getDeviceId, did)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setPlatform(plat);
            existing.setAppVersion(ver);
            existing.setLastSeenAt(now);
            existing.setUpdatedAt(now);
            deviceMapper.updateById(existing);
            return existing;
        }
        AppDevice d = new AppDevice();
        d.setUserId(userId);
        d.setDeviceId(did);
        d.setPlatform(plat);
        d.setAppVersion(ver);
        d.setLastSeenAt(now);
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        deviceMapper.insert(d);
        return d;
    }

    public void touchDevice(Long userId, String deviceId) {
        if (userId == null || deviceId == null || deviceId.isBlank()) return;
        try {
            LocalDateTime now = LocalDateTime.now();
            deviceMapper.update(null, new LambdaUpdateWrapper<AppDevice>()
                    .eq(AppDevice::getUserId, userId)
                    .eq(AppDevice::getDeviceId, deviceId.trim())
                    .set(AppDevice::getLastSeenAt, now)
                    .set(AppDevice::getUpdatedAt, now));
        } catch (Exception e) {
            log.debug("touchDevice ignored: {}", e.getMessage());
        }
    }

    /** 新订单 → 同家庭做餐方 */
    public void notifyNewOrder(Long ordererUserId, String orderNo, String dishSummary, String priceText, String remark) {
        if (!APP_SYSTEM_NOTIFY_ENABLED) { return; }
        try {
            String title = "新订单提醒";
            String body = safe(dishSummary, "有新订单")
                    + (priceText == null || priceText.isBlank() ? "" : (" · " + priceText));
            if (remark != null && !remark.isBlank()) {
                body = body + "（" + trim(remark, 40) + "）";
            }
            body = trim(body, 200);
            List<Long> chefs = findChefUserIds(ordererUserId);
            for (Long uid : chefs) {
                insertOne(uid, title, body, TYPE_NEW_ORDER, orderNo);
            }
        } catch (Exception e) {
            log.error("App notify new-order failed (ignored): {}", e.getMessage());
        }
    }

    /** 状态变更 → 点单方（订单归属用户） */
    public void notifyStatusChange(Long ordererUserId, String orderNo, String statusCode, String dishSummary, String remark) {
        if (!APP_SYSTEM_NOTIFY_ENABLED) { return; }
        try {
            if (ordererUserId == null) return;
            String statusText = wechatMpService.statusText(statusCode);
            String title;
            if ("cooking".equalsIgnoreCase(statusCode) || "preparing".equalsIgnoreCase(statusCode)) {
                title = "接单制作中";
            } else if ("ready".equalsIgnoreCase(statusCode) || "delivered".equalsIgnoreCase(statusCode)) {
                title = "做好了，可以取餐";
            } else if ("completed".equalsIgnoreCase(statusCode)) {
                title = "订单已完成";
            } else {
                title = "订单状态更新";
            }
            String body = statusText + " · " + safe(dishSummary, "-");
            if (remark != null && !remark.isBlank()) {
                body = body + " · " + trim(remark, 40);
            }
            if (orderNo != null) {
                body = body + "（" + orderNo + "）";
            }
            insertOne(ordererUserId, title, trim(body, 200), TYPE_STATUS, orderNo);
        } catch (Exception e) {
            log.error("App notify status failed (ignored): {}", e.getMessage());
        }
    }

    /** 取消 → 点单方 + 做餐方（对齐微信） */
    public void notifyCancel(Long ordererUserId, String orderNo, String dishSummary) {
        if (!APP_SYSTEM_NOTIFY_ENABLED) { return; }
        try {
            String title = "订单已取消";
            String body = trim(safe(dishSummary, "-") + (orderNo == null ? "" : ("（" + orderNo + "）")), 200);
            Set<Long> targets = new LinkedHashSet<>();
            if (ordererUserId != null) {
                targets.add(ordererUserId);
            }
            targets.addAll(findChefUserIds(ordererUserId));
            for (Long uid : targets) {
                insertOne(uid, title, body, TYPE_CANCEL, orderNo);
            }
        } catch (Exception e) {
            log.error("App notify cancel failed (ignored): {}", e.getMessage());
        }
    }

    public long unreadCount(Long userId) {
        if (userId == null) return 0L;
        Long c = notificationMapper.selectCount(new LambdaQueryWrapper<AppNotification>()
                .eq(AppNotification::getUserId, userId)
                .eq(AppNotification::getReadFlag, 0));
        return c == null ? 0L : c;
    }

    public List<AppNotification> listRecent(Long userId, int limit) {
        int lim = Math.max(1, Math.min(limit, 50));
        return notificationMapper.selectList(new LambdaQueryWrapper<AppNotification>()
                .eq(AppNotification::getUserId, userId)
                .orderByDesc(AppNotification::getId)
                .last("LIMIT " + lim));
    }

    /** 拉取 id > afterId 的新通知（升序，便于客户端顺序弹出） */
    public List<AppNotification> pollAfter(Long userId, Long afterId, int limit) {
        int lim = Math.max(1, Math.min(limit, 30));
        long after = afterId == null ? 0L : afterId;
        return notificationMapper.selectList(new LambdaQueryWrapper<AppNotification>()
                .eq(AppNotification::getUserId, userId)
                .gt(AppNotification::getId, after)
                .orderByAsc(AppNotification::getId)
                .last("LIMIT " + lim));
    }

    public int markRead(Long userId, List<Long> ids) {
        if (userId == null || ids == null || ids.isEmpty()) return 0;
        return notificationMapper.update(null, new LambdaUpdateWrapper<AppNotification>()
                .eq(AppNotification::getUserId, userId)
                .in(AppNotification::getId, ids)
                .eq(AppNotification::getReadFlag, 0)
                .set(AppNotification::getReadFlag, 1));
    }

    public int markAllRead(Long userId) {
        if (userId == null) return 0;
        return notificationMapper.update(null, new LambdaUpdateWrapper<AppNotification>()
                .eq(AppNotification::getUserId, userId)
                .eq(AppNotification::getReadFlag, 0)
                .set(AppNotification::getReadFlag, 1));
    }

    private void insertOne(Long userId, String title, String body, String type, String orderNo) {
        if (userId == null) return;
        AppNotification n = new AppNotification();
        n.setUserId(userId);
        n.setTitle(trim(safe(title, "Love 通知"), 120));
        n.setBody(trim(body, 500));
        n.setType(type);
        n.setOrderNo(orderNo);
        n.setReadFlag(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
        log.info("App notification saved id={} userId={} type={} orderNo={}",
                n.getId(), userId, type, orderNo);
    }

    private List<Long> findChefUserIds(Long ordererUserId) {
        List<Long> out = new ArrayList<>();
        Long familyId = AuthService.DEFAULT_FAMILY_ID;
        if (ordererUserId != null) {
            User u = userMapper.selectById(ordererUserId);
            if (u != null && u.getFamilyId() != null) {
                familyId = u.getFamilyId();
            }
        }
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, familyId));
        if (list == null) return out;
        for (User u : list) {
            if (u != null && u.isChef() && u.getId() != null) {
                out.add(u.getId());
            }
        }
        // fallback: any CHEF/ADMIN if family empty
        if (out.isEmpty()) {
            List<User> all = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .in(User::getRole, "CHEF", "ADMIN"));
            if (all != null) {
                for (User u : all) {
                    if (u != null && u.getId() != null) out.add(u.getId());
                }
            }
        }
        return out;
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s.trim();
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }
}
