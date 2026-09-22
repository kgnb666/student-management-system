package com.example.score.service;

import com.example.score.dto.ChangePasswordRequest;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.UserMapper;
import com.example.score.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自助修改密码的校验与副作用测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StudentService studentService;

    @Mock
    private ClassService classService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private UserAuthCache userAuthCache;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("原密码错误时拒绝改密")
    void shouldRejectWrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(user("old-encoded", 0));
        when(passwordEncoder.matches("wrong", "old-encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, request("wrong", "newpass1", "newpass1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密码不正确");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("两次输入的新密码不一致时拒绝改密")
    void shouldRejectMismatchedConfirmPassword() {
        assertThatThrownBy(() -> authService.changePassword(1L, request("old", "newpass1", "newpass2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("两次输入的密码不一致");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("新密码与原密码相同时拒绝改密")
    void shouldRejectSamePassword() {
        when(userMapper.selectById(1L)).thenReturn(user("old-encoded", 0));
        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);
        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L, request("old", "old", "old")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与原密码相同");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("改密成功后清除强制改密标记并让旧 token 失效")
    void shouldUpdatePasswordAndBumpTokenVersion() {
        User user = user("old-encoded", 1);
        user.setTokenVersion(3);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);
        when(passwordEncoder.matches("newpass1", "old-encoded")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded");

        authService.changePassword(1L, request("old", "newpass1", "newpass1"));

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(user.getNeedChangePassword()).isZero();
        assertThat(user.getTokenVersion()).isEqualTo(4);
        verify(userMapper).updateById(user);
    }

    private ChangePasswordRequest request(String oldPassword, String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }

    private User user(String password, int needChangePassword) {
        User user = new User();
        user.setId(1L);
        user.setUsername("2023001");
        user.setPassword(password);
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setNeedChangePassword(needChangePassword);
        return user;
    }
}
