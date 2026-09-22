package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.service.ScoreChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/score-logs")
@RequiredArgsConstructor
public class AdminScoreLogController {

    private final ScoreChangeLogService scoreChangeLogService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) Long studentId,
                          @RequestParam(required = false) Long page,
                          @RequestParam(required = false) Long size) {
        if (page != null) {
            return Result.success(scoreChangeLogService.listPaged(courseId, studentId, page, size));
        }
        return Result.success(scoreChangeLogService.list(courseId, studentId));
    }
}
