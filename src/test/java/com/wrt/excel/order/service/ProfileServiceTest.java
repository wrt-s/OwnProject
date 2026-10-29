package com.wrt.excel.order.service;

import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.mapper.UserMapper;
import com.wrt.excel.system.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private FileUploadService fileUploadService;
    @InjectMocks
    private ProfileService profileService;

    @Test
    void hashMatchesExistingLoginMd5() {
        assertEquals("e10adc3949ba59abbe56e057f20f883e", AuthService.hashPassword("123456"));
    }

    @Test
    void nicknameIsTrimmedAndNotRequiredUnique() {
        User user = user("佳宝");
        when(userMapper.selectById(1L)).thenReturn(user);
        User updated = profileService.updateNickname(1L, "  小宝  ");
        assertEquals("小宝", updated.getNickname());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("小宝", captor.getValue().getNickname());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    void blankNicknameRejected() {
        when(userMapper.selectById(1L)).thenReturn(user("佳宝"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateNickname(1L, "   "));
        assertEquals("昵称不能为空", ex.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void wrongCurrentPasswordDoesNotUpdate() {
        User user = user("佳宝");
        user.setPassword(AuthService.hashPassword("123456"));
        when(userMapper.selectById(1L)).thenReturn(user);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword(1L, "000000", "abcdef", "abcdef"));
        assertEquals("当前密码不正确", ex.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void passwordChangeStoresSameMd5AsLogin() {
        User user = user("佳宝");
        user.setPassword(AuthService.hashPassword("123456"));
        when(userMapper.selectById(1L)).thenReturn(user);
        profileService.changePassword(1L, "123456", "love88", "love88");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(AuthService.hashPassword("love88"), captor.getValue().getPassword());
    }

    @Test
    void confirmMismatchRejected() {
        when(userMapper.selectById(1L)).thenReturn(user("佳宝"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword(1L, "123456", "love88", "love99"));
        assertEquals("两次输入的新密码不一致", ex.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void avatarUrlIsPersisted() {
        User user = user("佳宝");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(fileUploadService.uploadImage(any())).thenReturn("/uploads/a.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        User updated = profileService.updateAvatar(1L, file);
        assertEquals("/uploads/a.jpg", updated.getAvatarUrl());
    }

    private static User user(String nickname) {
        User user = new User();
        user.setId(1L);
        user.setNickname(nickname);
        user.setPhone("17700000000");
        user.setRole(User.ROLE_ORDERER);
        return user;
    }
}
