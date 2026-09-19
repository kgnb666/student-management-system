package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseStudentVO {

    private Long studentId;
    private String studentNo;
    private String studentName;
    private String className;
    private Long classId;
    private Long scoreId;
    private BigDecimal usualScore;
    private BigDecimal examScore;
    private BigDecimal finalScore;
}
