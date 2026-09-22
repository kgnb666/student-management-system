package com.example.score.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchScoreRequest {

    @NotNull(message = "请选择课程")
    private Long courseId;

    @Valid
    private List<ScoreItem> scores;
}
