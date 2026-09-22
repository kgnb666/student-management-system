package com.example.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.score.entity.Score;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreAggregateVO;
import com.example.score.vo.ScoreVO;
import com.example.score.vo.StudentCourseScoreVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScoreMapper extends BaseMapper<Score> {

    List<ScoreVO> selectScoreList(@Param("semesterId") Long semesterId,
                                  @Param("courseId") Long courseId,
                                  @Param("studentId") Long studentId,
                                  @Param("teacherId") Long teacherId,
                                  @Param("keyword") String keyword);

    IPage<ScoreVO> selectScoreListPage(IPage<ScoreVO> page,
                                       @Param("semesterId") Long semesterId,
                                       @Param("courseId") Long courseId,
                                       @Param("studentId") Long studentId,
                                       @Param("teacherId") Long teacherId,
                                       @Param("keyword") String keyword);

    List<CourseStudentVO> selectCourseStudents(@Param("courseId") Long courseId);

    ScoreAggregateVO selectScoreAggregate(@Param("courseId") Long courseId,
                                          @Param("classId") Long classId,
                                          @Param("studentId") Long studentId,
                                          @Param("semesterId") Long semesterId);

    List<StudentCourseScoreVO> selectStudentCourseScores(@Param("studentId") Long studentId,
                                                         @Param("semesterId") Long semesterId);
}
