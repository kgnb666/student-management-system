package com.example.score.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourseStudentRequest {

    private List<Long> studentIds;
}
