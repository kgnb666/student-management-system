package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.entity.ClassInfo;
import com.example.score.dto.LoginRequest;
import com.example.score.dto.LoginResponse;
import com.example.score.dto.RegisterRequest;
import com.example.score.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @GetMapping("/classes")
    public Result<List<ClassInfo>> classes() {
        return Result.success(authService.listClasses());
    }
}
