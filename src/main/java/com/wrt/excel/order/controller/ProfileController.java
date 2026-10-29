package com.wrt.excel.order.controller;

import com.wrt.excel.order.dto.NicknameUpdateRequest;
import com.wrt.excel.order.dto.PasswordChangeRequest;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.OnlineUserService;
import com.wrt.excel.order.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已登录用户自助修改资料。点单方与做餐方共用。
 * 密码不会写入日志；失败由全局异常处理返回 4xx。
 */
@RestController
@RequestMapping("/api/auth/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;
    private final OnlineUserService onlineUserService;

    @PostMapping("/nickname")
    public Map<String, Object> updateNickname(@RequestBody(required = false) NicknameUpdateRequest req,
                                              HttpSession session) {
        Long userId = AuthService.requireSessionUserId(session);
        User user = profileService.updateNickname(userId, req == null ? null : req.getNickname());
        authService.bindSession(user, session);
        touchOnline(user, session);
        return ok("昵称已更新", authService.toPublicProfile(user));
    }

    @PostMapping("/avatar")
    public Map<String, Object> updateAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        Long userId = AuthService.requireSessionUserId(session);
        User user = profileService.updateAvatar(userId, file);
        authService.bindSession(user, session);
        return ok("头像已更新", authService.toPublicProfile(user));
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestBody(required = false) PasswordChangeRequest req,
                                              HttpSession session) {
        Long userId = AuthService.requireSessionUserId(session);
        String current = req == null ? null : req.getCurrentPassword();
        String next = req == null ? null : req.getNewPassword();
        String confirm = req == null ? null : req.getConfirmPassword();
        profileService.changePassword(userId, current, next, confirm);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "密码已修改，当前登录仍然有效");
        return result;
    }

    private void touchOnline(User user, HttpSession session) {
        try {
            onlineUserService.heartbeat(
                    user.getId(),
                    user.getNickname(),
                    user.normalizedRole(),
                    user.getFamilyId() != null ? user.getFamilyId() : AuthService.DEFAULT_FAMILY_ID,
                    session.getId()
            );
        } catch (Exception ignore) {
            // 在线昵称下次心跳会对齐
        }
    }

    private static Map<String, Object> ok(String message, Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
