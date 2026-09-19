package com.example.score.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String studentNo;
    private String name;
    private String gender;
    private String phone;
    private Long classId;
    private String password;
    private String confirmPassword;
}
