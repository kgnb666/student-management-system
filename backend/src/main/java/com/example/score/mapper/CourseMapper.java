package com.example.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.score.entity.Course;
import com.example.score.vo.CourseVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    List<CourseVO> selectCourseList(@Param("keyword") String keyword,
                                    @Param("semesterId") Long semesterId,
                                    @Param("teacherId") Long teacherId);

    IPage<CourseVO> selectCourseListPage(IPage<CourseVO> page,
                                         @Param("keyword") String keyword,
                                         @Param("semesterId") Long semesterId,
                                         @Param("teacherId") Long teacherId);

    List<CourseVO> selectStudentCourses(@Param("studentId") Long studentId,
                                        @Param("semesterId") Long semesterId);
}
