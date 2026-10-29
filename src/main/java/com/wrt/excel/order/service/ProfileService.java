package com.wrt.excel.order.service;

import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.UserMapper;
import com.wrt.excel.system.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 登录用户自助修改昵称、头像、密码。
 * 登录账号是手机号；昵称只是展示名，不要求唯一。
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    public static final int NICKNAME_MAX = 20;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 32;

    private final UserMapper userMapper;
    private final FileUploadService fileUploadService;

    public User updateNickname(Long userId, String raw) {
        User user = requireUser(userId);
        String nickname = normalizeNickname(raw);
        User patch = new User();
        patch.setId(user.getId());
        patch.setNickname(nickname);
        userMapper.updateById(patch);
        user.setNickname(nickname);
        return reload(user);
    }

    public User updateAvatar(Long userId, MultipartFile file) {
        User user = requireUser(userId);
        String stored = fileUploadService.uploadImage(file);
        if (!StringUtils.hasText(stored)) {
            throw new IllegalArgumentException("头像上传失败");
        }
        if (stored.length() > 1000) {
            throw new IllegalArgumentException("头像地址过长，请换一张图片");
        }
        User patch = new User();
        patch.setId(user.getId());
        patch.setAvatarUrl(stored);
        userMapper.updateById(patch);
        user.setAvatarUrl(stored);
        return reload(user);
    }

    /**
     * 校验当前密码后写入与登录相同的 MD5。不销毁 Session。
     */
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = requireUser(userId);
        if (!StringUtils.hasText(currentPassword)) {
            throw new IllegalArgumentException("请输入当前密码");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("请输入新密码");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }
        int len = newPassword.codePointCount(0, newPassword.length());
        if (len < PASSWORD_MIN || len > PASSWORD_MAX) {
            throw new IllegalArgumentException("新密码长度需为 6～32 位");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        String currentHash = AuthService.hashPassword(currentPassword);
        if (user.getPassword() == null || !user.getPassword().equalsIgnoreCase(currentHash)) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        User patch = new User();
        patch.setId(user.getId());
        patch.setPassword(AuthService.hashPassword(newPassword));
        userMapper.updateById(patch);
    }

    static String normalizeNickname(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("请输入昵称");
        }
        String nickname = raw.trim();
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (nickname.codePoints().anyMatch(cp -> Character.isISOControl(cp))) {
            throw new IllegalArgumentException("昵称不能包含控制字符");
        }
        int len = nickname.codePointCount(0, nickname.length());
        if (len > NICKNAME_MAX) {
            throw new IllegalArgumentException("昵称最多 " + NICKNAME_MAX + " 个字");
        }
        return nickname;
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new IllegalStateException("请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("请先登录");
        }
        return user;
    }

    private User reload(User fallback) {
        User fresh = userMapper.selectById(fallback.getId());
        return fresh != null ? fresh : fallback;
    }
}
