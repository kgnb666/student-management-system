package com.example.score.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseVO {

    private Long id;
    private String courseCode;
    private String courseName;
    private BigDecimal credit;
    private Integer hours;
    private String courseType;
    private Long semesterId;
    private String semesterName;
    private Long teacherId;
    private String teacherName;
    private Integer status;
    private Integer scoreStatus;
    private LocalDateTime submitTime;
}
