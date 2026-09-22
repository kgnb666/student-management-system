package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.score.entity.Course;
import com.example.score.entity.CourseStudent;
import com.example.score.entity.Score;
import com.example.score.entity.ScoreChangeLog;
import com.example.score.entity.Student;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.mapper.StudentMapper;
import com.example.score.vo.CourseVO;
import com.example.score.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final ScoreMapper scoreMapper;
    private final StudentMapper studentMapper;
    private final ScoreChangeLogService scoreChangeLogService;

    public List<CourseVO> list(String keyword, Long semesterId, Long teacherId) {
        return courseMapper.selectCourseList(keyword, semesterId, teacherId);
    }

    public PageResult<CourseVO> listPaged(String keyword, Long semesterId, Long teacherId, Long page, Long size) {
        Page<CourseVO> pageParam = new Page<>(PageResult.normalizeCurrent(page), PageResult.normalizeSize(size));
        return PageResult.of(courseMapper.selectCourseListPage(pageParam, keyword, semesterId, teacherId));
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

    /**
     * 覆盖式保存课程学生名单。
     *
     * @return 因学生被移出名单而同步删除的成绩条数
     */
    @Transactional
    public int assignStudents(Long courseId, List<Long> studentIds, Long operatorUserId) {
        getById(courseId);
        List<Long> oldIds = getStudentIds(courseId);
        List<Long> newIds = studentIds == null
                ? new ArrayList<>()
                : studentIds.stream().filter(Objects::nonNull).distinct().toList();
        checkStudentsExist(newIds);

        Set<Long> newIdSet = new HashSet<>(newIds);
        Set<Long> removedIds = oldIds.stream()
                .filter(id -> !newIdSet.contains(id))
                .collect(Collectors.toSet());

        int removedScoreCount = 0;
        for (Long studentId : removedIds) {
            List<Score> removedScores = scoreMapper.selectList(new LambdaQueryWrapper<Score>()
                    .eq(Score::getCourseId, courseId)
                    .eq(Score::getStudentId, studentId));
            for (Score score : removedScores) {
                scoreMapper.deleteById(score.getId());
                scoreChangeLogService.recordScoreChange(score, null, ScoreChangeLog.ACTION_DELETE,
                        operatorUserId, "因移出课程名单删除成绩");
            }
            removedScoreCount += removedScores.size();
        }

        courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId));
        for (Long studentId : newIds) {
            CourseStudent courseStudent = new CourseStudent();
            courseStudent.setCourseId(courseId);
            courseStudent.setStudentId(studentId);
            courseStudentMapper.insert(courseStudent);
        }
        return removedScoreCount;
    }

    private void checkStudentsExist(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return;
        }
        Long found = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .in(Student::getId, studentIds));
        if (found == null || found != studentIds.size()) {
            throw new BusinessException("学生名单中存在无效的学生，请刷新后重试");
        }
    }

    /**
     * 教师提交成绩：锁定该课程的成绩，之后教师无法再修改。
     */
    @Transactional
    public void submitScores(Long courseId, Long operatorUserId) {
        Course course = getById(courseId);
        if (course.getScoreStatus() != null && course.getScoreStatus() == 1) {
            throw new BusinessException("该课程成绩已提交，无需重复提交");
        }
        updateScoreStatus(courseId, 1);
        scoreChangeLogService.recordCourseAction(courseId, ScoreChangeLog.ACTION_SUBMIT,
                operatorUserId, "教师提交成绩并锁定");
    }

    /**
     * 管理员解锁成绩：把课程成绩退回录入中状态。
     */
    @Transactional
    public void unlockScores(Long courseId, Long operatorUserId) {
        Course course = getById(courseId);
        if (course.getScoreStatus() == null || course.getScoreStatus() == 0) {
            throw new BusinessException("该课程成绩当前处于录入中状态，无需解锁");
        }
        updateScoreStatus(courseId, 0);
        scoreChangeLogService.recordCourseAction(courseId, ScoreChangeLog.ACTION_UNLOCK,
                operatorUserId, "管理员解锁成绩");
    }

    private void updateScoreStatus(Long courseId, int scoreStatus) {
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getScoreStatus, scoreStatus)
                .set(Course::getSubmitTime, scoreStatus == 1 ? LocalDateTime.now() : null));
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
