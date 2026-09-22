package com.example.score.interceptor;

import com.example.score.exception.BusinessException;
import com.example.score.entity.User;
import com.example.score.service.UserAuthCache;
import com.example.score.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserAuthCache userAuthCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        try {
            Claims claims = jwtUtil.parseToken(authorization.substring(7));
            Long userId = ((Number) claims.get("userId")).longValue();
            int tokenVersion = claims.get("tokenVersion") == null
                    ? 0
                    : ((Number) claims.get("tokenVersion")).intValue();

            // 每次请求回查用户状态与 token 版本：账号停用、待审核或改过密码后，旧 token 立即失效。
            User user = userAuthCache.get(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用，请重新登录");
            }
            int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            if (currentVersion != tokenVersion) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
            }

            // 管理员新建账号或重置密码后，必须先改密才能使用其他功能
            if (user.getNeedChangePassword() != null && user.getNeedChangePassword() == 1
                    && !request.getRequestURI().startsWith("/api/auth/")) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "请先修改初始密码");
            }

            // 角色以数据库为准，避免 token 中的角色与实际权限不一致。
            String role = user.getRole();
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
            checkRole(request.getRequestURI(), role);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
    }

    private void checkRole(String uri, String role) {
        if (uri.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "没有权限访问该功能");
        }
        if (uri.startsWith("/api/teacher/") && !"TEACHER".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "没有权限访问该功能");
        }
        if (uri.startsWith("/api/student/") && !"STUDENT".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "没有权限访问该功能");
        }
    }
}
