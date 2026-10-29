package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 简易登录（手机号 + MD5 密码），Session 保存当前用户。
 * 业务 API 应优先用 Session 用户，勿盲目信任客户端 userId。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_USER_ID = "LOVE_USER_ID";
    public static final String SESSION_ROLE = "LOVE_ROLE";
    public static final String SESSION_NICKNAME = "LOVE_NICKNAME";
    public static final String SESSION_PHONE = "LOVE_PHONE";
    public static final String SESSION_FAMILY_ID = "LOVE_FAMILY_ID";
    public static final Long DEFAULT_FAMILY_ID = 1L;

    private final UserMapper userMapper;

    public User login(String phone, String password, HttpSession session) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("请输入手机号和密码");
        }
        phone = phone.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (user.getPassword() == null || !user.getPassword().equalsIgnoreCase(md5)) {
            throw new IllegalArgumentException("密码错误");
        }
        user.setLastLoginTime(LocalDateTime.now());
        // 顺便把旧角色规范为 ORDERER / CHEF
        String norm = user.normalizedRole();
        if (!norm.equals(user.getRole())) {
            user.setRole(norm);
        }
        userMapper.updateById(user);

        Long fid = user.getFamilyId() != null ? user.getFamilyId() : DEFAULT_FAMILY_ID;
        if (user.getFamilyId() == null) {
            user.setFamilyId(fid);
            userMapper.updateById(user);
        }
        bindSession(user, session);
        return user;
    }

    /** 登录成功或 remember-me 恢复时写入 Session */
    public void bindSession(User user, HttpSession session) {
        if (user == null || session == null) return;
        String norm = user.normalizedRole();
        Long fid = user.getFamilyId() != null ? user.getFamilyId() : DEFAULT_FAMILY_ID;
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_ROLE, norm);
        session.setAttribute(SESSION_NICKNAME, user.getNickname());
        session.setAttribute(SESSION_PHONE, user.getPhone());
        session.setAttribute(SESSION_FAMILY_ID, fid);
    }

    public User loadUserById(Long id) {
        if (id == null) return null;
        return userMapper.selectById(id);
    }

    public void applySessionTtl(HttpSession session, boolean remember) {
        if (session == null) return;
        session.setMaxInactiveInterval(remember
                ? RememberMeService.REMEMBER_SECONDS
                : RememberMeService.SHORT_SECONDS);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public User currentUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (id == null) return null;
        User user = userMapper.selectById((Long) id);
        if (user == null) return null;
        // 保持 session 角色最新
        session.setAttribute(SESSION_ROLE, user.normalizedRole());
        session.setAttribute(SESSION_NICKNAME, user.getNickname());
        Long fid = user.getFamilyId() != null ? user.getFamilyId() : DEFAULT_FAMILY_ID;
        session.setAttribute(SESSION_FAMILY_ID, fid);
        return user;
    }

    public Map<String, Object> toPublicProfile(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("userId", user.getId());
        m.put("nickname", user.getNickname());
        m.put("phone", user.getPhone());
        m.put("role", user.normalizedRole());
        m.put("familyId", user.getFamilyId() != null ? user.getFamilyId() : DEFAULT_FAMILY_ID);
        m.put("roleLabel", user.isChef() ? "做餐方" : "点单方");
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("themeColor", user.getThemeColor());
        m.put("lastLoginTime", user.getLastLoginTime());
        return m;
    }

    public static Long sessionUserId(HttpSession session) {
        if (session == null) return null;
        Object id = session.getAttribute(SESSION_USER_ID);
        return id == null ? null : (Long) id;
    }

    public static String sessionRole(HttpSession session) {
        if (session == null) return null;
        Object r = session.getAttribute(SESSION_ROLE);
        return r == null ? null : String.valueOf(r);
    }

    public static boolean isChefSession(HttpSession session) {
        String r = sessionRole(session);
        return User.ROLE_CHEF.equals(r) || "ADMIN".equalsIgnoreCase(r);
    }

    /**
     * 必须已登录；返回 Session 中的用户 ID。
     */
    public static Long requireSessionUserId(HttpSession session) {
        Long id = sessionUserId(session);
        if (id == null) {
            throw new IllegalStateException("请先登录");
        }
        return id;
    }

    /**
     * 自助业务（下单、签到、抽奖、收藏、盲盒等）：始终以 Session 用户为准，
     * 忽略客户端传来的 userId（防止越权）。
     */
    public static Long forceSessionUserId(HttpSession session, Long clientUserId) {
        Long sid = requireSessionUserId(session);
        if (clientUserId != null && !Objects.equals(clientUserId, sid)) {
            // ORDERER 与 CHEF 自助路径均覆盖；厨房代操作请用 resolveTargetUserId
        }
        return sid;
    }

    /**
     * 解析目标用户：
     * - ORDERER：强制 Session，忽略不匹配的 clientUserId
     * - CHEF：若传入 clientUserId 则允许代查/代操作（厨房/管理 API），否则用 Session
     */
    public static Long sessionFamilyId(HttpSession session) {
        if (session == null) return null;
        Object fid = session.getAttribute(SESSION_FAMILY_ID);
        return fid == null ? null : (Long) fid;
    }

    /**
     * 当前登录用户的家庭 ID（未设置时回落到默认家庭 1，保证情侣共享）
     */
    public static Long requireFamilyId(HttpSession session) {
        requireSessionUserId(session);
        Long fid = sessionFamilyId(session);
        return fid != null ? fid : DEFAULT_FAMILY_ID;
    }

    /**
     * 校验资源属于当前家庭（同家庭成员可共同编辑）
     */
    public static void assertSameFamily(Long resourceFamilyId, HttpSession session) {
        Long fid = requireFamilyId(session);
        Long rf = resourceFamilyId != null ? resourceFamilyId : DEFAULT_FAMILY_ID;
        if (!Objects.equals(rf, fid)) {
            throw new IllegalStateException("无权操作此家庭数据");
        }
    }


    /** 同家庭点单方（爱心币归属方）；无则返回 null */
    public User findFamilyOrderer(Long familyId) {
        Long fid = familyId != null ? familyId : DEFAULT_FAMILY_ID;
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, fid));
        if (list == null) return null;
        for (User u : list) {
            if (u != null && u.isOrderer()) return u;
        }
        return null;
    }

    public static Long resolveTargetUserId(HttpSession session, Long clientUserId) {
        Long sid = requireSessionUserId(session);
        if (clientUserId == null || Objects.equals(clientUserId, sid)) {
            return sid;
        }
        if (isChefSession(session)) {
            return clientUserId;
        }
        // 点单方传入他人 id：忽略，始终用 Session
        return sid;
    }

    /**
     * 爱心币读写：点单方强制 Session；做餐方只能操作同一家庭的成员。
     */
    public Long resolveCoinTarget(HttpSession session, Long clientUserId) {
        Long sid = requireSessionUserId(session);
        if (clientUserId == null || Objects.equals(clientUserId, sid)) {
            return sid;
        }
        if (!isChefSession(session)) {
            return sid;
        }
        User target = userMapper.selectById(clientUserId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        assertSameFamily(target.getFamilyId(), session);
        return target.getId();
    }
}
