package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.dto.BatchScoreRequest;
import com.example.score.dto.ScoreRequest;
import com.example.score.entity.CourseStudent;
import com.example.score.entity.Score;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.ScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final CourseStudentMapper courseStudentMapper;

    public List<ScoreVO> list(Long semesterId, Long courseId, Long studentId, Long teacherId, String keyword) {
        return scoreMapper.selectScoreList(semesterId, courseId, studentId, teacherId, keyword);
    }

    public List<CourseStudentVO> courseStudents(Long courseId) {
        return scoreMapper.selectCourseStudents(courseId);
    }

    @Transactional
    public void save(ScoreRequest request) {
        checkScore(request.getUsualScore(), request.getExamScore());
        if (request.getCourseId() == null || request.getStudentId() == null) {
            throw new BusinessException("请选择课程和学生");
        }
        if (request.getUsualScore() == null && request.getExamScore() == null) {
            throw new BusinessException("请至少填写一项成绩");
        }
        checkCourseStudent(request.getCourseId(), request.getStudentId());

        Score score = scoreMapper.selectOne(new LambdaQueryWrapper<Score>()
                .eq(Score::getCourseId, request.getCourseId())
                .eq(Score::getStudentId, request.getStudentId()));
        if (score == null) {
            score = new Score();
            score.setCourseId(request.getCourseId());
            score.setStudentId(request.getStudentId());
        }
        score.setUsualScore(request.getUsualScore());
        score.setExamScore(request.getExamScore());
        score.setFinalScore(calculateFinalScore(request.getUsualScore(), request.getExamScore()));

        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }
    }

    @Transactional
    public void saveBatch(BatchScoreRequest request) {
        if (request.getCourseId() == null) {
            throw new BusinessException("请选择课程");
        }
        if (request.getScores() == null) {
            return;
        }
        for (var item : request.getScores()) {
            if (item.getUsualScore() == null && item.getExamScore() == null) {
                scoreMapper.delete(new LambdaQueryWrapper<Score>()
                        .eq(Score::getCourseId, request.getCourseId())
                        .eq(Score::getStudentId, item.getStudentId()));
                continue;
            }
            ScoreRequest scoreRequest = new ScoreRequest();
            scoreRequest.setCourseId(request.getCourseId());
            scoreRequest.setStudentId(item.getStudentId());
            scoreRequest.setUsualScore(item.getUsualScore());
            scoreRequest.setExamScore(item.getExamScore());
            save(scoreRequest);
        }
    }

    public void update(Long id, ScoreRequest request) {
        checkScore(request.getUsualScore(), request.getExamScore());
        if (request.getCourseId() == null || request.getStudentId() == null) {
            throw new BusinessException("请选择课程和学生");
        }
        checkCourseStudent(request.getCourseId(), request.getStudentId());
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            throw new BusinessException("成绩记录不存在");
        }
        score.setCourseId(request.getCourseId());
        score.setStudentId(request.getStudentId());
        score.setUsualScore(request.getUsualScore());
        score.setExamScore(request.getExamScore());
        score.setFinalScore(calculateFinalScore(request.getUsualScore(), request.getExamScore()));
        scoreMapper.updateById(score);
    }

    public void delete(Long id) {
        if (scoreMapper.deleteById(id) == 0) {
            throw new BusinessException("成绩记录不存在");
        }
    }

    public ScoreStatisticsVO statistics(Long courseId, Long classId) {
        List<BigDecimal> scores = courseStudents(courseId).stream()
                .filter(student -> classId == null || classId.equals(student.getClassId()))
                .map(CourseStudentVO::getFinalScore)
                .filter(score -> score != null)
                .toList();
        return buildStatistics(scores);
    }

    public ScoreStatisticsVO studentStatistics(Long studentId, Long semesterId) {
        List<BigDecimal> scores = list(semesterId, null, studentId, null, null).stream()
                .map(ScoreVO::getFinalScore)
                .filter(score -> score != null)
                .toList();
        return buildStatistics(scores);
    }

    private ScoreStatisticsVO buildStatistics(List<BigDecimal> scores) {
        List<ScoreStatisticsVO.DistributionItem> distribution = new ArrayList<>();
        distribution.add(new ScoreStatisticsVO.DistributionItem("不及格", 0));
        distribution.add(new ScoreStatisticsVO.DistributionItem("60-69", 0));
        distribution.add(new ScoreStatisticsVO.DistributionItem("70-79", 0));
        distribution.add(new ScoreStatisticsVO.DistributionItem("80-89", 0));
        distribution.add(new ScoreStatisticsVO.DistributionItem("90-100", 0));

        if (scores.isEmpty()) {
            return new ScoreStatisticsVO(0.0, 0.0, 0.0, 0.0, 0.0, distribution);
        }

        for (BigDecimal score : scores) {
            double value = score.doubleValue();
            if (value < 60) {
                distribution.get(0).setValue(distribution.get(0).getValue() + 1);
            } else if (value < 70) {
                distribution.get(1).setValue(distribution.get(1).getValue() + 1);
            } else if (value < 80) {
                distribution.get(2).setValue(distribution.get(2).getValue() + 1);
            } else if (value < 90) {
                distribution.get(3).setValue(distribution.get(3).getValue() + 1);
            } else {
                distribution.get(4).setValue(distribution.get(4).getValue() + 1);
            }
        }

        double average = scores.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);
        double max = scores.stream()
                .max(Comparator.naturalOrder())
                .map(BigDecimal::doubleValue)
                .orElse(0.0);
        double min = scores.stream()
                .min(Comparator.naturalOrder())
                .map(BigDecimal::doubleValue)
                .orElse(0.0);
        long passCount = scores.stream().filter(score -> score.doubleValue() >= 60).count();
        long excellentCount = scores.stream().filter(score -> score.doubleValue() >= 90).count();

        return new ScoreStatisticsVO(
                round(average),
                round(max),
                round(min),
                round(passCount * 100.0 / scores.size()),
                round(excellentCount * 100.0 / scores.size()),
                distribution
        );
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

    private boolean validScore(BigDecimal score) {
        return score == null || (score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(BigDecimal.valueOf(100)) <= 0);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
