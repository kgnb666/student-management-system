package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreVO {

    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String className;
    private Long semesterId;
    private String semesterName;
    private BigDecimal usualScore;
    private BigDecimal examScore;
    private BigDecimal finalScore;
    private Long updateBy;
    private String updaterName;
}
