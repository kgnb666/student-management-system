package com.example.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "学号不能为空")
    @Size(max = 30, message = "学号长度不能超过 30 个字符")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String name;

    private String gender;

    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String phone;

    @NotNull(message = "请选择班级")
    private Long classId;

    @NotBlank(message = "密码不能少于 6 位")
    @Size(min = 6, message = "密码不能少于 6 位")
    private String password;

    private String confirmPassword;
}
