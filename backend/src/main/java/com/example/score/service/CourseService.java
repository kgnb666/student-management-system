package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.entity.Course;
import com.example.score.entity.CourseStudent;
import com.example.score.entity.Score;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ScoreMapper scoreMapper;

    public List<CourseVO> list(String keyword, Long semesterId, Long teacherId) {
        return courseMapper.selectCourseList(keyword, semesterId, teacherId);
    }

    public List<CourseVO> listByStudent(Long studentId, Long semesterId) {
        return courseMapper.selectStudentCourses(studentId, semesterId);
    }

    public Course getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    public void create(Course course) {
        checkCourse(course);
        if (course.getStatus() == null) {
            course.setStatus(1);
        }
        courseMapper.insert(course);
    }

    public void update(Course course) {
        checkCourse(course);
        getById(course.getId());
        courseMapper.updateById(course);
    }

    public void delete(Long id) {
        getById(id);
        courseMapper.deleteById(id);
    }

    public List<Long> getStudentIds(Long courseId) {
        return courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, courseId))
                .stream()
                .map(CourseStudent::getStudentId)
                .toList();
    }

    @Transactional
    public void assignStudents(Long courseId, List<Long> studentIds) {
        getById(courseId);
        List<Long> oldIds = getStudentIds(courseId);
        List<Long> newIds = studentIds == null ? new ArrayList<>() : studentIds;
        Set<Long> removedIds = oldIds.stream()
                .filter(id -> !newIds.contains(id))
                .collect(Collectors.toSet());

        for (Long studentId : removedIds) {
            scoreMapper.delete(new LambdaQueryWrapper<Score>()
                    .eq(Score::getCourseId, courseId)
                    .eq(Score::getStudentId, studentId));
        }

        courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId));
        for (Long studentId : newIds) {
            CourseStudent courseStudent = new CourseStudent();
            courseStudent.setCourseId(courseId);
            courseStudent.setStudentId(studentId);
            courseStudentMapper.insert(courseStudent);
        }
    }

    private void checkCourse(Course course) {
        if (course.getCourseCode() == null || course.getCourseCode().isBlank()) {
            throw new BusinessException("课程编号不能为空");
        }
        if (course.getCourseName() == null || course.getCourseName().isBlank()) {
            throw new BusinessException("课程名称不能为空");
        }
        if (course.getSemesterId() == null) {
            throw new BusinessException("请选择学期");
        }
        if (course.getTeacherId() == null) {
            throw new BusinessException("请选择授课教师");
        }
    }
}
