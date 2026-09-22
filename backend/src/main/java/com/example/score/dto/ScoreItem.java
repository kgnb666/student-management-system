package com.example.score.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreItem {

    @NotNull(message = "请选择课程和学生")
    private Long studentId;

    @DecimalMin(value = "0", message = "成绩必须在 0 到 100 之间")
    @DecimalMax(value = "100", message = "成绩必须在 0 到 100 之间")
    private BigDecimal usualScore;

    @DecimalMin(value = "0", message = "成绩必须在 0 到 100 之间")
    @DecimalMax(value = "100", message = "成绩必须在 0 到 100 之间")
    private BigDecimal examScore;
}
