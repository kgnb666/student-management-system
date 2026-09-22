package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.LoginRequest;
import com.example.score.dto.LoginResponse;
import com.example.score.dto.RegisterRequest;
import com.example.score.dto.ChangePasswordRequest;
import com.example.score.service.AuthService;
import com.example.score.service.RegisterRateLimiter;
import com.example.score.util.ClientIpUtil;
import com.example.score.vo.ClassOptionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegisterRateLimiter registerRateLimiter;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, ClientIpUtil.resolve(httpRequest)));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        registerRateLimiter.checkAllowed(ClientIpUtil.resolve(httpRequest));
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return Result.success();
    }

    @GetMapping("/classes")
    public Result<List<ClassOptionVO>> classes() {
        return Result.success(authService.listClasses());
    }
}
