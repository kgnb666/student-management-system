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
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public Result<?> list(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long semesterId,
                          @RequestParam(required = false) Long teacherId,
                          @RequestParam(required = false) Long page,
                          @RequestParam(required = false) Long size) {
        if (page != null) {
            return Result.success(courseService.listPaged(keyword, semesterId, teacherId, page, size));
        }
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
    public Result<Integer> assignStudents(@PathVariable Long id,
                                          @RequestAttribute("userId") Long userId,
                                          @RequestBody CourseStudentRequest request) {
        // data 为因移出名单而同步删除的成绩条数
        return Result.success(courseService.assignStudents(id, request.getStudentIds(), userId));
    }

    @PostMapping("/{id}/unlock")
    public Result<Void> unlockScores(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId) {
        courseService.unlockScores(id, userId);
        return Result.success();
    }
}
