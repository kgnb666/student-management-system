package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.entity.Semester;
import com.example.score.entity.Student;
import com.example.score.mapper.ClassInfoMapper;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.SemesterMapper;
import com.example.score.mapper.StudentMapper;
import com.example.score.mapper.TeacherMapper;
import com.example.score.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassInfoMapper classInfoMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;
    private final ScoreService scoreService;

    public DashboardVO overview() {
        Semester current = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .last("LIMIT 1"));
        Long semesterId = current == null ? null : current.getId();
        return new DashboardVO(
                studentMapper.selectCount(null).intValue(),
                teacherMapper.selectCount(null).intValue(),
                classInfoMapper.selectCount(null).intValue(),
                courseMapper.selectCount(null).intValue(),
                studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, 2)).intValue(),
                semesterId,
                current == null ? null : current.getSemesterName(),
                scoreService.statisticsBySemester(semesterId)
        );
    }
}
