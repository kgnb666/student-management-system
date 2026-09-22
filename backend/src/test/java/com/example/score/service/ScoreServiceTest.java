package com.example.score.service;

import com.example.score.dto.ScoreRequest;
import com.example.score.exception.BusinessException;
import com.example.score.entity.Score;
import com.example.score.entity.Course;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.vo.ScoreAggregateVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.StudentCourseScoreVO;
import com.example.score.vo.StudentGpaVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩计算与统计口径的单元测试。数据库访问全部使用 Mock，不依赖 MySQL。
 */
@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private CourseStudentMapper courseStudentMapper;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private ScoreChangeLogService scoreChangeLogService;

    @InjectMocks
    private ScoreService scoreService;

    @Test
    @DisplayName("总评成绩按平时 30% + 期末 70% 计算并保留 1 位小数")
    void shouldCalculateFinalScore() {
        assertThat(saveAndCaptureFinalScore(new BigDecimal("80"), new BigDecimal("90")))
                .isEqualByComparingTo(new BigDecimal("87.0"));
        assertThat(saveAndCaptureFinalScore(new BigDecimal("60"), new BigDecimal("70")))
                .isEqualByComparingTo(new BigDecimal("67.0"));
        assertThat(saveAndCaptureFinalScore(new BigDecimal("90"), new BigDecimal("86")))
                .isEqualByComparingTo(new BigDecimal("87.2"));
    }

    @Test
    @DisplayName("分数边界 0 与 100 参与计算且结果正确")
    void shouldCalculateFinalScoreAtBoundaries() {
        assertThat(saveAndCaptureFinalScore(BigDecimal.ZERO, new BigDecimal("100")))
                .isEqualByComparingTo(new BigDecimal("70.0"));
        assertThat(saveAndCaptureFinalScore(new BigDecimal("100"), BigDecimal.ZERO))
                .isEqualByComparingTo(new BigDecimal("30.0"));
        assertThat(saveAndCaptureFinalScore(new BigDecimal("100"), new BigDecimal("100")))
                .isEqualByComparingTo(new BigDecimal("100.0"));
    }

    @Test
    @DisplayName("只填写一项成绩时不计算总评")
    void shouldNotCalculateFinalScoreWhenPartial() {
        assertThat(saveAndCaptureFinalScore(new BigDecimal("80"), null)).isNull();
        assertThat(saveAndCaptureFinalScore(null, new BigDecimal("90"))).isNull();
    }

    @Test
    @DisplayName("统计结果按 SQL 聚合值组装：平均分、及格率、优秀率与五档分布")
    void shouldBuildStatisticsFromAggregate() {
        ScoreAggregateVO aggregate = new ScoreAggregateVO();
        aggregate.setAverage(new BigDecimal("79.8"));
        aggregate.setMax(new BigDecimal("94.1"));
        aggregate.setMin(new BigDecimal("60.1"));
        aggregate.setTotal(8L);
        aggregate.setPassCount(8L);
        aggregate.setExcellentCount(3L);
        aggregate.setBucket0(0L);
        aggregate.setBucket1(2L);
        aggregate.setBucket2(1L);
        aggregate.setBucket3(2L);
        aggregate.setBucket4(3L);
        when(scoreMapper.selectScoreAggregate(any(), any(), any(), any())).thenReturn(aggregate);

        ScoreStatisticsVO statistics = scoreService.statistics(1L, null);

        assertThat(statistics.getAverageScore()).isEqualTo(79.8);
        assertThat(statistics.getMaxScore()).isEqualTo(94.1);
        assertThat(statistics.getMinScore()).isEqualTo(60.1);
        assertThat(statistics.getPassRate()).isEqualTo(100.0);
        assertThat(statistics.getExcellentRate()).isEqualTo(37.5);
        assertThat(statistics.getDistribution())
                .extracting(ScoreStatisticsVO.DistributionItem::getName)
                .containsExactly("不及格", "60-69", "70-79", "80-89", "90-100");
        assertThat(statistics.getDistribution())
                .extracting(ScoreStatisticsVO.DistributionItem::getValue)
                .containsExactly(0, 2, 1, 2, 3);
    }

    @Test
    @DisplayName("没有任何成绩时统计全部返回 0")
    void shouldReturnZerosWhenNoScore() {
        ScoreAggregateVO aggregate = new ScoreAggregateVO();
        aggregate.setTotal(0L);
        aggregate.setPassCount(0L);
        aggregate.setExcellentCount(0L);
        aggregate.setBucket0(0L);
        aggregate.setBucket1(0L);
        aggregate.setBucket2(0L);
        aggregate.setBucket3(0L);
        aggregate.setBucket4(0L);
        when(scoreMapper.selectScoreAggregate(any(), any(), any(), any())).thenReturn(aggregate);

        ScoreStatisticsVO statistics = scoreService.studentStatistics(1L, 999L);

        assertThat(statistics.getAverageScore()).isZero();
        assertThat(statistics.getMaxScore()).isZero();
        assertThat(statistics.getMinScore()).isZero();
        assertThat(statistics.getPassRate()).isZero();
        assertThat(statistics.getExcellentRate()).isZero();
        assertThat(statistics.getDistribution())
                .extracting(ScoreStatisticsVO.DistributionItem::getValue)
                .containsExactly(0, 0, 0, 0, 0);
    }

    /**
     * 通过 save() 触发总评计算，并抓取写入数据库前的最小成绩对象。
     */
    private BigDecimal saveAndCaptureFinalScore(BigDecimal usualScore, BigDecimal examScore) {
        when(courseMapper.selectById(any())).thenReturn(courseWithScoreStatus(0));
        when(courseStudentMapper.selectCount(any())).thenReturn(1L);
        when(scoreMapper.selectOne(any())).thenReturn(null);

        ScoreRequest request = new ScoreRequest();
        request.setCourseId(1L);
        request.setStudentId(1L);
        request.setUsualScore(usualScore);
        request.setExamScore(examScore);

        scoreService.save(request, 1L);

        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        // 同一个测试方法内可能多次调用 save，captor.getValue() 取最后一次写入的记录
        verify(scoreMapper, atLeastOnce()).insert(captor.capture());
        return captor.getValue().getFinalScore();
    }

    @Test
    @DisplayName("绩点按学分加权计算，保留 2 位小数")
    void shouldCalculateWeightedGpa() {
        when(scoreMapper.selectStudentCourseScores(any(), any())).thenReturn(List.of(
                courseScore("4.0", "90"),
                courseScore("2.0", "60")
        ));

        StudentGpaVO gpa = scoreService.studentGpa(1L, 2L);

        // (4.0 * 4.0 + 2.0 * 1.0) / 6 = 18 / 6 = 3.00
        assertThat(gpa.gpa()).isEqualTo(3.0);
        assertThat(gpa.totalCredit()).isEqualTo(6.0);
        assertThat(gpa.earnedCredit()).isEqualTo(6.0);
        assertThat(gpa.courseCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("不及格课程不计入已获学分，但仍计入总学分")
    void shouldExcludeFailedCourseFromEarnedCredit() {
        when(scoreMapper.selectStudentCourseScores(any(), any())).thenReturn(List.of(
                courseScore("3.0", "50"),
                courseScore("1.0", "85")
        ));

        StudentGpaVO gpa = scoreService.studentGpa(1L, null);

        // (3.0 * 0 + 1.0 * 3.7) / 4 = 0.925 -> 0.93
        assertThat(gpa.gpa()).isEqualTo(0.93);
        assertThat(gpa.totalCredit()).isEqualTo(4.0);
        assertThat(gpa.earnedCredit()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("没有任何成绩时绩点为 0")
    void shouldReturnZeroGpaWhenNoCourse() {
        when(scoreMapper.selectStudentCourseScores(any(), any())).thenReturn(List.of());

        StudentGpaVO gpa = scoreService.studentGpa(1L, null);

        assertThat(gpa.gpa()).isZero();
        assertThat(gpa.totalCredit()).isZero();
        assertThat(gpa.earnedCredit()).isZero();
        assertThat(gpa.courseCount()).isZero();
    }

    private StudentCourseScoreVO courseScore(String credit, String finalScore) {
        StudentCourseScoreVO row = new StudentCourseScoreVO();
        row.setCredit(new BigDecimal(credit));
        row.setFinalScore(new BigDecimal(finalScore));
        return row;
    }

    private Course courseWithScoreStatus(Integer scoreStatus) {
        Course course = new Course();
        course.setId(1L);
        course.setScoreStatus(scoreStatus);
        return course;
    }

    @Test
    @DisplayName("课程成绩已提交锁定时不允许再修改")
    void shouldRejectWhenCourseLocked() {
        when(courseMapper.selectById(any())).thenReturn(courseWithScoreStatus(1));
        ScoreRequest request = new ScoreRequest();
        request.setCourseId(1L);
        request.setStudentId(1L);
        request.setUsualScore(new BigDecimal("80"));
        request.setExamScore(new BigDecimal("90"));

        assertThatThrownBy(() -> scoreService.save(request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已提交锁定");
    }

}
