package com.example.score.interceptor;

import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.service.UserAuthCache;
import com.example.score.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 鉴权拦截器测试：未登录、账号不可用、token 失效与角色越权。
 */
@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserAuthCache userAuthCache;

    private JwtInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new JwtInterceptor(jwtUtil, userAuthCache);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("未携带 token 返回 401")
    void shouldRejectMissingToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/student/profile");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("OPTIONS 预检请求直接放行")
    void shouldPassOptionsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/student/profile");

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("账号被停用后旧 token 返回 401")
    void shouldRejectDisabledUser() {
        MockHttpServletRequest request = requestWithToken("/api/student/profile");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        when(userAuthCache.get(1L)).thenReturn(user("STUDENT", 0, 0));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("token 版本不一致（改过密码）返回 401")
    void shouldRejectStaleTokenVersion() {
        MockHttpServletRequest request = requestWithToken("/api/student/profile");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        when(userAuthCache.get(1L)).thenReturn(user("STUDENT", 1, 1));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("学生访问管理员接口返回 403")
    void shouldRejectStudentAccessingAdminApi() {
        MockHttpServletRequest request = requestWithToken("/api/admin/students");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        when(userAuthCache.get(1L)).thenReturn(user("STUDENT", 1, 0));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("角色以数据库为准：token 内角色过期不影响鉴权结果")
    void shouldUseRoleFromDatabase() throws Exception {
        MockHttpServletRequest request = requestWithToken("/api/student/profile");
        // token 中仍是 STUDENT，但数据库里已经是 TEACHER，访问学生接口应被拒绝
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        when(userAuthCache.get(1L)).thenReturn(user("TEACHER", 1, 0));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("合法学生请求放行并写入用户属性")
    void shouldPassValidStudentRequest() throws Exception {
        MockHttpServletRequest request = requestWithToken("/api/student/profile");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        when(userAuthCache.get(1L)).thenReturn(user("STUDENT", 1, 0));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(request.getAttribute("userId")).isEqualTo(1L);
        assertThat(request.getAttribute("role")).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("需要强制改密的账号访问业务接口返回 403")
    void shouldRejectBusinessApiWhenPasswordChangeRequired() {
        MockHttpServletRequest request = requestWithToken("/api/student/profile");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        User user = user("STUDENT", 1, 0);
        user.setNeedChangePassword(1);
        when(userAuthCache.get(1L)).thenReturn(user);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先修改初始密码")
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("需要强制改密的账号仍可访问改密接口")
    void shouldAllowChangePasswordApiWhenPasswordChangeRequired() throws Exception {
        MockHttpServletRequest request = requestWithToken("/api/auth/change-password");
        when(jwtUtil.parseToken(anyString())).thenReturn(claims("STUDENT", 0));
        User user = user("STUDENT", 1, 0);
        user.setNeedChangePassword(1);
        when(userAuthCache.get(1L)).thenReturn(user);

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    private MockHttpServletRequest requestWithToken(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.addHeader("Authorization", "Bearer test-token");
        return request;
    }

    private Claims claims(String role, int tokenVersion) {
        return Jwts.claims()
                .add("userId", 1L)
                .add("role", role)
                .add("tokenVersion", tokenVersion)
                .build();
    }

    private User user(String role, int status, int tokenVersion) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        user.setStatus(status);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
