package com.wrt.excel.order.controller;

import com.wrt.excel.order.dto.LoginRequest;
import com.wrt.excel.order.entity.OperationLog;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.RememberMeService;
import com.wrt.excel.order.service.OnlineUserService;
import com.wrt.excel.order.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录 / 登出 / 当前用户（写入登录日志）
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OperationLogService operationLogService;
    private final OnlineUserService onlineUserService;
    private final RememberMeService rememberMeService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req,
                                     HttpSession session,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        String phone = req != null ? req.getPhone() : null;
        String ip = clientIp(request);
        long t0 = System.currentTimeMillis();
        try {
            User user = authService.login(phone, req != null ? req.getPassword() : null, session);
            boolean remember = req == null || req.getRememberMe() == null || Boolean.TRUE.equals(req.getRememberMe());
            authService.applySessionTtl(session, remember);
            rememberMeService.issue(response, user.getId(), remember);
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("data", authService.toPublicProfile(user));

            // 登录成功后立即心跳，便于做餐方看到在线
            try {
                onlineUserService.heartbeat(
                        user.getId(),
                        user.getNickname(),
                        user.normalizedRole(),
                        user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID,
                        session.getId()
                );
            } catch (Exception ignore) {}

            saveLoginLog(user.getId(), user.getNickname(), "登录", "用户登录成功",
                    "/api/auth/login", "POST", phone, 1, null, ip, System.currentTimeMillis() - t0);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            saveLoginLog(null, phone, "登录失败", "登录失败: " + e.getMessage(),
                    "/api/auth/login", "POST", phone, 0, e.getMessage(), ip, System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.error("登录失败", e);
            result.put("code", 500);
            result.put("message", "登录失败，请稍后重试");
            saveLoginLog(null, phone, "登录失败", "登录异常",
                    "/api/auth/login", "POST", phone, 0, e.getMessage(), ip, System.currentTimeMillis() - t0);
        }
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        String ip = clientIp(request);
        Long userId = null;
        String nickname = null;
        try {
            userId = AuthService.sessionUserId(session);
            Object nick = session.getAttribute(AuthService.SESSION_NICKNAME);
            nickname = nick != null ? String.valueOf(nick) : null;
            if (nickname == null && userId != null) {
                User u = authService.currentUser(session);
                if (u != null) nickname = u.getNickname();
            }
        } catch (Exception ignore) {}

        try {
            if (userId != null) {
                onlineUserService.markOffline(userId);
            }
            rememberMeService.clear(response);
            authService.logout(session);
        } catch (Exception ignored) {
            // session 可能已失效
        }

        saveLoginLog(userId, nickname != null ? nickname : "未知", "登出", "用户退出登录",
                "/api/auth/logout", "POST", null, 1, null, ip, 0L);

        result.put("code", 200);
        result.put("message", "已退出登录");
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = authService.currentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "未登录");
            return result;
        }
        // 轻量心跳：打开页面时刷新在线
        try {
            onlineUserService.heartbeat(
                    user.getId(),
                    user.getNickname(),
                    user.normalizedRole(),
                    user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID,
                    session.getId()
            );
        } catch (Exception ignore) {}
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", authService.toPublicProfile(user));
        return result;
    }

    private void saveLoginLog(Long userId, String username, String operationType, String description,
                              String url, String method, String params, int status, String errorMsg,
                              String ip, long costMs) {
        try {
            OperationLog op = new OperationLog();
            op.setUserId(userId);
            op.setUsername(username);
            op.setModule("登录");
            op.setOperationType(operationType);
            op.setDescription(description);
            op.setMethod("AuthController." + ("登出".equals(operationType) ? "logout" : "login"));
            op.setRequestUrl(url);
            op.setRequestMethod(method);
            if (params != null) {
                // 不落密码，只记手机号
                op.setRequestParams("{\"phone\":\"" + params.replace("\"", "") + "\"}");
            }
            op.setIpAddress(ip);
            op.setCostTime(costMs);
            op.setStatus(status);
            op.setErrorMsg(errorMsg);
            op.setOperationTime(LocalDateTime.now());
            op.setCreatedAt(LocalDateTime.now());
            operationLogService.saveLog(op);
        } catch (Exception e) {
            log.warn("写入登录日志失败: {}", e.getMessage());
        }
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
