package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 计算绩点所需的最小字段：课程学分与总评成绩。
 */
@Data
public class StudentCourseScoreVO {

    private BigDecimal credit;
    private BigDecimal finalScore;
}
