package com.example.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.score.entity.Student;
import com.example.score.vo.StudentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentMapper extends BaseMapper<Student> {

    List<StudentVO> selectStudentList(@Param("keyword") String keyword, @Param("classId") Long classId);

    StudentVO selectStudentVO(@Param("id") Long id);
}
