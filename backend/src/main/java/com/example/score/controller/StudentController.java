package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.entity.Semester;
import com.example.score.entity.Student;
import com.example.score.service.CourseService;
import com.example.score.service.ScoreService;
import com.example.score.service.SemesterService;
import com.example.score.service.StudentService;
import com.example.score.vo.CourseVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.ScoreVO;
import com.example.score.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ScoreService scoreService;
    private final SemesterService semesterService;

    @GetMapping("/profile")
    public Result<StudentVO> profile(@RequestAttribute("userId") Long userId) {
        return Result.success(studentService.getProfileByUserId(userId));
    }

    @GetMapping("/semesters")
    public Result<List<Semester>> semesters() {
        return Result.success(semesterService.list());
    }

    @GetMapping("/courses")
    public Result<List<CourseVO>> courses(@RequestAttribute("userId") Long userId,
                                          @RequestParam(required = false) Long semesterId) {
        Student student = getStudent(userId);
        return Result.success(courseService.listByStudent(student.getId(), semesterId));
    }

    @GetMapping("/scores")
    public Result<List<ScoreVO>> scores(@RequestAttribute("userId") Long userId,
                                        @RequestParam(required = false) Long semesterId) {
        Student student = getStudent(userId);
        return Result.success(scoreService.list(semesterId, null, student.getId(), null, null));
    }

    @GetMapping("/statistics")
    public Result<ScoreStatisticsVO> statistics(@RequestAttribute("userId") Long userId,
                                                @RequestParam(required = false) Long semesterId) {
        Student student = getStudent(userId);
        return Result.success(scoreService.studentStatistics(student.getId(), semesterId));
    }

    private Student getStudent(Long userId) {
        return studentService.getByUserId(userId);
    }
}
