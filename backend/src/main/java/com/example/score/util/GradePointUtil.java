package com.example.score.util;

import java.math.BigDecimal;

/**
 * 4.0 制绩点换算。
 *
 * <p>分数区间为左闭右闭：90 及以上 4.0，85-89 为 3.7，依次递减，60 以下记 0。</p>
 */
public final class GradePointUtil {

    private GradePointUtil() {
    }

    public static BigDecimal gradePoint(BigDecimal finalScore) {
        if (finalScore == null) {
            return BigDecimal.ZERO;
        }
        double value = finalScore.doubleValue();
        if (value >= 90) {
            return new BigDecimal("4.0");
        }
        if (value >= 85) {
            return new BigDecimal("3.7");
        }
        if (value >= 82) {
            return new BigDecimal("3.3");
        }
        if (value >= 78) {
            return new BigDecimal("3.0");
        }
        if (value >= 75) {
            return new BigDecimal("2.7");
        }
        if (value >= 72) {
            return new BigDecimal("2.3");
        }
        if (value >= 68) {
            return new BigDecimal("2.0");
        }
        if (value >= 64) {
            return new BigDecimal("1.5");
        }
        if (value >= 60) {
            return new BigDecimal("1.0");
        }
        return BigDecimal.ZERO;
    }

    /**
     * 是否及格（总评不低于 60）。
     */
    public static boolean isPassed(BigDecimal finalScore) {
        return finalScore != null && finalScore.doubleValue() >= 60;
    }
}
