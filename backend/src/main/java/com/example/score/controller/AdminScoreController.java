package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.dto.ScoreRequest;
import com.example.score.service.ScoreService;
import com.example.score.vo.ScoreVO;
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
@RequestMapping("/api/admin/scores")
@RequiredArgsConstructor
public class AdminScoreController {

    private final ScoreService scoreService;

    @GetMapping
    public Result<List<ScoreVO>> list(@RequestParam(required = false) Long semesterId,
                                      @RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) Long studentId,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(scoreService.list(semesterId, courseId, studentId, null, keyword));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ScoreRequest request) {
        scoreService.save(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScoreRequest request) {
        scoreService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scoreService.delete(id);
        return Result.success();
    }
}
