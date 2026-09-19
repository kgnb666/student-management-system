package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.CourseStudentRequest;
import com.example.score.entity.Course;
import com.example.score.service.CourseService;
import com.example.score.service.ScoreService;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.CourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final ScoreService scoreService;

    @GetMapping
    public Result<List<CourseVO>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long semesterId,
                                       @RequestParam(required = false) Long teacherId) {
        return Result.success(courseService.list(keyword, semesterId, teacherId));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Course course) {
        courseService.create(course);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.update(course);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/students")
    public Result<List<CourseStudentVO>> students(@PathVariable Long id) {
        return Result.success(scoreService.courseStudents(id));
    }

    @PutMapping("/{id}/students")
    public Result<Void> assignStudents(@PathVariable Long id, @RequestBody CourseStudentRequest request) {
        courseService.assignStudents(id, request.getStudentIds());
        return Result.success();
    }
}
