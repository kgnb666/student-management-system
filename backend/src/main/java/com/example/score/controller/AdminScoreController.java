package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.ScoreRequest;
import com.example.score.service.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scores")
@RequiredArgsConstructor
public class AdminScoreController {

    private final ScoreService scoreService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) Long semesterId,
                          @RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) Long studentId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long page,
                          @RequestParam(required = false) Long size) {
        if (page != null) {
            return Result.success(scoreService.listPaged(semesterId, courseId, studentId, null, keyword, page, size));
        }
        return Result.success(scoreService.list(semesterId, courseId, studentId, null, keyword));
    }

    @PostMapping
    public Result<Void> create(@RequestAttribute("userId") Long userId,
                               @Valid @RequestBody ScoreRequest request) {
        scoreService.save(request, userId);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId,
                               @Valid @RequestBody ScoreRequest request) {
        scoreService.update(id, request, userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId) {
        scoreService.delete(id, userId);
        return Result.success();
    }
}
