package com.example.score.vo;

/**
 * 学生学分绩点统计。绩点按学分加权，保留 2 位小数。
 */
public record StudentGpaVO(Double gpa, Double totalCredit, Double earnedCredit, Integer courseCount) {
}
