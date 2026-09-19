package com.example.score.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreStatisticsVO {

    private Double averageScore;
    private Double maxScore;
    private Double minScore;
    private Double passRate;
    private Double excellentRate;
    private List<DistributionItem> distribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionItem {
        private String name;
        private Integer value;
    }
}
