package com.example.score.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentVO {

    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String phone;
    private Long classId;
    private String className;
    private Integer status;
}
