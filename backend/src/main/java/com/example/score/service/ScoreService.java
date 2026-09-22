package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.score.dto.BatchScoreRequest;
import com.example.score.dto.ScoreRequest;
import com.example.score.entity.CourseStudent;
import com.example.score.entity.Course;
import com.example.score.entity.Score;
import com.example.score.entity.ScoreChangeLog;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreAggregateVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.ScoreVO;
import com.example.score.vo.StudentCourseScoreVO;
import com.example.score.vo.StudentGpaVO;
import com.example.score.vo.PageResult;
import com.example.score.util.GradePointUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final CourseMapper courseMapper;
    private final ScoreChangeLogService scoreChangeLogService;

    public List<ScoreVO> list(Long semesterId, Long courseId, Long studentId, Long teacherId, String keyword) {
        return scoreMapper.selectScoreList(semesterId, courseId, studentId, teacherId, keyword);
    }

    public PageResult<ScoreVO> listPaged(Long semesterId, Long courseId, Long studentId, Long teacherId,
                                         String keyword, Long page, Long size) {
        Page<ScoreVO> pageParam = new Page<>(PageResult.normalizeCurrent(page), PageResult.normalizeSize(size));
        return PageResult.of(scoreMapper.selectScoreListPage(pageParam, semesterId, courseId, studentId, teacherId, keyword));
    }

    public List<CourseStudentVO> courseStudents(Long courseId) {
        return scoreMapper.selectCourseStudents(courseId);
    }

    @Transactional
    public void save(ScoreRequest request, Long operatorUserId) {
        checkScore(request.getUsualScore(), request.getExamScore());
        if (request.getCourseId() == null || request.getStudentId() == null) {
            throw new BusinessException("请选择课程和学生");
        }
        checkCourseEditable(request.getCourseId());
        if (request.getUsualScore() == null && request.getExamScore() == null) {
            throw new BusinessException("请至少填写一项成绩");
        }
        checkCourseStudent(request.getCourseId(), request.getStudentId());

        Score score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getCourseId, request.getCourseId())
                .eq(Score::getStudentId, request.getStudentId()));
        Score before = score == null ? null : copyOf(score);
        if (score == null) {
            score = new Score();
            score.setCourseId(request.getCourseId());
            score.setStudentId(request.getStudentId());
        }
        score.setUsualScore(request.getUsualScore());
        score.setExamScore(request.getExamScore());
        score.setFinalScore(calculateFinalScore(request.getUsualScore(), request.getExamScore()));
        score.setUpdateBy(operatorUserId);

        if (score.getId() == null) {
            scoreMapper.insert(score);
            scoreChangeLogService.recordScoreChange(null, score, ScoreChangeLog.ACTION_CREATE, operatorUserId, null);
        } else {
            scoreMapper.updateById(score);
            if (scoreChanged(before, score)) {
                scoreChangeLogService.recordScoreChange(before, score, ScoreChangeLog.ACTION_UPDATE, operatorUserId, null);
            }
        }
    }

    @Transactional
    public void saveBatch(BatchScoreRequest request, Long operatorUserId) {
        if (request.getCourseId() == null) {
            throw new BusinessException("请选择课程");
        }
        checkCourseEditable(request.getCourseId());
        if (request.getScores() == null) {
            return;
        }
        for (var item : request.getScores()) {
            if (item.getUsualScore() == null && item.getExamScore() == null) {
                Score existing = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                        .eq(Score::getCourseId, request.getCourseId())
                        .eq(Score::getStudentId, item.getStudentId()));
                if (existing != null) {
                    scoreMapper.deleteById(existing.getId());
                    scoreChangeLogService.recordScoreChange(existing, null, ScoreChangeLog.ACTION_DELETE,
                            operatorUserId, "批量录入时清空成绩");
                }
                continue;
            }
            ScoreRequest scoreRequest = new ScoreRequest();
            scoreRequest.setCourseId(request.getCourseId());
            scoreRequest.setStudentId(item.getStudentId());
            scoreRequest.setUsualScore(item.getUsualScore());
            scoreRequest.setExamScore(item.getExamScore());
            save(scoreRequest, operatorUserId);
        }
    }

    /**
     * 修改成绩。课程与学生以记录本身为准，不接受请求中的变更，
     * 避免成绩被“搬”到其他课程或触发 (course_id, student_id) 唯一键冲突。
     */
    public void update(Long id, ScoreRequest request, Long operatorUserId) {
        checkScore(request.getUsualScore(), request.getExamScore());
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            throw new BusinessException("成绩记录不存在");
        }
        checkCourseEditable(score.getCourseId());
        if (request.getUsualScore() == null && request.getExamScore() == null) {
            throw new BusinessException("请至少填写一项成绩");
        }
        Score before = copyOf(score);
        score.setUsualScore(request.getUsualScore());
        score.setExamScore(request.getExamScore());
        score.setFinalScore(calculateFinalScore(request.getUsualScore(), request.getExamScore()));
        score.setUpdateBy(operatorUserId);
        scoreMapper.updateById(score);
        if (scoreChanged(before, score)) {
            scoreChangeLogService.recordScoreChange(before, score, ScoreChangeLog.ACTION_UPDATE, operatorUserId, null);
        }
    }

    public void delete(Long id, Long operatorUserId) {
        Score before = scoreMapper.selectById(id);
        if (before == null) {
            throw new BusinessException("成绩记录不存在");
        }
        scoreMapper.deleteById(id);
        scoreChangeLogService.recordScoreChange(before, null, ScoreChangeLog.ACTION_DELETE, operatorUserId, null);
    }

    public ScoreStatisticsVO statistics(Long courseId, Long classId) {
        return buildStatistics(scoreMapper.selectScoreAggregate(courseId, classId, null, null));
    }

    public ScoreStatisticsVO studentStatistics(Long studentId, Long semesterId) {
        return buildStatistics(scoreMapper.selectScoreAggregate(null, null, studentId, semesterId));
    }

    /**
     * 按学期统计全部成绩，用于管理员首页概览。
     */
    public ScoreStatisticsVO statisticsBySemester(Long semesterId) {
        return buildStatistics(scoreMapper.selectScoreAggregate(null, null, null, semesterId));
    }

    /**
     * 学生学分绩点：按课程学分对 4.0 制绩点加权平均，保留 2 位小数。
     */
    public StudentGpaVO studentGpa(Long studentId, Long semesterId) {
        List<StudentCourseScoreVO> rows = scoreMapper.selectStudentCourseScores(studentId, semesterId);

        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal earnedCredit = BigDecimal.ZERO;
        BigDecimal weightedPoint = BigDecimal.ZERO;
        for (StudentCourseScoreVO row : rows) {
            BigDecimal credit = row.getCredit() == null ? BigDecimal.ZERO : row.getCredit();
            totalCredit = totalCredit.add(credit);
            weightedPoint = weightedPoint.add(credit.multiply(GradePointUtil.gradePoint(row.getFinalScore())));
            if (GradePointUtil.isPassed(row.getFinalScore())) {
                earnedCredit = earnedCredit.add(credit);
            }
        }

        double gpa = totalCredit.signum() == 0
                ? 0.0
                : weightedPoint.divide(totalCredit, 2, RoundingMode.HALF_UP).doubleValue();
        return new StudentGpaVO(gpa, totalCredit.doubleValue(), earnedCredit.doubleValue(), rows.size());
    }

    /**
     * 由 SQL 聚合结果组装统计视图，统计口径与内存计算版本保持一致。
     */
    private ScoreStatisticsVO buildStatistics(ScoreAggregateVO aggregate) {
        List<ScoreStatisticsVO.DistributionItem> distribution = List.of(
                new ScoreStatisticsVO.DistributionItem("不及格", toInt(aggregate.getBucket0())),
                new ScoreStatisticsVO.DistributionItem("60-69", toInt(aggregate.getBucket1())),
                new ScoreStatisticsVO.DistributionItem("70-79", toInt(aggregate.getBucket2())),
                new ScoreStatisticsVO.DistributionItem("80-89", toInt(aggregate.getBucket3())),
                new ScoreStatisticsVO.DistributionItem("90-100", toInt(aggregate.getBucket4()))
        );

        long total = aggregate.getTotal() == null ? 0L : aggregate.getTotal();
        if (total == 0) {
            return new ScoreStatisticsVO(0.0, 0.0, 0.0, 0.0, 0.0, distribution);
        }

        long passCount = aggregate.getPassCount() == null ? 0L : aggregate.getPassCount();
        long excellentCount = aggregate.getExcellentCount() == null ? 0L : aggregate.getExcellentCount();
        return new ScoreStatisticsVO(
                toDouble(aggregate.getAverage()),
                toDouble(aggregate.getMax()),
                toDouble(aggregate.getMin()),
                round(passCount * 100.0 / total),
                round(excellentCount * 100.0 / total),
                distribution
        );
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private Score copyOf(Score score) {
        Score copy = new Score();
        copy.setId(score.getId());
        copy.setCourseId(score.getCourseId());
        copy.setStudentId(score.getStudentId());
        copy.setUsualScore(score.getUsualScore());
        copy.setExamScore(score.getExamScore());
        copy.setFinalScore(score.getFinalScore());
        return copy;
    }

    /**
     * 相比变更前的值，两项成绩是否发生了实际变化。
     */
    private boolean scoreChanged(Score before, Score after) {
        return !sameValue(before.getUsualScore(), after.getUsualScore())
                || !sameValue(before.getExamScore(), after.getExamScore());
    }

    private boolean sameValue(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private BigDecimal calculateFinalScore(BigDecimal usualScore, BigDecimal examScore) {
        if (usualScore == null || examScore == null) {
            return null;
        }
        return usualScore.multiply(BigDecimal.valueOf(0.3))
                .add(examScore.multiply(BigDecimal.valueOf(0.7)))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private void checkScore(BigDecimal usualScore, BigDecimal examScore) {
        if (!validScore(usualScore) || !validScore(examScore)) {
            throw new BusinessException("成绩必须在 0 到 100 之间");
        }
    }

    private void checkCourseStudent(Long courseId, Long studentId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (count == 0) {
            throw new BusinessException("该学生不在课程名单中");
        }
    }

    /**
     * 已提交锁定的课程不允许再修改成绩，需管理员先解锁。
     */
    public void checkCourseEditable(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (course.getScoreStatus() != null && course.getScoreStatus() == 1) {
            throw new BusinessException("该课程成绩已提交锁定，如需修改请联系管理员解锁");
        }
    }

    private boolean validScore(BigDecimal score) {
        return score == null || (score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(BigDecimal.valueOf(100)) <= 0);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
