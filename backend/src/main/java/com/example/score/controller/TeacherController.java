package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.BatchScoreRequest;
import com.example.score.entity.Course;
import com.example.score.entity.Teacher;
import com.example.score.exception.BusinessException;
import com.example.score.service.CourseService;
import com.example.score.service.ScoreExcelService;
import com.example.score.service.ScoreChangeLogService;
import com.example.score.service.ScoreService;
import com.example.score.service.TeacherService;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.CourseVO;
import com.example.score.vo.ScoreStatisticsVO;
import com.example.score.vo.ScoreImportResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final CourseService courseService;
    private final ScoreService scoreService;
    private final ScoreExcelService scoreExcelService;
    private final ScoreChangeLogService scoreChangeLogService;

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
    public Result<?> scores(@RequestAttribute("userId") Long userId,
                            @RequestParam(required = false) Long courseId,
                            @RequestParam(required = false) Long studentId,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long page,
                            @RequestParam(required = false) Long size) {
        Teacher teacher = teacherService.getByUserId(userId);
        if (page != null) {
            return Result.success(
                    scoreService.listPaged(null, courseId, studentId, teacher.getId(), keyword, page, size));
        }
        return Result.success(scoreService.list(null, courseId, studentId, teacher.getId(), keyword));
    }

    @PostMapping("/scores/batch")
    public Result<Void> saveScores(@RequestAttribute("userId") Long userId,
                                   @Valid @RequestBody BatchScoreRequest request) {
        checkCourseOwner(userId, request.getCourseId());
        scoreService.saveBatch(request, userId);
        return Result.success();
    }

    @PostMapping("/courses/{courseId}/submit")
    public Result<Void> submitScores(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long courseId) {
        checkCourseOwner(userId, courseId);
        courseService.submitScores(courseId, userId);
        return Result.success();
    }

    @GetMapping("/courses/{courseId}/scores/export")
    public ResponseEntity<byte[]> exportScores(@RequestAttribute("userId") Long userId,
                                               @PathVariable Long courseId) {
        checkCourseOwner(userId, courseId);
        byte[] content = scoreExcelService.exportCourseScores(courseId);
        String filename = URLEncoder.encode("课程成绩单.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @PostMapping("/courses/{courseId}/scores/import")
    public Result<ScoreImportResultVO> importScores(@RequestAttribute("userId") Long userId,
                                                    @PathVariable Long courseId,
                                                    @RequestParam("file") MultipartFile file) {
        checkCourseOwner(userId, courseId);
        return Result.success(scoreExcelService.importCourseScores(courseId, file, userId));
    }

    @GetMapping("/courses/{courseId}/score-logs")
    public Result<?> scoreLogs(@RequestAttribute("userId") Long userId,
                               @PathVariable Long courseId,
                               @RequestParam(required = false) Long page,
                               @RequestParam(required = false) Long size) {
        checkCourseOwner(userId, courseId);
        if (page != null) {
            return Result.success(scoreChangeLogService.listPaged(courseId, null, page, size));
        }
        return Result.success(scoreChangeLogService.list(courseId, null));
    }

    private void checkCourseOwner(Long userId, Long courseId) {
        Teacher teacher = teacherService.getByUserId(userId);
        Course course = courseService.getById(courseId);
        if (!teacher.getId().equals(course.getTeacherId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能操作自己负责的课程");
        }
    }
}
