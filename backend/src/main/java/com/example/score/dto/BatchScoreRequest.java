package com.example.score.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchScoreRequest {

    private Long courseId;
    private List<ScoreItem> scores;
}
