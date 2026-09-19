package com.example.score.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreRequest {

    private Long courseId;
    private Long studentId;
    private BigDecimal usualScore;
    private BigDecimal examScore;
}
