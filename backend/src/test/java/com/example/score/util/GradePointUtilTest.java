package com.example.score.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 4.0 制绩点换算的边界测试。
 */
class GradePointUtilTest {

    @ParameterizedTest(name = "总评 {0} -> 绩点 {1}")
    @CsvSource({
            "100,4.0", "90,4.0", "89.9,3.7", "85,3.7",
            "84.9,3.3", "82,3.3", "81.9,3.0", "78,3.0",
            "77.9,2.7", "75,2.7", "74.9,2.3", "72,2.3",
            "71.9,2.0", "68,2.0", "67.9,1.5", "64,1.5",
            "63.9,1.0", "60,1.0", "59.9,0.0", "0,0.0"
    })
    void shouldMapScoreToGradePoint(String score, String expected) {
        assertThat(GradePointUtil.gradePoint(new BigDecimal(score)))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("没有成绩时绩点为 0")
    void shouldReturnZeroWhenScoreIsNull() {
        assertThat(GradePointUtil.gradePoint(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("及格判定以总评 60 为界")
    void shouldJudgePassedByScore() {
        assertThat(GradePointUtil.isPassed(new BigDecimal("60"))).isTrue();
        assertThat(GradePointUtil.isPassed(new BigDecimal("59.9"))).isFalse();
        assertThat(GradePointUtil.isPassed(null)).isFalse();
    }
}
