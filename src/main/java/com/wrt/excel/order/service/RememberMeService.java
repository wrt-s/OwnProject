package com.wrt.excel.order.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 长效「记住登录」：HMAC 签名 cookie，关闭浏览器 / 容器重启后仍可恢复 Session。
 * 不落明文密码；仅存 userId + 过期时间 + 签名。
 */
@Service
public class RememberMeService {

    public static final String COOKIE_NAME = "LOVE_REMEMBER";
    /** 30 天 */
    public static final int REMEMBER_SECONDS = 30 * 24 * 60 * 60;
    /** 不记住时：2 小时 */
    public static final int SHORT_SECONDS = 2 * 60 * 60;

    @Value("${love.remember.secret:LoveRememberMeKey2024Private}")
    private String secret;

    public void issue(HttpServletResponse response, Long userId, boolean remember) {
        if (userId == null || response == null) return;
        // rememberMe:false must not set LOVE_REMEMBER (session-only login)
        if (!remember) {
            clear(response);
            return;
        }
        int maxAge = REMEMBER_SECONDS;
        long exp = System.currentTimeMillis() / 1000L + maxAge;
        String token = buildToken(userId, exp);
        response.addHeader("Set-Cookie",
                COOKIE_NAME + "=" + token
                        + "; Path=/; Max-Age=" + maxAge
                        + "; HttpOnly; SameSite=Lax");
    }

    public void clear(HttpServletResponse response) {
        if (response == null) return;
        response.addHeader("Set-Cookie",
                COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }

    public Long resolveUserId(HttpServletRequest request) {
        String raw = readCookie(request, COOKIE_NAME);
        if (!StringUtils.hasText(raw)) return null;
        try {
            String[] parts = raw.split("\\.");
            if (parts.length != 3) return null;
            String uidStr = new String(b64d(parts[0]), StandardCharsets.UTF_8);
            String expStr = new String(b64d(parts[1]), StandardCharsets.UTF_8);
            String sig = parts[2];
            long exp = Long.parseLong(expStr);
            if (exp < System.currentTimeMillis() / 1000L) return null;
            String expect = sign(uidStr + ":" + expStr);
            if (!MessageDigest.isEqual(
                    expect.getBytes(StandardCharsets.UTF_8),
                    sig.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return Long.parseLong(uidStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildToken(Long userId, long exp) {
        String uid = String.valueOf(userId);
        String expStr = String.valueOf(exp);
        String sig = sign(uid + ":" + expStr);
        return b64e(uid.getBytes(StandardCharsets.UTF_8))
                + "." + b64e(expStr.getBytes(StandardCharsets.UTF_8))
                + "." + sig;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] dig = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return b64e(dig);
        } catch (Exception e) {
            throw new IllegalStateException("remember-me sign failed", e);
        }
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static String b64e(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static byte[] b64d(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
