package com.example.score.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "请输入用户名和密码")
    private String username;

    @NotBlank(message = "请输入用户名和密码")
    private String password;
}
