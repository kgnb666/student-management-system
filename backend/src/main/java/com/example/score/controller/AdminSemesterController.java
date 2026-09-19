package com.example.score.controller;

import com.example.score.common.Result;
import com.example.score.entity.Semester;
import com.example.score.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/semesters")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterService semesterService;

    @GetMapping
    public Result<List<Semester>> list() {
        return Result.success(semesterService.list());
    }

    @PostMapping
    public Result<Void> create(@RequestBody Semester semester) {
        semesterService.create(semester);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Semester semester) {
        semester.setId(id);
        semesterService.update(semester);
        return Result.success();
    }

    @PutMapping("/{id}/current")
    public Result<Void> setCurrent(@PathVariable Long id) {
        semesterService.setCurrent(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return Result.success();
    }
}
