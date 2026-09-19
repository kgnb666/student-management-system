package com.example.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.score.entity.Score;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScoreMapper extends BaseMapper<Score> {

    List<ScoreVO> selectScoreList(@Param("semesterId") Long semesterId,
                                  @Param("courseId") Long courseId,
                                  @Param("studentId") Long studentId,
                                  @Param("teacherId") Long teacherId,
                                  @Param("keyword") String keyword);

    List<CourseStudentVO> selectCourseStudents(@Param("courseId") Long courseId);
}
