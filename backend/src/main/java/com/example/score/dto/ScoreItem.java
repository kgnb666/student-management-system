package com.example.score.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreItem {

    private Long studentId;
    private BigDecimal usualScore;
    private BigDecimal examScore;
}
