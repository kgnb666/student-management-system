package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreChangeLogVO {

    private Long id;
    private Long scoreId;
    private Long courseId;
    private String courseName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String operatorName;
    private String operatorRole;
    private String action;
    private BigDecimal beforeUsualScore;
    private BigDecimal beforeExamScore;
    private BigDecimal beforeFinalScore;
    private BigDecimal afterUsualScore;
    private BigDecimal afterExamScore;
    private BigDecimal afterFinalScore;
    private String remark;
    private LocalDateTime createTime;
}
