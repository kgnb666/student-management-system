package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩统计的 SQL 聚合结果，避免把全量成绩取回内存后再计算。
 */
@Data
public class ScoreAggregateVO {

    private BigDecimal average;
    private BigDecimal max;
    private BigDecimal min;
    private Long total;
    private Long passCount;
    private Long excellentCount;
    private Long bucket0;
    private Long bucket1;
    private Long bucket2;
    private Long bucket3;
    private Long bucket4;
}
