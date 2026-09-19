package com.example.score.interceptor;

import com.example.score.exception.BusinessException;
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
            String role = claims.get("role", String.class);
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
