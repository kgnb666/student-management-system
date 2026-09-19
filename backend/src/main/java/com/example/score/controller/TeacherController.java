package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.BatchScoreRequest;
import com.example.score.entity.Course;
import com.example.score.entity.Teacher;
import com.example.score.exception.BusinessException;
import com.example.score.service.CourseService;
import com.example.score.service.ScoreService;
import com.example.score.service.TeacherService;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.CourseVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.ScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final CourseService courseService;
    private final ScoreService scoreService;

    @GetMapping("/courses")
    public Result<List<CourseVO>> courses(@RequestAttribute("userId") Long userId) {
        Teacher teacher = teacherService.getByUserId(userId);
        return Result.success(courseService.list(null, null, teacher.getId()));
    }

    @GetMapping("/courses/{courseId}/students")
    public Result<List<CourseStudentVO>> courseStudents(@RequestAttribute("userId") Long userId,
                                                        @PathVariable Long courseId) {
        checkCourseOwner(userId, courseId);
        return Result.success(scoreService.courseStudents(courseId));
    }

    @GetMapping("/courses/{courseId}/statistics")
    public Result<ScoreStatisticsVO> statistics(@RequestAttribute("userId") Long userId,
                                                @PathVariable Long courseId,
                                                @RequestParam(required = false) Long classId) {
        checkCourseOwner(userId, courseId);
        return Result.success(scoreService.statistics(courseId, classId));
    }

    @GetMapping("/scores")
    public Result<List<ScoreVO>> scores(@RequestAttribute("userId") Long userId,
                                        @RequestParam(required = false) Long courseId,
                                        @RequestParam(required = false) Long studentId,
                                        @RequestParam(required = false) String keyword) {
        Teacher teacher = teacherService.getByUserId(userId);
        return Result.success(scoreService.list(null, courseId, studentId, teacher.getId(), keyword));
    }

    @PostMapping("/scores/batch")
    public Result<Void> saveScores(@RequestAttribute("userId") Long userId,
                                   @RequestBody BatchScoreRequest request) {
        checkCourseOwner(userId, request.getCourseId());
        scoreService.saveBatch(request);
        return Result.success();
    }

    private void checkCourseOwner(Long userId, Long courseId) {
        Teacher teacher = teacherService.getByUserId(userId);
        Course course = courseService.getById(courseId);
        if (!teacher.getId().equals(course.getTeacherId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能操作自己负责的课程");
        }
    }
}
