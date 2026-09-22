package com.example.score.vo;

/**
 * 管理员首页概览：基础数据规模 + 当前学期成绩统计。
 */
public record DashboardVO(Integer studentCount,
                          Integer teacherCount,
                          Integer classCount,
                          Integer courseCount,
                          Integer pendingStudentCount,
                          Long currentSemesterId,
                          String currentSemesterName,
                          ScoreStatisticsVO scoreStatistics) {
}
